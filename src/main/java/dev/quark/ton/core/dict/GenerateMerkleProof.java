package dev.quark.ton.core.dict;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.dict.utils.ReadUnaryLength;

import java.math.BigInteger;
import java.util.List;

public final class GenerateMerkleProof {

    private GenerateMerkleProof() {}

    private static int ceilLog2(int x) {
        // TS: Math.ceil(Math.log2(n + 1))
        if (x <= 1) return 0;
        int p = 0;
        int v = 1;
        while (v < x) {
            v <<= 1;
            p++;
        }
        return p;
    }

    private static Cell convertToPrunedBranch(Cell c) {
        // TS:
        // exotic: true
        // bits: beginCell()
        //   .storeUint(1,8).storeUint(1,8).storeBuffer(hash).storeUint(depth,16)
        //   .endCell().beginParse().loadBits(288)
        BitString bits = Builder.beginCell()
                .storeUint(1, 8)
                .storeUint(1, 8)
                .storeBuffer(c.hash(0))
                .storeUint(c.depth(0), 16)
                .endCell()
                .beginParse()
                .loadBits(288);

        // В TS у prunedBranch refs нет.
        return Cell.exotic(bits, List.of());
    }

    private static Cell convertToMerkleProof(Cell c) {
        // TS:
        // exotic: true
        // bits: beginCell().storeUint(3,8).storeBuffer(hash).storeUint(depth,16)
        //      .endCell().beginParse().loadBits(280)
        // refs: [c]
        BitString bits = Builder.beginCell()
                .storeUint(3, 8)
                .storeBuffer(c.hash(0))
                .storeUint(c.depth(0), 16)
                .endCell()
                .beginParse()
                .loadBits(280);

        return Cell.exotic(bits, List.of(c));
    }

    private static Cell doGenerateMerkleProof(String prefix, Slice slice, int n, String key) {
        // TS: const originalCell = slice.asCell();
        Cell originalCell = slice.asCell();

        // Reading label (mutates slice)
        int lb0 = slice.loadBit() ? 1 : 0;
        int prefixLength = 0;
        String pp = prefix;

        if (lb0 == 0) {
            // Short label
            prefixLength = ReadUnaryLength.readUnaryLength(slice);

            for (int i = 0; i < prefixLength; i++) {
                pp += (slice.loadBit() ? '1' : '0');
            }
        } else {
            int lb1 = slice.loadBit() ? 1 : 0;
            if (lb1 == 0) {
                // Long label
                int lenBits = ceilLog2(n + 1);
                long pl = slice.loadUint(lenBits);
                prefixLength = (int) pl;

                for (int i = 0; i < prefixLength; i++) {
                    pp += (slice.loadBit() ? '1' : '0');
                }
            } else {
                // Same label
                char bit = slice.loadBit() ? '1' : '0';
                int lenBits = ceilLog2(n + 1);
                long pl = slice.loadUint(lenBits);
                prefixLength = (int) pl;

                for (int i = 0; i < prefixLength; i++) {
                    pp += bit;
                }
            }
        }

        if (n - prefixLength == 0) {
            return originalCell;
        } else {
            // TS: let sl = originalCell.beginParse();
            Slice sl = originalCell.beginParse();
            Cell left = sl.loadRef();
            Cell right = sl.loadRef();

            // Left branch
            if (!left.isExotic()) {
                String needPrefix = pp + '0';
                if (needPrefix.equals(key.substring(0, Math.min(key.length(), pp.length() + 1)))) {
                    left = doGenerateMerkleProof(
                            needPrefix,
                            left.beginParse(),
                            n - prefixLength - 1,
                            key
                    );
                } else {
                    left = convertToPrunedBranch(left);
                }
            }

            // Right branch
            if (!right.isExotic()) {
                String needPrefix = pp + '1';
                if (needPrefix.equals(key.substring(0, Math.min(key.length(), pp.length() + 1)))) {
                    right = doGenerateMerkleProof(
                            needPrefix,
                            right.beginParse(),
                            n - prefixLength - 1,
                            key
                    );
                } else {
                    right = convertToPrunedBranch(right);
                }
            }

            // TS:
            // return beginCell().storeSlice(sl).storeRef(left).storeRef(right).endCell();
            return Builder.beginCell()
                    .storeSlice(sl)
                    .storeRef(left)
                    .storeRef(right)
                    .endCell();
        }
    }

    private static String padStartBinary(BigInteger v, int bits) {
        // TS: keyObject.serialize(key).toString(2).padStart(bits, '0')
        String s = v.toString(2);
        if (s.length() >= bits) return s;
        StringBuilder sb = new StringBuilder(bits);
        for (int i = s.length(); i < bits; i++) {
            sb.append('0');
        }
        sb.append(s);
        return sb.toString();
    }

    public static <K, V> Cell generateMerkleProof(Dictionary<K, V> dict, K key, Dictionary.DictionaryKey<K> keyObject, Dictionary.DictionaryValue<V> valueObject) {
        Slice s = Builder.beginCell()
                .storeDictDirect(dict, keyObject, valueObject)
                .endCell()
                .beginParse();

        String binKey = padStartBinary(keyObject.serialize(key), keyObject.bits());
        Cell proofCell = doGenerateMerkleProof("", s, keyObject.bits(), binKey);
        return convertToMerkleProof(proofCell);
    }

}
