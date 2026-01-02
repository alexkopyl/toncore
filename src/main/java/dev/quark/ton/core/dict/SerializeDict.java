package dev.quark.ton.core.dict;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.dict.utils.FindCommonPrefix;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;

import static dev.quark.ton.core.boc.Builder.beginCell;

/**
 * 1:1 port of ton-core/src/dict/serializeDict.ts
 */
public final class SerializeDict {

    private SerializeDict() {}

    // -------------------------------------------------------------------------
    // Tree build (Edge/Node)
    // -------------------------------------------------------------------------

    private static String pad(String src, int size) {
        String s = src;
        while (s.length() < size) {
            s = "0" + s;
        }
        return s;
    }

    private interface Node<T> {}

    private static final class Fork<T> implements Node<T> {
        final Edge<T> left;
        final Edge<T> right;

        Fork(Edge<T> left, Edge<T> right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class Leaf<T> implements Node<T> {
        final T value;

        Leaf(T value) {
            this.value = value;
        }
    }

    private static final class Edge<T> {
        final String label;
        final Node<T> node;

        Edge(String label, Node<T> node) {
            this.label = label;
            this.node = node;
        }
    }

    private static <T> Map<String, T> removePrefixMap(Map<String, T> src, int length) {
        if (length == 0) {
            return src;
        } else {
            Map<String, T> res = new LinkedHashMap<>();
            for (Map.Entry<String, T> e : src.entrySet()) {
                res.put(e.getKey().substring(length), e.getValue());
            }
            return res;
        }
    }

    private static final class Forked<T> {
        final Map<String, T> left;
        final Map<String, T> right;

        Forked(Map<String, T> left, Map<String, T> right) {
            this.left = left;
            this.right = right;
        }
    }

    private static <T> Forked<T> forkMap(Map<String, T> src) {
        if (src.size() == 0) {
            throw new IllegalStateException("Internal inconsistency");
        }

        Map<String, T> left = new LinkedHashMap<>();
        Map<String, T> right = new LinkedHashMap<>();

        for (Map.Entry<String, T> e : src.entrySet()) {
            String k = e.getKey();
            T d = e.getValue();
            if (k.startsWith("0")) {
                left.put(k.substring(1), d);
            } else {
                right.put(k.substring(1), d);
            }
        }

        if (left.size() == 0) {
            throw new IllegalStateException("Internal inconsistency. Left emtpy.");
        }
        if (right.size() == 0) {
            throw new IllegalStateException("Internal inconsistency. Right emtpy.");
        }

        return new Forked<>(left, right);
    }

    private static <T> Node<T> buildNode(Map<String, T> src) {
        if (src.size() == 0) {
            throw new IllegalStateException("Internal inconsistency");
        }
        if (src.size() == 1) {
            return new Leaf<>(src.values().iterator().next());
        }
        Forked<T> forked = forkMap(src);
        return new Fork<>(
                buildEdge(forked.left),
                buildEdge(forked.right)
        );
    }

    private static <T> Edge<T> buildEdge(Map<String, T> src) {
        if (src.size() == 0) {
            throw new IllegalStateException("Internal inconsistency");
        }

        // TS: findCommonPrefix(Array.from(src.keys()))
        String label = FindCommonPrefix.findCommonPrefix(new ArrayList<>(src.keySet()));
        return new Edge<>(label, buildNode(removePrefixMap(src, label.length())));
    }

    /** TS: export function buildTree(src, keyLength) */
    public static <T> Edge<T> buildTree(Map<BigInteger, T> src, int keyLength) {

        // Convert map keys: Map<bigint,T> -> Map<string,T> (binary padded)
        Map<String, T> converted = new LinkedHashMap<>();
        for (Map.Entry<BigInteger, T> e : src.entrySet()) {
            String padded = pad(e.getKey().toString(2), keyLength);
            converted.put(padded, e.getValue());
        }

        // Calculate root label
        return buildEdge(converted);
    }

    // -------------------------------------------------------------------------
    // Label encoding
    // -------------------------------------------------------------------------

    /** TS: writeLabelShort(src,to) */
    public static Builder writeLabelShort(String src, Builder to) {

        // Header: 0
        to.storeBit(false);

        // Unary length: src.length times "1", then "0"
        for (int i = 0; i < src.length(); i++) {
            to.storeBit(true);
        }
        to.storeBit(false);

        // Value
        for (int i = 0; i < src.length(); i++) {
            to.storeBit(src.charAt(i) == '1');
        }

        return to;
    }

    private static int labelShortLength(String src) {
        // 1 (header) + len (unary ones) + 1 (unary zero) + len (bits)
        return 1 + src.length() + 1 + src.length();
    }

    private static int ceilLog2(int x) {
        // TS: Math.ceil(Math.log2(x))
        if (x <= 1) return 0;
        int p = 0;
        int v = 1;
        while (v < x) {
            v <<= 1;
            p++;
        }
        return p;
    }

    /** TS: writeLabelLong(src,keyLength,to) */
    public static Builder writeLabelLong(String src, int keyLength, Builder to) {

        // Header: 10
        to.storeBit(true);
        to.storeBit(false);

        // Length
        int lengthBits = ceilLog2(keyLength + 1);
        to.storeUint((long) src.length(), lengthBits);

        // Value
        for (int i = 0; i < src.length(); i++) {
            to.storeBit(src.charAt(i) == '1');
        }

        return to;
    }

    private static int labelLongLength(String src, int keyLength) {
        return 1 + 1 + ceilLog2(keyLength + 1) + src.length();
    }

    /** TS: writeLabelSame(value,length,keyLength,to) */
    public static void writeLabelSame(boolean value, int length, int keyLength, Builder to) {

        // Header: 11
        to.storeBit(true);
        to.storeBit(true);

        // Value
        to.storeBit(value);

        // Length
        int lenLen = ceilLog2(keyLength + 1);
        to.storeUint((long) length, lenLen);
    }

    private static int labelSameLength(int keyLength) {
        return 1 + 1 + 1 + ceilLog2(keyLength + 1);
    }

    private static boolean isSame(String src) {
        if (src.length() == 0 || src.length() == 1) {
            return true;
        }
        char first = src.charAt(0);
        for (int i = 1; i < src.length(); i++) {
            if (src.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    /** TS: detectLabelType(src,keyLength) */
    public static String detectLabelType(String src, int keyLength) {
        String kind = "short";
        int kindLength = labelShortLength(src);

        int longLength = labelLongLength(src, keyLength);
        if (longLength < kindLength) {
            kindLength = longLength;
            kind = "long";
        }

        if (isSame(src)) {
            int sameLength = labelSameLength(keyLength);
            if (sameLength < kindLength) {
                kindLength = sameLength;
                kind = "same";
            }
        }

        return kind;
    }

    private static void writeLabel(String src, int keyLength, Builder to) {
        String type = detectLabelType(src, keyLength);
        if (type.equals("short")) {
            writeLabelShort(src, to);
        }
        if (type.equals("long")) {
            writeLabelLong(src, keyLength, to);
        }
        if (type.equals("same")) {
            writeLabelSame(src.charAt(0) == '1', src.length(), keyLength, to);
        }
    }

    // -------------------------------------------------------------------------
    // Edge / Node writing
    // -------------------------------------------------------------------------

    private static <T> void writeNode(Node<T> src, int keyLength, BiConsumer<T, Builder> serializer, Builder to) {
        if (src instanceof Leaf<T> leaf) {
            serializer.accept(leaf.value, to);
        }
        if (src instanceof Fork<T> fork) {
            Builder leftCell = beginCell();
            Builder rightCell = beginCell();

            // TS: writeEdge(left, keyLength - 1)
            writeEdge(fork.left, keyLength - 1, serializer, leftCell);
            writeEdge(fork.right, keyLength - 1, serializer, rightCell);

            to.storeRef(leftCell.endCell());
            to.storeRef(rightCell.endCell());
        }
    }

    private static <T> void writeEdge(Edge<T> src, int keyLength, BiConsumer<T, Builder> serializer, Builder to) {
        // label
        writeLabel(src.label, keyLength, to);

        // node
        writeNode(src.node, keyLength - src.label.length(), serializer, to);
    }

    /** TS: export function serializeDict(src,keyLength,serializer,to) */
    public static <T> void serializeDict(Map<BigInteger, T> src, int keyLength, BiConsumer<T, Builder> serializer, Builder to) {
        Edge<T> tree = buildTree(src, keyLength);
        writeEdge(tree, keyLength, serializer, to);
    }
}
