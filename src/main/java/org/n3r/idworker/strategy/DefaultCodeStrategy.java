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

public class DefaultCodeStrategy implements org.n3r.idworker.CodeStrategy {
    public static final CodeStrategy instance = new DefaultCodeStrategy();

    static final int MAX_PREFIX_INDEX = Integer.MAX_VALUE;
    Logger log = LoggerFactory.getLogger(DefaultCodeStrategy.class);
    FileLock fileLock;
    BloomFilter codesFilter;
    FileOutputStream fileOutputStream;
    int prefixIndex;
    File codePrefixIndex;

    public DefaultCodeStrategy() {
        checkAvailPrefix();
    }

    private void checkAvailPrefix() {
        release();

        File dir = new File(System.getProperty("user.home") + File.separator + ".idworkers");

        prefixIndex = -1;
        while (++prefixIndex < MAX_PREFIX_INDEX) {
            codePrefixIndex = new File(dir, "code.prefix." + (prefixIndex));

            if (!createPrefixIndexFile()) continue;
            if (!createFileLock()) continue;
            if (!createCodeFilter()) continue;

            log.info("get available prefix index {}", prefixIndex);

            createFileOut();
            destroyFileLockWhenShutdown();
            break;
        }

        if (prefixIndex == MAX_PREFIX_INDEX)
            throw new RuntimeException("all prefixes are used up, the world maybe ends!");
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
                    1.0 /* the number of bits used per element */,
                    8388608 /* 2^23, the expected number of elements the filter will contain */,
                    1 /* the number of hash functions used */);

        } else if (bitSet.cardinality() > 8388608) {
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

    static final int MAX_RETRY_TIMES = 10;
    static final int CACHE_CODES_NUM = 1000;

    SecureRandom secureRandom = new SecureRandom();
    Queue<Integer> availableCodes = new ArrayDeque<Integer>(CACHE_CODES_NUM);

    @Override
    public int prefix() {
        return prefixIndex;
    }

    @Override
    public int nextRandomCode() {
        if (availableCodes.isEmpty()) generate();

        return availableCodes.poll();
    }

    @Override
    public void release() {
        Serializes.closeQuietly(fileOutputStream);
        fileLock.destroy();
    }

    private void generate() {
        for (int i = 0; i < CACHE_CODES_NUM; ++i) {
            availableCodes.add(generateOne());
        }

        writeBitSetToFile();
    }

    private int generateOne() {
        int retryTimes = -1;
        while (true) {
            if (++retryTimes == MAX_RETRY_TIMES) {
                writeBitSetToFile();
                checkAvailPrefix();
                retryTimes = 0;
            }


            int code = secureRandom.nextInt(100000); // 5
            boolean found = codesFilter.mightContain(code);

            if (found) {
                code = secureRandom.nextInt(1000000); // 6
                found = codesFilter.mightContain(code);
            }
            if (found) {
                code = secureRandom.nextInt(10000000); // 7
                found = codesFilter.mightContain(code);
            }
            if (found) {
                code = secureRandom.nextInt(100000000); // 8
                found = codesFilter.mightContain(code);
            }
            if (found) {
                code = secureRandom.nextInt(1000000000); // 9
                found = codesFilter.mightContain(code);
            }
            if (found) {
                code = secureRandom.nextInt(Integer.MAX_VALUE);
                found = codesFilter.mightContain(code);
            }

            if (!found) {
                codesFilter.add(code);
                if (retryTimes > 1)
                    log.info("get available code {} with {} times with prefix {}", code, retryTimes, prefixIndex);
                return code;
            }
        }
    }

    private void writeBitSetToFile() {
        Serializes.writeObject(fileOutputStream, codesFilter.getBitSet());
    }
}
