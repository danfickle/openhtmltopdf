package com.openhtmltopdf.layout;

public class CounterLanguage {
    static String toRoman(int val) {
        int[] ints = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] nums = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            int count = val / ints[i];
            for (int j = 0; j < count; j++) {
                sb.append(nums[i]);
            }
            val -= ints[i] * count;
        }
        return sb.toString();
    }

    static String toLatin(int val) {
        String result = "";
        val -= 1;
        while (val >= 0) {
            int letter = val % 26;
            val = val / 26 - 1;
            result = ((char) (letter + 65)) + result;
        }
        return result;
    }
}