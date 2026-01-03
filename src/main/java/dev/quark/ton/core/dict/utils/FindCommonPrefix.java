package dev.quark.ton.core.dict.utils;

import java.util.List;

public final class FindCommonPrefix {

    private FindCommonPrefix() {}

    public static String findCommonPrefix(List<String> src) {
        return findCommonPrefix(src, 0);
    }

    public static String findCommonPrefix(List<String> src, int startPos) {
        if (src.isEmpty()) return "";
        String r = src.get(0).substring(startPos);

        for (int i = 1; i < src.size(); i++) {
            String s = src.get(i);
            while (!s.startsWith(r, startPos)) {
                r = r.substring(0, r.length() - 1);
                if (r.isEmpty()) return r;
            }
        }
        return r;
    }
}
