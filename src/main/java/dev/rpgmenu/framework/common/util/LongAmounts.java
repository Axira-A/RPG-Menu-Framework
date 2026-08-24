package dev.rpgmenu.framework.common.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class LongAmounts {
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final long[] DIVISORS = {1_000_000_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L};
    private static final String[] SUFFIXES = {"E", "P", "T", "B", "M", "K"};

    private LongAmounts() {}
    public static String compact(long amount) {
        long absolute = amount == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(amount);
        for (int i = 0; i < DIVISORS.length; i++) {
            if (absolute >= DIVISORS[i]) return ONE_DECIMAL.format((double)amount / DIVISORS[i]) + SUFFIXES[i];
        }
        return Long.toString(amount);
    }
}
