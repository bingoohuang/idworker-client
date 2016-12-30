package org.n3r.idworker;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IdWorkerIntTest {

    @Test
    public void onlyUniqueIds() {
        IdWorkerInt worker = new IdWorkerInt(31);
        Set<Integer> set = new HashSet<>();
        int n = 2000000;
        for (int i = 0; i < 2000000; ++i) {
            int id = worker.nextIdInt();
            if (set.contains(id)) {
                System.out.println(id);
            } else {
                set.add(id);
            }
        }

        assertThat(set.size(), is(n));
    }

    @Test
    public void onlyUniqueIds2() {
        Set<Integer> set = new HashSet<>();
        int n = 2000000;
        for (int i = 0; i < 2000000; ++i) {
            int id = Id.nextInt();
            if (set.contains(id)) {
                System.out.println(id);
            } else {
                set.add(id);
            }
        }

        assertThat(set.size(), is(n));
    }
}
