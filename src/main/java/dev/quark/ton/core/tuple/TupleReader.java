package dev.quark.ton.core.tuple;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static dev.quark.ton.core.tuple.Tuple.*;

/**
 * 1:1 port of ton-core/src/tuple/reader.ts :contentReference[oaicite:1]{index=1}
 */
public final class TupleReader {

    private final ArrayList<TupleItem> items;

    public TupleReader(List<TupleItem> items) {
        this.items = new ArrayList<>(items);
    }

    public int remaining() {
        return items.size();
    }

    public TupleItem peek() {
        if (items.isEmpty()) {
            throw new IllegalStateException("EOF");
        }
        return items.get(0);
    }

    public TupleItem pop() {
        if (items.isEmpty()) {
            throw new IllegalStateException("EOF");
        }
        return items.remove(0);
    }

    public TupleReader skip() {
        return skip(1);
    }

    public TupleReader skip(int num) {
        for (int i = 0; i < num; i++) {
            pop();
        }
        return this;
    }

    public BigInteger readBigNumber() {
        TupleItem popped = pop();
        if (!(popped instanceof TupleItemInt ti)) {
            throw new IllegalStateException("Not a number");
        }
        return ti.value();
    }

    public BigInteger readBigNumberOpt() {
        TupleItem popped = pop();
        if (popped instanceof TupleItemNull) {
            return null;
        }
        if (!(popped instanceof TupleItemInt ti)) {
            throw new IllegalStateException("Not a number");
        }
        return ti.value();
    }

    public long readNumber() {
        return readBigNumber().longValue();
    }

    public Long readNumberOpt() {
        BigInteger r = readBigNumberOpt();
        return (r != null) ? r.longValue() : null;
    }

    public boolean readBoolean() {
        long res = readNumber();
        return res != 0;
    }

    public Boolean readBooleanOpt() {
        Long res = readNumberOpt();
        return (res != null) ? (res != 0) : null;
    }

    public Address readAddress() {
        Address r = readCell().beginParse().loadAddress();
        if (r != null) {
            return r;
        }
        throw new IllegalStateException("Not an address");
    }

    public Address readAddressOpt() {
        Cell r = readCellOpt();
        if (r != null) {
            return r.beginParse().loadMaybeAddress();
        }
        return null;
    }

    public Cell readCell() {
        TupleItem popped = pop();
        if (!(popped instanceof TupleItemCell
                || popped instanceof TupleItemSlice
                || popped instanceof TupleItemBuilder)) {
            throw new IllegalStateException("Not a cell: " + popped.type());
        }
        if (popped instanceof TupleItemCell tc) return tc.cell();
        if (popped instanceof TupleItemSlice ts) return ts.cell();
        return ((TupleItemBuilder) popped).cell();
    }

    public Cell readCellOpt() {
        TupleItem popped = pop();
        if (popped instanceof TupleItemNull) {
            return null;
        }
        if (!(popped instanceof TupleItemCell
                || popped instanceof TupleItemSlice
                || popped instanceof TupleItemBuilder)) {
            throw new IllegalStateException("Not a cell");
        }
        if (popped instanceof TupleItemCell tc) return tc.cell();
        if (popped instanceof TupleItemSlice ts) return ts.cell();
        return ((TupleItemBuilder) popped).cell();
    }

    public TupleReader readTuple() {
        TupleItem popped = pop();
        if (!(popped instanceof TupleItemTuple tt)) {
            throw new IllegalStateException("Not a tuple");
        }
        return new TupleReader(tt.items());
    }

    public TupleReader readTupleOpt() {
        TupleItem popped = pop();
        if (popped instanceof TupleItemNull) {
            return null;
        }
        if (!(popped instanceof TupleItemTuple tt)) {
            throw new IllegalStateException("Not a tuple");
        }
        return new TupleReader(tt.items());
    }

    private static List<TupleItem> readLispList(TupleReader reader) {
        ArrayList<TupleItem> result = new ArrayList<>();
        TupleReader tail = reader;

        while (tail != null) {
            TupleItem head = tail.pop();

            if (tail.items.isEmpty()
                    || !(tail.items.get(0) instanceof TupleItemTuple || tail.items.get(0) instanceof TupleItemNull)) {
                throw new IllegalStateException("Lisp list consists only from (any, tuple) elements and ends with null");
            }

            tail = tail.readTupleOpt();
            result.add(head);
        }

        return result;
    }

    public List<TupleItem> readLispListDirect() {
        if (items.size() == 1 && items.get(0) instanceof TupleItemNull) {
            return List.of();
        }
        return readLispList(this);
    }

    public List<TupleItem> readLispList() {
        return readLispList(this.readTupleOpt());
    }

    public byte[] readBuffer() {
        Slice s = readCell().beginParse();

        if (s.remainingRefs() != 0) {
            throw new IllegalStateException("Not a buffer");
        }
        if (s.remainingBits() % 8 != 0) {
            throw new IllegalStateException("Not a buffer");
        }
        return s.loadBuffer(s.remainingBits() / 8);
    }

    public byte[] readBufferOpt() {
        TupleItem popped = peek();
        if (popped instanceof TupleItemNull) {
            return null;
        }

        Slice s = readCell().beginParse();
        if (s.remainingRefs() != 0) {
            throw new IllegalStateException("Not a buffer");
        }
        if (s.remainingBits() % 8 != 0) {
            throw new IllegalStateException("Not a buffer");
        }
        return s.loadBuffer(s.remainingBits() / 8);
    }

    public String readString() {
        return readCell().beginParse().loadStringTail();
    }

    public String readStringOpt() {
        TupleItem popped = peek();
        if (popped instanceof TupleItemNull) {
            return null;
        }
        return readCell().beginParse().loadStringTail();
    }
}
