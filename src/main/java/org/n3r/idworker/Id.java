package org.n3r.idworker;

import org.n3r.idworker.strategy.DefaultWorkerIdStrategy;

public class Id {
    private static WorkerIdStrategy workerIdStrategy;
    private static IdWorker idWorker;
    private static IdWorkerInt idWorkerInt;

    static {
        configure(DefaultWorkerIdStrategy.instance);
    }

    public static synchronized void configure(WorkerIdStrategy custom) {
        if (workerIdStrategy == custom) return;

        if (workerIdStrategy != null) workerIdStrategy.release();
        workerIdStrategy = custom;
        workerIdStrategy.initialize();
        long availableWorkerId = workerIdStrategy.availableWorkerId();
        idWorker = new IdWorker(availableWorkerId);
        idWorkerInt = new IdWorkerInt(availableWorkerId & (~(-1L << 5L)));
    }

    public static long next() {
        return idWorker.nextId();
    }

    public static int nextInt() {
        return idWorkerInt.nextIdInt();
    }

    public static long getWorkerId() {
        return idWorker.getWorkerId();
    }
}
