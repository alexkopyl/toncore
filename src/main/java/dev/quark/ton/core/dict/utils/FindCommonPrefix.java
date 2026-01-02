package dev.quark.ton.core.dict.utils;

import java.util.List;

public final class FindCommonPrefix {

    private FindCommonPrefix() {}

    public static String findCommonPrefix(List<String> src) {
        return findCommonPrefix(src, 0);
    }

    /** 1:1 port of findCommonPrefix(src: string[], startPos = 0) */
    public static String findCommonPrefix(List<String> src, int startPos) {

        // Corner cases
        if (src.isEmpty()) {
            return "";
        }

        String first = src.get(0);
        int sp = Math.max(0, startPos);
        if (sp > first.length()) {
            sp = first.length();
        }

        String r = first.substring(sp);

        for (int i = 1; i < src.size(); i++) {
            String s = src.get(i);

            while (!startsWithAt(s, r, sp)) {
                r = r.substring(0, r.length() - 1);
                if (r.isEmpty()) {
                    return r;
                }
            }
        }

        return r;
    }

    private static boolean startsWithAt(String s, String r, int startPos) {
        if (r.isEmpty()) return true;
        if (startPos < 0) return false;
        if (startPos > s.length()) return false;
        return s.startsWith(r, startPos);
    }
}
