package dev.quark.ton.core.tuple;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 1:1 port of ton-core/src/tuple/tuple.ts
 */
public final class Tuple {

    private Tuple() {}

    private static final BigInteger INT64_MIN = new BigInteger("-9223372036854775808");
    private static final BigInteger INT64_MAX = new BigInteger("9223372036854775807");

    // ---------------------------------------------------------------------
    // TupleItem union
    // ---------------------------------------------------------------------

    public sealed interface TupleItem
            permits TupleItemNull, TupleItemInt, TupleItemNaN, TupleItemCell, TupleItemSlice, TupleItemBuilder, TupleItemTuple {
        String type();
    }

    public static final class TupleItemNull implements TupleItem {
        @Override public String type() { return "null"; }

        @Override public boolean equals(Object o) { return o instanceof TupleItemNull; }
        @Override public int hashCode() { return 0; }
        @Override public String toString() { return "{type:null}"; }
    }

    public static final class TupleItemInt implements TupleItem {
        private final BigInteger value;

        public TupleItemInt(BigInteger value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public BigInteger value() { return value; }

        @Override public String type() { return "int"; }

        @Override public boolean equals(Object o) {
            return (o instanceof TupleItemInt other) && value.equals(other.value);
        }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public String toString() { return "{type:int,value=" + value + "}"; }
    }

    public static final class TupleItemNaN implements TupleItem {
        @Override public String type() { return "nan"; }

        @Override public boolean equals(Object o) { return o instanceof TupleItemNaN; }
        @Override public int hashCode() { return 1; }
        @Override public String toString() { return "{type:nan}"; }
    }

    public static final class TupleItemCell implements TupleItem {
        private final Cell cell;

        public TupleItemCell(Cell cell) {
            this.cell = Objects.requireNonNull(cell, "cell");
        }

        public Cell cell() { return cell; }

        @Override public String type() { return "cell"; }

        @Override public boolean equals(Object o) {
            return (o instanceof TupleItemCell other) && cell.equals(other.cell);
        }
        @Override public int hashCode() { return cell.hashCode(); }
        @Override public String toString() { return "{type:cell}"; }
    }

    public static final class TupleItemSlice implements TupleItem {
        private final Cell cell;

        public TupleItemSlice(Cell cell) {
            this.cell = Objects.requireNonNull(cell, "cell");
        }

        public Cell cell() { return cell; }

        @Override public String type() { return "slice"; }

        @Override public boolean equals(Object o) {
            return (o instanceof TupleItemSlice other) && cell.equals(other.cell);
        }
        @Override public int hashCode() { return cell.hashCode() * 31 + 2; }
        @Override public String toString() { return "{type:slice}"; }
    }

    public static final class TupleItemBuilder implements TupleItem {
        private final Cell cell;

        public TupleItemBuilder(Cell cell) {
            this.cell = Objects.requireNonNull(cell, "cell");
        }

        public Cell cell() { return cell; }

        @Override public String type() { return "builder"; }

        @Override public boolean equals(Object o) {
            return (o instanceof TupleItemBuilder other) && cell.equals(other.cell);
        }
        @Override public int hashCode() { return cell.hashCode() * 31 + 3; }
        @Override public String toString() { return "{type:builder}"; }
    }

    public static final class TupleItemTuple implements TupleItem {
        private final List<TupleItem> items;

        public TupleItemTuple(List<TupleItem> items) {
            this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        }

        public List<TupleItem> items() { return items; }

        @Override public String type() { return "tuple"; }

        @Override public boolean equals(Object o) {
            return (o instanceof TupleItemTuple other) && items.equals(other.items);
        }
        @Override public int hashCode() { return items.hashCode(); }
        @Override public String toString() { return "{type:tuple,len=" + items.size() + "}"; }
    }

    // ---------------------------------------------------------------------
    // serializeTuple / parseTuple
    // ---------------------------------------------------------------------

    public static Cell serializeTuple(List<TupleItem> src) {
        Builder builder = Builder.beginCell();
        builder.storeUint(src.size(), 24);

        ArrayList<TupleItem> r = new ArrayList<>(src);
        serializeTupleTail(r, builder);

        return builder.endCell();
    }

    public static List<TupleItem> parseTuple(Cell src) {
        ArrayList<TupleItem> res = new ArrayList<>();
        Slice cs = src.beginParse();

        int size = (int) cs.loadUint(24);
        for (int i = 0; i < size; i++) {
            Cell next = cs.loadRef();
            res.add(0, parseStackItem(cs));
            cs = next.beginParse();
        }

        return res;
    }

    // ---------------------------------------------------------------------
    // Internal: stack item codec
    // ---------------------------------------------------------------------

    private static void serializeTupleTail(List<TupleItem> src, Builder builder) {
        if (!src.isEmpty()) {

            Builder tail = Builder.beginCell();
            serializeTupleTail(src.subList(0, src.size() - 1), tail);
            builder.storeRef(tail.endCell());

            serializeTupleItem(src.get(src.size() - 1), builder);
        }
    }

    private static void serializeTupleItem(TupleItem src, Builder builder) {

        if (src instanceof TupleItemNull) {
            builder.storeUint(0x00, 8);
            return;
        }

        if (src instanceof TupleItemInt ti) {
            BigInteger v = ti.value();
            if (v.compareTo(INT64_MAX) <= 0 && v.compareTo(INT64_MIN) >= 0) {
                builder.storeUint(0x01, 8);
                builder.storeInt(v, 64);
            } else {
                builder.storeUint(0x0100, 15);
                builder.storeInt(v, 257);
            }
            return;
        }

        if (src instanceof TupleItemNaN) {
            builder.storeInt(BigInteger.valueOf(0x02ffL), 16);
            return;
        }

        if (src instanceof TupleItemCell tc) {
            builder.storeUint(0x03, 8);
            builder.storeRef(tc.cell());
            return;
        }

        if (src instanceof TupleItemSlice ts) {
            builder.storeUint(0x04, 8);

            // In TS they use src.cell.bits.length and src.cell.refs.length.
            // In Java we derive via beginParse().
            Slice s = ts.cell().beginParse();
            int endBits = s.remainingBits();
            int endRefs = s.remainingRefs();

            builder.storeUint(0, 10);
            builder.storeUint(endBits, 10);
            builder.storeUint(0, 3);
            builder.storeUint(endRefs, 3);
            builder.storeRef(ts.cell());
            return;
        }

        if (src instanceof TupleItemBuilder tb) {
            builder.storeUint(0x05, 8);
            builder.storeRef(tb.cell());
            return;
        }

        if (src instanceof TupleItemTuple tt) {
            Cell head = null;
            Cell tail = null;

            List<TupleItem> items = tt.items();
            for (int i = 0; i < items.size(); i++) {

                // swap head/tail
                Cell s = head;
                head = tail;
                tail = s;

                if (i > 1) {
                    head = Builder.beginCell()
                            .storeRef(Objects.requireNonNull(tail))
                            .storeRef(Objects.requireNonNull(head))
                            .endCell();
                }

                Builder bc = Builder.beginCell();
                serializeTupleItem(items.get(i), bc);
                tail = bc.endCell();
            }

            builder.storeUint(0x07, 8);
            builder.storeUint(items.size(), 16);

            if (head != null) {
                builder.storeRef(head);
            }
            if (tail != null) {
                builder.storeRef(tail);
            }
            return;
        }

        throw new IllegalArgumentException("Invalid value");
    }

    private static TupleItem parseStackItem(Slice cs) {
        int kind = (int) cs.loadUint(8);

        if (kind == 0) {
            return new TupleItemNull();
        } else if (kind == 1) {
            return new TupleItemInt(cs.loadIntBig(64));
        } else if (kind == 2) {
            long x = cs.loadUint(7);
            if (x == 0) {
                return new TupleItemInt(cs.loadIntBig(257));
            } else {
                cs.loadBit(); // must eq 1
                return new TupleItemNaN();
            }
        } else if (kind == 3) {
            return new TupleItemCell(cs.loadRef());
        } else if (kind == 4) {
            int startBits = (int) cs.loadUint(10);
            int endBits = (int) cs.loadUint(10);
            int startRefs = (int) cs.loadUint(3);
            int endRefs = (int) cs.loadUint(3);

            Slice rs = cs.loadRef().beginParse();
            rs.skip(startBits);
            var dt = rs.loadBits(endBits - startBits);

            Builder builder = Builder.beginCell().storeBits(dt);

            if (startRefs < endRefs) {
                for (int i = 0; i < startRefs; i++) {
                    rs.loadRef();
                }
                for (int i = 0; i < endRefs - startRefs; i++) {
                    builder.storeRef(rs.loadRef());
                }
            }

            return new TupleItemSlice(builder.endCell());
        } else if (kind == 5) {
            return new TupleItemBuilder(cs.loadRef());
        } else if (kind == 7) {
            int length = (int) cs.loadUint(16);
            ArrayList<TupleItem> items = new ArrayList<>();

            if (length > 1) {
                Slice head = cs.loadRef().beginParse();
                Slice tail = cs.loadRef().beginParse();
                items.add(0, parseStackItem(tail));

                for (int i = 0; i < length - 2; i++) {
                    Slice ohead = head;
                    head = ohead.loadRef().beginParse();
                    tail = ohead.loadRef().beginParse();
                    items.add(0, parseStackItem(tail));
                }

                items.add(0, parseStackItem(head));
            } else if (length == 1) {
                items.add(parseStackItem(cs.loadRef().beginParse()));
            }

            return new TupleItemTuple(items);
        } else {
            throw new IllegalArgumentException("Unsupported stack item");
        }
    }
}
