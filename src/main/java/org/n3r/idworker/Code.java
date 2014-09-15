package org.n3r.idworker;

import org.n3r.idworker.strategy.DefaultCodeStrategy;

public class Code {
    private static CodeStrategy strategy;

    static {
        CodeStrategy strategy = new DefaultCodeStrategy();
        strategy.init();
        configure(strategy);
    }

    public static synchronized void configure(CodeStrategy custom) {
        if (strategy == custom) return;
        if (strategy != null) strategy.release();

        strategy = custom;
    }


    /**
     * Next Unique code.
     * The max length will be 1024-Integer.MAX-Integer.MAX(2147483647) which has 4+10+10+2*1=26 characters.
     * The min length will be 0-0.
     *
     * @return unique string code.
     */
    public static String nextCode() {
        int prefix = strategy.prefix();
        if (prefix == 0) return Id.getWorkerId() + "-" + strategy.nextRandomCode();

        return String.format("%d-%d-%d", Id.getWorkerId(), prefix, strategy.nextRandomCode());
    }
}
