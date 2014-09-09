package org.n3r.idworker;

import org.n3r.idworker.strategy.DefaultCodeStrategy;

public class Code {
    private static CodeStrategy codeStrategy;

    static {
        configure(DefaultCodeStrategy.instance);
    }

    public static synchronized void configure(CodeStrategy custom) {
        if (codeStrategy == custom) return;
        if (codeStrategy != null) codeStrategy.release();

        codeStrategy = custom;
    }

    public static synchronized int nextRandomCode() {
        return codeStrategy.nextRandomCode();
    }

    public static int prefix() {
        return codeStrategy.prefix();
    }

    /**
     * Next Unique code.
     * The max length will be 1024-Integer.MAX-Integer.MAX(2147483647) which has 4+10+10+2*1=26 characters.
     * The min length will be 0-0.
     *
     * @return unique string code.
     */
    public static String nextCode() {
        return nextCode("-");
    }

    public static String nextCode(String separate) {
        int prefix = codeStrategy.prefix();
        if (prefix == 0) return Id.getWorkerId() + separate + nextRandomCode();

        return Id.getWorkerId() + separate + prefix() + separate + nextRandomCode();
    }
}
