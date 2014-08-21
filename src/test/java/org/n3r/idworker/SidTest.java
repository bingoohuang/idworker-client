package org.n3r.idworker;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SidTest {
    @Test
    public void test1() {
        System.out.println(Sid.next());
        System.out.println(Sid.nextShort());
        for (int i = 0; i < 10000; ++i) {
            assertThat(Sid.next().length(), is(21));
            assertThat(Sid.nextShort().length(), is(16));
        }
    }


}
