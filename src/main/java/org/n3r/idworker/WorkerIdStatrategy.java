package org.n3r.idworker;

public interface WorkerIdStatrategy {
    void initialize();

    long availableWorkerId();

    void release();
}
