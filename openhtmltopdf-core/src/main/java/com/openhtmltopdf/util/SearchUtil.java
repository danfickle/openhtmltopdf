package com.openhtmltopdf.util;

public class SearchUtil {
    public interface IntComparator {
        /**
         * Return 0 for a match, -1 (too low) if we should continue
         * searching after the index and +1 (too high) if we should continue
         * searching before the index.
         */
        int compare(int index);
    }

    /**
     * Method to search over any kind of sorted range.
     * The comparator is called with an int following the binary search
     * algorithm until a match or the range is exhausted.
     * @return the matching value or -1 if we exhausted the range. 
     */
    public static <T> int intBinarySearch(
            IntComparator comparator,
            int startIndex,
            int count
            ) {
        int low = startIndex;
        int high = startIndex + count - 1;

        while (low <= high) {
            int mid = (low + high) >> 1;
            int comparison = comparator.compare(mid);

            if (comparison == 0) {
                return mid;
            } else if (comparison < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return -1;
    }

}
