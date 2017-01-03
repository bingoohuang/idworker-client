package org.n3r.idworker;

public class IdWorkerInt extends IdWorker {

    public IdWorkerInt(long workerId) {
        super(workerId & (~(-1L << 5L)));
    }

    @Override
    public long workerIdBits() {
        return 5L;
    }

    @Override
    public long sequenceBits() {
        return 5L;
    }

    public synchronized int nextIdInt() {
        int nextIdInt = (int) super.nextId();
        return (nextIdInt << 1) >>> 1;
    }
}
