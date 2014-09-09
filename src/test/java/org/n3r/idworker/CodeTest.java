package org.n3r.idworker;

import org.junit.Test;

public class CodeTest {
    @Test
    public void test() {
        int i = 0;
        long start = System.currentTimeMillis();
        while (i++ < 10000) { // cost 1095 milis
            System.out.println(Code.nextCode());
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
