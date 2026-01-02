package dev.quark.ton.core.boc.utils;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 1:1 port of boc/strings.ts
 *
 * Encoding: UTF-8 (same as Node.js Buffer default for toString()/from()).
 */
public final class Strings {

    private Strings() {}

    // ===== Public API =====

    public static String readString(Slice slice) {
        byte[] bytes = readBuffer(slice);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Cell stringToCell(String src) {
        Builder b = Builder.beginCell();
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        writeBuffer(bytes, 0, bytes.length, b);
        return b.endCell();
    }


    public static void writeString(String src, Builder builder) {
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        writeBuffer(bytes, 0, bytes.length, builder);
    }

    // ===== Internal helpers =====

    private static byte[] readBuffer(Slice slice) {
        // Check consistency
        int remBits = slice.remainingBits();
        if (remBits % 8 != 0) {
            throw new IllegalArgumentException("Invalid string length: " + remBits);
        }
        int remRefs = slice.remainingRefs();
        if (remRefs != 0 && remRefs != 1) {
            throw new IllegalArgumentException("invalid number of refs: " + remRefs);
        }

        // Read bytes from current slice tail
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int remBytes = remBits / 8;
        if (remBytes > 0) {
            byte[] part = slice.loadBuffer(remBytes);
            out.writeBytes(part);
        }

        // Read tail continuation
        if (slice.remainingRefs() == 1) {
            Cell ref = slice.loadRef();
            byte[] tail = readBuffer(ref.beginParse());
            out.writeBytes(tail);
        }

        return out.toByteArray();
    }

    private static void writeBuffer(byte[] src, int off, int len, Builder builder) {
        if (len <= 0) return;

        int bytesCanFit = builder.availableBits() / 8;
        if (bytesCanFit <= 0) {
            // No room in this cell: put everything into a ref chain
            Builder bb = Builder.beginCell();
            writeBuffer(src, off, len, bb);
            builder.storeRef(bb.endCell());
            return;
        }

        if (len > bytesCanFit) {
            // Split: write head here, tail into a ref cell
            builder.storeBuffer(sliceOf(src, off, bytesCanFit));

            Builder bb = Builder.beginCell();
            writeBuffer(src, off + bytesCanFit, len - bytesCanFit, bb);
            builder.storeRef(bb.endCell());
        } else {
            builder.storeBuffer(sliceOf(src, off, len));
        }
    }

    private static byte[] sliceOf(byte[] src, int off, int len) {
        byte[] r = new byte[len];
        System.arraycopy(src, off, r, 0, len);
        return r;
    }
}
