package org.n3r.idworker;

public interface CodeStrategy {
    int prefix();

    int nextRandomCode();

    void release();
}
