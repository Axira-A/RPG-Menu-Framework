package dev.rpgmenu.framework.client.layout;

import java.util.Arrays;

/** Pure integer allocator used by the top-tab widget layout and its resolution tests. */
public final class AdaptiveTabWidths {
    private AdaptiveTabWidths() {}

    public static int[] distribute(int available, int[] minimums) {
        if (available < 1 || minimums.length == 0) throw new IllegalArgumentException("Tab strip must be non-empty");
        int required = Arrays.stream(minimums).sum();
        if (required > available) throw new IllegalArgumentException("Minimum tab widths exceed the strip");
        int extra = available - required;
        int[] result = minimums.clone();
        for (int i = 0; i < result.length; i++) {
            result[i] += extra / result.length + (i < extra % result.length ? 1 : 0);
        }
        return result;
    }
}
