package org.n3r.idworker;

public interface CodeStrategy {
    void init();

    int prefix();

    int nextRandomCode();

    void release();
}
