package org.n3r.idworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

public class IdWorker {
    protected long epoch = 1387886498127L; // 2013-12-24 20:01:38.127

    protected long workerIdBits;
    protected long maxWorkerId;
    protected long sequenceBits;

    protected long workerIdShift;
    protected long timestampLeftShift;
    protected long sequenceMask;

    protected long lastMillis = -1L;

    protected final long workerId;
    protected long sequence = 0L;
    protected Logger logger = LoggerFactory.getLogger(IdWorker.class);

    public IdWorker(long workerId) {
        workerIdBits = workerIdBits();
        maxWorkerId = -1L ^ (-1L << workerIdBits);
        sequenceBits = sequenceBits();

        workerIdShift = sequenceBits;
        timestampLeftShift = sequenceBits + workerIdBits;
        sequenceMask = -1L ^ (-1L << sequenceBits);

        this.workerId = checkWorkerId(workerId);

        logger.debug("worker starting. timestamp left shift {}, worker id bits {}, sequence bits {}, worker id {}",
                timestampLeftShift, workerIdBits, sequenceBits, workerId);
    }

    public long getEpoch() {
        return epoch;
    }

    public long workerIdBits() {
        return 10L;
    }

    public long sequenceBits() {
        return 11L;
    }

    private long checkWorkerId(long workerId) {
        // sanity check for workerId
        if (workerId > maxWorkerId || workerId < 0) {
            int rand = new SecureRandom().nextInt((int) maxWorkerId + 1);
            logger.warn("worker Id can't be greater than {} or less than 0, use a random {}", maxWorkerId, rand);
            return rand;
        }

        return workerId;
    }

    public synchronized long nextId() {
        long timestamp = millisGen();

        if (timestamp < lastMillis) {
            logger.error("clock is moving backwards.  Rejecting requests until {}.", lastMillis);
            throw new InvalidSystemClock(String.format(
                    "Clock moved backwards.  Refusing to generate id for {} milliseconds", lastMillis - timestamp));
        }

        if (lastMillis == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0)
                timestamp = tilNextMillis(lastMillis);
        } else {
            sequence = 0;
        }

        lastMillis = timestamp;
        long diff = timestamp - getEpoch();
        return (diff << timestampLeftShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    protected long tilNextMillis(long lastMillis) {
        long millis = millisGen();
        while (millis <= lastMillis)
            millis = millisGen();

        return millis;
    }

    protected long millisGen() {
        return System.currentTimeMillis();
    }

    public long getLastMillis() {
        return lastMillis;
    }

    public long getWorkerId() {
        return workerId;
    }
}
