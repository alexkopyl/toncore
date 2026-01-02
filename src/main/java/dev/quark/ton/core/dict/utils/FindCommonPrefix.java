package dev.quark.ton.core.dict.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FindCommonPrefix {

    private FindCommonPrefix() {}

    /** 1:1 port of findCommonPrefix.ts */
    public static String findCommonPrefix(List<String> src) {

        // Corner cases
        if (src.size() == 0) {
            return "";
        }
        if (src.size() == 1) {
            return src.get(0);
        }

        // Searching for prefix
        List<String> sorted = new ArrayList<>(src);
        Collections.sort(sorted);

        int size = 0;
        String a = sorted.get(0);
        String b = sorted.get(sorted.size() - 1);

        int limit = Math.min(a.length(), b.length());
        for (int i = 0; i < limit; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                break;
            }
            size++;
        }

        // NOTE: TS returns src[0].slice(0,size), not sorted[0]
        String firstOriginal = src.get(0);
        if (size > firstOriginal.length()) {
            size = firstOriginal.length();
        }
        return firstOriginal.substring(0, size);
    }
}
