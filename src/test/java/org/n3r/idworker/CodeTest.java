package org.n3r.idworker;

import org.junit.Test;
import org.n3r.idworker.strategy.DayPrefixCodeStrategy;

public class CodeTest {
    static CodeStrategy strategy;

    static {
        DayPrefixCodeStrategy dayPrefixCodeStrategy = new DayPrefixCodeStrategy("yyMM");
        dayPrefixCodeStrategy.setMinRandomSize(7);
        dayPrefixCodeStrategy.setMaxRandomSize(7);
        strategy = dayPrefixCodeStrategy;
        strategy.init();
    }

    public static synchronized String nextNo() {
        return String.format("%d-%04d-%07d", Id.getWorkerId(),
                strategy.prefix(), strategy.nextRandomCode());
    }

    @Test
    public void test() {
        int i = 0;
        long start = System.currentTimeMillis();
        while (i++ < 10000) { // cost 1095 milis
            System.out.println(nextNo());
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
