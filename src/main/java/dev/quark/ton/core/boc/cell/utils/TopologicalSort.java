package dev.quark.ton.core.boc.cell.utils;

import dev.quark.ton.core.boc.Cell;

import java.util.*;

/**
 * 1:1 port of ton-core/src/boc/cell/utils/topologicalSort.ts
 */
public final class TopologicalSort {

    private TopologicalSort() {}

    public static final class Entry {
        public final Cell cell;
        public final int[] refs;

        public Entry(Cell cell, int[] refs) {
            this.cell = cell;
            this.refs = refs;
        }
    }

    public static List<Entry> sort(Cell src) {

        // pending traversal
        List<Cell> pending = new ArrayList<>();
        pending.add(src);

        // IMPORTANT: preserve insertion order like JS Map/Set
        Map<String, Node> allCells = new LinkedHashMap<>();
        Set<String> notPermCells = new LinkedHashSet<>();
        List<String> sorted = new ArrayList<>();

        // Collect all reachable cells
        while (!pending.isEmpty()) {
            List<Cell> cells = new ArrayList<>(pending);
            pending.clear();

            for (Cell cell : cells) {
                String hash = bytesToHex(cell.hash());
                if (allCells.containsKey(hash)) {
                    continue;
                }

                notPermCells.add(hash);

                List<String> refs = new ArrayList<>();
                for (Cell r : cell.refs) {
                    refs.add(bytesToHex(r.hash()));
                    pending.add(r);
                }

                allCells.put(hash, new Node(cell, refs));
            }
        }

        // DFS with temporary marks
        Set<String> tempMark = new HashSet<>();

        class Visitor {
            void visit(String hash) {
                if (!notPermCells.contains(hash)) {
                    return;
                }
                if (tempMark.contains(hash)) {
                    throw new IllegalStateException("Not a DAG");
                }

                tempMark.add(hash);

                List<String> refs = allCells.get(hash).refs;
                // TS: for (ci = refs.length-1; ci>=0; ci--)
                for (int ci = refs.size() - 1; ci >= 0; ci--) {
                    visit(refs.get(ci));
                }

                // TS: sorted.push(hash)
                sorted.add(hash);

                tempMark.remove(hash);
                notPermCells.remove(hash);
            }
        }

        Visitor visitor = new Visitor();

        // TS: while (notPermCells.size > 0) { id = Array.from(notPermCells)[0]; visit(id); }
        while (!notPermCells.isEmpty()) {
            String id = notPermCells.iterator().next(); // insertion-order because LinkedHashSet
            visitor.visit(id);
        }

        // TS: indexes.set(sorted[sorted.length - i - 1], i)
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            indexes.put(sorted.get(sorted.size() - i - 1), i);
        }

        // TS: for i=sorted.length-1..0 result.push(...)
        List<Entry> result = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            String key = sorted.get(i);
            Node node = allCells.get(key);

            int[] refIdx = new int[node.refs.size()];
            for (int j = 0; j < node.refs.size(); j++) {
                refIdx[j] = indexes.get(node.refs.get(j));
            }
            result.add(new Entry(node.cell, refIdx));
        }

        return result;
    }

    private static final class Node {
        final Cell cell;
        final List<String> refs;

        Node(Cell cell, List<String> refs) {
            this.cell = cell;
            this.refs = refs;
        }
    }

    private static String bytesToHex(byte[] data) {
        char[] out = new char[data.length * 2];
        final char[] hex = "0123456789abcdef".toCharArray();
        int k = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            out[k++] = hex[v >>> 4];
            out[k++] = hex[v & 0x0F];
        }
        return new String(out);
    }
}
