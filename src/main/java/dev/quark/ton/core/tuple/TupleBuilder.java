package dev.quark.ton.core.tuple;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static dev.quark.ton.core.tuple.Tuple.*;

/**
 * 1:1 port of ton-core/src/tuple/builder.ts
 */
public final class TupleBuilder {

    private final ArrayList<TupleItem> tuple = new ArrayList<>();

    public TupleBuilder writeNumber(BigInteger v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            tuple.add(new TupleItemInt(v));
        }
        return this;
    }

    public TupleBuilder writeNumber(Long v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            tuple.add(new TupleItemInt(BigInteger.valueOf(v)));
        }
        return this;
    }

    public TupleBuilder writeBoolean(Boolean v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            tuple.add(new TupleItemInt(v ? BigInteger.valueOf(-1) : BigInteger.ZERO));
        }
        return this;
    }

    public TupleBuilder writeBuffer(byte[] v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            Cell c = Builder.beginCell().storeBuffer(v).endCell();
            tuple.add(new TupleItemSlice(c));
        }
        return this;
    }

    public TupleBuilder writeString(String v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            Cell c = Builder.beginCell().storeStringTail(v).endCell();
            tuple.add(new TupleItemSlice(c));
        }
        return this;
    }

    public TupleBuilder writeCell(Object v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else if (v instanceof Cell c) {
            tuple.add(new TupleItemCell(c));
        } else if (v instanceof Slice s) {
            tuple.add(new TupleItemCell(s.asCell()));
        } else {
            throw new IllegalArgumentException("Expected Cell or Slice");
        }
        return this;
    }

    public TupleBuilder writeSlice(Object v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else if (v instanceof Cell c) {
            tuple.add(new TupleItemSlice(c));
        } else if (v instanceof Slice s) {
            tuple.add(new TupleItemSlice(s.asCell()));
        } else {
            throw new IllegalArgumentException("Expected Cell or Slice");
        }
        return this;
    }

    public TupleBuilder writeBuilder(Object v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else if (v instanceof Cell c) {
            tuple.add(new TupleItemBuilder(c));
        } else if (v instanceof Slice s) {
            tuple.add(new TupleItemBuilder(s.asCell()));
        } else {
            throw new IllegalArgumentException("Expected Cell or Slice");
        }
        return this;
    }

    public TupleBuilder writeTuple(List<TupleItem> v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            tuple.add(new TupleItemTuple(v));
        }
        return this;
    }

    public TupleBuilder writeAddress(Address v) {
        if (v == null) {
            tuple.add(new TupleItemNull());
        } else {
            Cell c = Builder.beginCell().storeAddress(v).endCell();
            tuple.add(new TupleItemSlice(c));
        }
        return this;
    }

    public List<TupleItem> build() {
        return List.copyOf(tuple);
    }
}
