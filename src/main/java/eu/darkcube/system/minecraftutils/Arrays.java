package eu.darkcube.system.minecraftutils;

import java.util.Comparator;

class Arrays {
    private static final int QUICKSORT_NO_REC = 16;
    private static final int QUICKSORT_MEDIAN_OF_9 = 128;

    private static int med3(final int a, final int b, final int c, final IntComparator comp) {
        final int ab = comp.compare(a, b);
        final int ac = comp.compare(a, c);
        final int bc = comp.compare(b, c);
        return (ab < 0 ?
                (bc < 0 ? b : ac < 0 ? c : a) :
                (bc > 0 ? b : ac > 0 ? c : a));
    }

    @FunctionalInterface
    public interface Swapper {
        /**
         * Swaps the data at the given positions.
         *
         * @param a the first position to swap.
         * @param b the second position to swap.
         */
        void swap(int a, int b);
    }

    @FunctionalInterface
    public interface IntComparator extends Comparator<Integer> {
        /**
         * Compares its two primitive-type arguments for order. Returns a negative integer, zero, or a
         * positive integer as the first argument is less than, equal to, or greater than the second.
         *
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         * @see java.util.Comparator
         */
        int compare(int k1, int k2);

        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation delegates to the corresponding type-specific method.
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        @Override
        default int compare(Integer ok1, Integer ok2) {
            return compare(ok1.intValue(), ok2.intValue());
        }

        /**
         * Return a new comparator that first uses this comparator, then uses the second comparator if this
         * comparator compared the two elements as equal.
         *
         * @see Comparator#thenComparing(Comparator)
         */
        default IntComparator thenComparing(IntComparator second) {
            return (IntComparator & java.io.Serializable) (k1, k2) -> {
                int comp = compare(k1, k2);
                return comp == 0 ? second.compare(k1, k2) : comp;
            };
        }

        @Override
        default Comparator<Integer> thenComparing(Comparator<? super Integer> second) {
            if (second instanceof IntComparator) return thenComparing((IntComparator) second);
            return Comparator.super.thenComparing(second);
        }
    }

    public static void quickSort(final int from, final int to, final IntComparator comp, final Swapper swapper) {
        final int len = to - from;
        // Insertion sort on smallest arrays
        if (len < QUICKSORT_NO_REC) {
            for (int i = from; i < to; i++)
                for (int j = i; j > from && (comp.compare(j - 1, j) > 0); j--) {
                    swapper.swap(j, j - 1);
                }
            return;
        }

        // Choose a partition element, v
        int m = from + len / 2; // Small arrays, middle element
        int l = from;
        int n = to - 1;
        if (len > QUICKSORT_MEDIAN_OF_9) { // Big arrays, pseudomedian of 9
            final int s = len / 8;
            l = med3(l, l + s, l + 2 * s, comp);
            m = med3(m - s, m, m + s, comp);
            n = med3(n - 2 * s, n - s, n, comp);
        }
        m = med3(l, m, n, comp); // Mid-size, med of 3
        // int v = x[m];

        int a = from;
        int b = a;
        int c = to - 1;
        // Establish Invariant: v* (<v)* (>v)* v*
        int d = c;
        while (true) {
            int comparison;
            while (b <= c && ((comparison = comp.compare(b, m)) <= 0)) {
                if (comparison == 0) {
                    // Fix reference to pivot if necessary
                    if (a == m) m = b;
                    else if (b == m) m = a;
                    swapper.swap(a++, b);
                }
                b++;
            }
            while (c >= b && ((comparison = comp.compare(c, m)) >= 0)) {
                if (comparison == 0) {
                    // Fix reference to pivot if necessary
                    if (c == m) m = d;
                    else if (d == m) m = c;
                    swapper.swap(c, d--);
                }
                c--;
            }
            if (b > c) break;
            // Fix reference to pivot if necessary
            if (b == m) m = d;
            swapper.swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        s = Math.min(a - from, b - a);
        swap(swapper, from, b - s, s);
        s = Math.min(d - c, to - d - 1);
        swap(swapper, b, to - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) quickSort(from, from + s, comp, swapper);
        if ((s = d - c) > 1) quickSort(to - s, to, comp, swapper);
    }

    protected static void swap(final Swapper swapper, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) swapper.swap(a, b);
    }
}
