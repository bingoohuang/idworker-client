package org.n3r.idworker;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DayCodeTest {
    static final int CAPACITY = 1000;

    @Test
    public void test() {
        int i = 0;
        long start = System.currentTimeMillis();
        Set<String> set = new HashSet<String>(CAPACITY);
        while (i++ < CAPACITY) {
            String next = Code.next();
            assertThat(set.add(next), is(true));
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
