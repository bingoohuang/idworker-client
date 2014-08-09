package org.n3r.idworker;

import org.n3r.idworker.strategy.DefaultWorkerIdStrategy;

public class Id {
    private static WorkerIdStatrategy workerIdStrategy;
    private static IdWorker idWorker;

    static {
        configure(DefaultWorkerIdStrategy.instance);
    }

    public static synchronized void configure(WorkerIdStatrategy custom) {
        if (workerIdStrategy == custom) return;

        if (workerIdStrategy != null) workerIdStrategy.release();
        workerIdStrategy = custom;
        workerIdStrategy.initialize();
        idWorker = new IdWorker(workerIdStrategy.availableWorkerId());
    }

    public static long next() {
        return idWorker.nextId();
    }
}
