package org.n3r.idworker.bloomfilter.hash;

public interface HashFunction {

    int hash(String data, int seed);

}