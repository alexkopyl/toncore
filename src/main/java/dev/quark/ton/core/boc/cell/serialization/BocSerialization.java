package dev.quark.ton.core.boc.cell.serialization;

import dev.quark.ton.core.boc.*;
import dev.quark.ton.core.boc.cell.descriptor.Descriptor;
import dev.quark.ton.core.boc.cell.utils.TopologicalSort;
import dev.quark.ton.core.boc.utils.PaddedBits;
import dev.quark.ton.core.utils.BitsForNumber;
import dev.quark.ton.core.utils.Crc32c;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/serialization.ts
 */
public final class BocSerialization {

    private BocSerialization() {}

    /* ============================================================ */
    /* ======================= helpers ============================ */
    /* ============================================================ */

    private static int getHashesCount(int levelMask) {
        return getHashesCountFromMask(levelMask & 7);
    }

    private static int getHashesCountFromMask(int mask) {
        int n = 0;
        for (int i = 0; i < 3; i++) {
            n += (mask & 1);
            mask >>= 1;
        }
        return n + 1;
    }

    private static ReadCellResult readCell(BitReader reader, int sizeBytes) {

        // D1
        int d1 = (int) reader.loadUint(8);
        int refsCount = d1 % 8;
        boolean exotic = (d1 & 8) != 0;

        // D2
        int d2 = (int) reader.loadUint(8);
        int dataByteSize = (int) Math.ceil(d2 / 2.0);
        boolean paddingAdded = (d2 % 2) != 0;

        int levelMask = d1 >> 5;
        boolean hasHashes = (d1 & 16) != 0;

        int hashesSize = hasHashes ? getHashesCount(levelMask) * 32 : 0;
        int depthSize = hasHashes ? getHashesCount(levelMask) * 2 : 0;

        reader.skip((hashesSize + depthSize) * 8);

        // Bits
        BitString bits = BitString.EMPTY;
        if (dataByteSize > 0) {
            if (paddingAdded) {
                bits = reader.loadPaddedBits(dataByteSize * 8);
            } else {
                bits = reader.loadBits(dataByteSize * 8);
            }
        }

        // Refs
        int[] refs = new int[refsCount];
        for (int i = 0; i < refsCount; i++) {
            refs[i] = (int) reader.loadUint(sizeBytes * 8);
        }

        return new ReadCellResult(bits, refs, exotic);
    }

    private static int calcCellSize(Cell cell, int sizeBytes) {
        return 2 + (int) Math.ceil(cell.bits.length() / 8.0) + cell.refs.size() * sizeBytes;
    }

    /* ============================================================ */
    /* ======================= parsing ============================ */
    /* ============================================================ */

    public static ParsedBoc parseBoc(byte[] src) {

        BitReader reader = new BitReader(new BitString(src, 0, src.length * 8));
        long magic = reader.loadUint(32);

        if (magic == 0x68ff65f3L || magic == 0xacc3a728L) {

            int size = (int) reader.loadUint(8);
            int offBytes = (int) reader.loadUint(8);
            int cells = (int) reader.loadUint(size * 8);
            int roots = (int) reader.loadUint(size * 8);
            int absent = (int) reader.loadUint(size * 8);
            int totalCellSize = (int) reader.loadUint(offBytes * 8);

            byte[] index = reader.loadBuffer(cells * offBytes);
            byte[] cellData = reader.loadBuffer(totalCellSize);

            if (magic == 0xacc3a728L) {
                byte[] crc32 = reader.loadBuffer(4);
                byte[] calc = Crc32c.crc32c(slice(src, 0, src.length - 4));
                if (!java.util.Arrays.equals(calc, crc32)) {
                    throw new IllegalStateException("Invalid CRC32C");
                }
            }

            return new ParsedBoc(size, offBytes, cells, roots, absent, totalCellSize, index, cellData, new int[]{0});
        }

        if (magic == 0xb5ee9c72L) {

            boolean hasIdx = reader.loadUint(1) == 1;
            boolean hasCrc32c = reader.loadUint(1) == 1;
            reader.loadUint(1); // cache bits
            reader.loadUint(2); // flags

            int size = (int) reader.loadUint(3);
            int offBytes = (int) reader.loadUint(8);
            int cells = (int) reader.loadUint(size * 8);
            int roots = (int) reader.loadUint(size * 8);
            int absent = (int) reader.loadUint(size * 8);
            int totalCellSize = (int) reader.loadUint(offBytes * 8);

            int[] root = new int[roots];
            for (int i = 0; i < roots; i++) {
                root[i] = (int) reader.loadUint(size * 8);
            }

            byte[] index = hasIdx ? reader.loadBuffer(cells * offBytes) : null;
            byte[] cellData = reader.loadBuffer(totalCellSize);

            if (hasCrc32c) {
                byte[] crc32 = reader.loadBuffer(4);
                byte[] calc = Crc32c.crc32c(slice(src, 0, src.length - 4));
                if (!java.util.Arrays.equals(calc, crc32)) {
                    throw new IllegalStateException("Invalid CRC32C");
                }
            }

            return new ParsedBoc(size, offBytes, cells, roots, absent, totalCellSize, index, cellData, root);
        }

        throw new IllegalStateException("Invalid magic");
    }

    public static List<Cell> deserializeBoc(byte[] src) {

        ParsedBoc boc = parseBoc(src);
        BitReader reader = new BitReader(new BitString(boc.cellData, 0, boc.cellData.length * 8));

        List<TempCell> cells = new ArrayList<>();
        for (int i = 0; i < boc.cells; i++) {
            ReadCellResult r = readCell(reader, boc.size);
            cells.add(new TempCell(r.bits, r.refs, r.exotic));
        }

        for (int i = cells.size() - 1; i >= 0; i--) {
            TempCell tc = cells.get(i);
            List<Cell> refs = new ArrayList<>();
            for (int r : tc.refs) {
                if (cells.get(r).result == null) {
                    throw new IllegalStateException("Invalid BOC file");
                }
                refs.add(cells.get(r).result);
            }

            Cell.Options opts = new Cell.Options();
            opts.bits = tc.bits;
            opts.exotic = tc.exotic;
            opts.refs = refs;
            tc.result = new Cell(opts);
        }

        List<Cell> roots = new ArrayList<>();
        for (int r : boc.root) {
            roots.add(cells.get(r).result);
        }

        return roots;
    }

    /* ============================================================ */
    /* ======================= serialization ====================== */
    /* ============================================================ */

    public static byte[] serializeBoc(Cell root, boolean idx, boolean crc32) {

        List<TopologicalSort.Entry> all = TopologicalSort.sort(root);
        int cellsNum = all.size();

        int sizeBytes = Math.max(
                (int) Math.ceil(BitsForNumber.bitsForNumber(cellsNum, BitsForNumber.Mode.UINT) / 8.0),
                1
        );

        int totalCellSize = 0;
        int[] index = new int[cellsNum];
        for (int i = 0; i < cellsNum; i++) {
            totalCellSize += calcCellSize(all.get(i).cell, sizeBytes);
            index[i] = totalCellSize; // end offset
        }

        int offsetBytes = Math.max(
                (int) Math.ceil(BitsForNumber.bitsForNumber(totalCellSize, BitsForNumber.Mode.UINT) / 8.0),
                1
        );

        int totalBits =
                (4 + 1 + 1 + 3 * sizeBytes + offsetBytes + sizeBytes +
                        (idx ? cellsNum * offsetBytes : 0) +
                        totalCellSize + (crc32 ? 4 : 0)) * 8;

        BitBuilder builder = new BitBuilder(totalBits);

        builder.writeUint(0xb5ee9c72L, 32);
        builder.writeBit(idx);
        builder.writeBit(crc32);
        builder.writeBit(false);
        builder.writeUint(0, 2);
        builder.writeUint(sizeBytes, 3);
        builder.writeUint(offsetBytes, 8);
        builder.writeUint(cellsNum, sizeBytes * 8);
        builder.writeUint(1, sizeBytes * 8);
        builder.writeUint(0, sizeBytes * 8);
        builder.writeUint(totalCellSize, offsetBytes * 8);
        builder.writeUint(0, sizeBytes * 8);

        if (idx) {
            for (int i : index) {
                builder.writeUint(i, offsetBytes * 8);
            }
        }

        for (TopologicalSort.Entry e : all) {
            writeCellToBuilder(e.cell, e.refs, sizeBytes, builder);
        }

        if (crc32) {
            byte[] crc = Crc32c.crc32c(builder.buffer());
            builder.writeBuffer(crc);
        }

        return builder.buffer();
    }

    private static void writeCellToBuilder(Cell cell, int[] refs, int sizeBytes, BitBuilder to) {
        int d1 = Descriptor.getRefsDescriptor(cell.refs, cell.mask.value(), cell.type);
        int d2 = Descriptor.getBitsDescriptor(cell.bits);
        to.writeUint(d1, 8);
        to.writeUint(d2, 8);
        to.writeBuffer(PaddedBits.bitsToPaddedBuffer(cell.bits));
        for (int r : refs) {
            to.writeUint(r, sizeBytes * 8);
        }
    }

    /* ============================================================ */
    /* ======================= structs ============================ */
    /* ============================================================ */

    private record ReadCellResult(BitString bits, int[] refs, boolean exotic) {}

    private static final class TempCell {
        final BitString bits;
        final int[] refs;
        final boolean exotic;
        Cell result;

        TempCell(BitString bits, int[] refs, boolean exotic) {
            this.bits = bits;
            this.refs = refs;
            this.exotic = exotic;
        }
    }

    private static final class ParsedBoc {
        final int size, offBytes, cells, roots, absent, totalCellSize;
        final byte[] index, cellData;
        final int[] root;

        ParsedBoc(int size, int offBytes, int cells, int roots, int absent,
                  int totalCellSize, byte[] index, byte[] cellData, int[] root) {
            this.size = size;
            this.offBytes = offBytes;
            this.cells = cells;
            this.roots = roots;
            this.absent = absent;
            this.totalCellSize = totalCellSize;
            this.index = index;
            this.cellData = cellData;
            this.root = root;
        }
    }

    private static byte[] slice(byte[] src, int from, int to) {
        byte[] r = new byte[to - from];
        System.arraycopy(src, from, r, 0, r.length);
        return r;
    }
}
