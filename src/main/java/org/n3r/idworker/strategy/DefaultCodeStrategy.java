package org.n3r.idworker.strategy;

import org.n3r.idworker.CodeStrategy;
import org.n3r.idworker.utils.BloomFilter;
import org.n3r.idworker.utils.Serializes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Queue;

public class DefaultCodeStrategy implements CodeStrategy {
    static final int MAX_PREFIX_INDEX = Integer.MAX_VALUE;
    Logger log = LoggerFactory.getLogger(DefaultCodeStrategy.class);

    File dir = new File(System.getProperty("user.home") + File.separator + ".idworkers");
    FileLock fileLock;
    BloomFilter codesFilter;
    FileOutputStream fileOutputStream;

    int prefixIndex;
    File codePrefixIndex;

    int minRandomSize = 5;
    int maxRandomSize = 10;

    @Override
    public void init() {
        release();

        prefixIndex = -1;
        while (++prefixIndex < MAX_PREFIX_INDEX) {
            if (tryUsePrefix(prefixIndex)) break;
        }

        if (prefixIndex == MAX_PREFIX_INDEX)
            throw new RuntimeException("all prefixes are used up, the world maybe ends!");
    }

    public void setMinRandomSize(int minRandomSize) {
        this.minRandomSize = minRandomSize;
    }

    public void setMaxRandomSize(int maxRandomSize) {
        this.maxRandomSize = maxRandomSize;
    }

    protected boolean tryUsePrefix(int prefix) {
        codePrefixIndex = new File(dir, "code.prefix." + (prefix));

        if (!createPrefixIndexFile()) return false;
        if (!createFileLock()) return false;
        if (!createCodeFilter()) return false;

        log.info("get available prefix index {}", prefix);

        createFileOut();
        destroyFileLockWhenShutdown();
        return true;
    }

    private boolean createFileLock() {
        if (fileLock != null) fileLock.destroy();
        fileLock = new FileLock(codePrefixIndex);
        return fileLock.tryLock();
    }

    private void createFileOut() {
        Serializes.closeQuietly(fileOutputStream);
        try {
            fileOutputStream = new FileOutputStream(codePrefixIndex);
        } catch (FileNotFoundException e) {
            // ignore
        }
    }

    private boolean createCodeFilter() {
        BitSet bitSet = Serializes.readObject(codePrefixIndex);
        if (bitSet == null || bitSet.isEmpty()) {
            log.info("create new bloom filter");
            codesFilter = new BloomFilter(
                    2.0 /* the number of bits used per element */,
                    8388608 /* 2^23, the expected number of elements the filter will contain */,
                    1 /* the number of hash functions used */);

        } else if (bitSet.cardinality() >= 2000000000) {
            log.info("old bloom filter is full with cardinality {}", bitSet.cardinality());
            return false;
        } else {
            log.info("rebuild new bloom filter with cardinality {}", bitSet.cardinality());

            codesFilter = new BloomFilter(
                    2.0 /* number of bits used per element */,
                    8388608 /* 2^23 */,
                    1 /* the number of hash functions used */,
                    bitSet);
        }

        return true;
    }

    private void destroyFileLockWhenShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                release();
            }
        });
    }

    private boolean createPrefixIndexFile() {
        try {
            codePrefixIndex.createNewFile();
            return codePrefixIndex.exists();
        } catch (IOException e) {
            log.warn("create file {} error {}", codePrefixIndex, e.getMessage());
        }
        return false;
    }

    @Override
    public int prefix() {
        return prefixIndex;
    }

    static final int MAX_RETRY_TIMES = 10;
    static final int CACHE_CODES_NUM = 1000;

    SecureRandom secureRandom = new SecureRandom();
    Queue<Integer> availableCodes = new ArrayDeque<Integer>(CACHE_CODES_NUM);

    @Override
    public int nextRandomCode() {
        if (availableCodes.isEmpty()) generate();

        return availableCodes.poll();
    }

    @Override
    public void release() {
        if (fileOutputStream != null) writeBitSetToFile();
        Serializes.closeQuietly(fileOutputStream);
        if (fileLock != null) fileLock.destroy();
    }

    private void generate() {
        for (int i = 0; i < CACHE_CODES_NUM; ++i)
            availableCodes.add(generateOne());

        writeBitSetToFile();
    }

    private int generateOne() {
        int retryTimes = -1;
        while (true) {
            if (++retryTimes == MAX_RETRY_TIMES) {
                init();
                retryTimes = 0;
            }

            boolean found = true;
            int code = -1;

            for (int size = minRandomSize; found && size <= maxRandomSize; ++size) {
                code = secureRandom.nextInt(max(size));
                found = codesFilter.mightContain(code);
            }

            if (!found) {
                codesFilter.add(code);
                if (retryTimes > 1)
                    log.info("get available code {} with {} times with prefix {}", code, retryTimes, prefix());
                return code;
            }
        }
    }

    private int max(int size) {
        switch (size) {
            case 1: // fall through
            case 2: // fall through
            case 3: // fall through
            case 4:
                return 10000;
            case 5:
                return 100000;
            case 6:
                return 1000000;
            case 7:
                return 10000000;
            case 8:
                return 100000000;
            case 9:
                return 1000000000;
            default:
                return Integer.MAX_VALUE;
        }
    }

    private void writeBitSetToFile() {
        Serializes.writeObject(fileOutputStream, codesFilter.getBitSet());
    }

}
