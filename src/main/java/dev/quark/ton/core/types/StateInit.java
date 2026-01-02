package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.function.Consumer;

/**
 * TL-B:
 * StateInit _ split_depth:(Maybe (## 5)) special:(Maybe TickTock)
 *          code:(Maybe ^Cell) data:(Maybe ^Cell)
 *          library:(HashmapE 256 SimpleLib) = StateInit;
 *
 * 1:1 port of ton-core/src/tlb/StateInit.ts
 *
 * NOTE: "Maybe" in TS is represented by nullable fields in Java.
 */
public final class StateInit {

    // split_depth:(Maybe (## 5))
    private final Long splitDepth;               // nullable

    // special:(Maybe TickTock)
    private final TickTock special;              // nullable

    // code:(Maybe ^Cell) data:(Maybe ^Cell)
    private final Cell code;                     // nullable
    private final Cell data;                     // nullable

    // library:(HashmapE 256 SimpleLib)
    private final Dictionary<BigInteger, SimpleLibrary> libraries; // nullable

    public StateInit(
            Long splitDepth,
            TickTock special,
            Cell code,
            Cell data,
            Dictionary<BigInteger, SimpleLibrary> libraries
    ) {
        this.splitDepth = splitDepth;
        this.special = special;
        this.code = code;
        this.data = data;
        this.libraries = libraries;
    }

    public Long splitDepth() { return splitDepth; }
    public TickTock special() { return special; }
    public Cell code() { return code; }
    public Cell data() { return data; }
    public Dictionary<BigInteger, SimpleLibrary> libraries() { return libraries; }

    // ---------------------------------------------------------------------
    // loadStateInit(slice): StateInit
    // ---------------------------------------------------------------------
    public static StateInit loadStateInit(Slice slice) {

        // Split Depth
        Long splitDepth = null;
        if (slice.loadBit()) {
            splitDepth = slice.loadUint(5);
        }

        // TickTock
        TickTock special = null;
        if (slice.loadBit()) {
            special = TickTock.loadTickTock(slice);
        }

        // Code and Data
        Cell code = slice.loadMaybeRef();
        Cell data = slice.loadMaybeRef();

        // Libs
        Dictionary<BigInteger, SimpleLibrary> libraries =
                slice.loadDict(Dictionary.Keys.BigUint(256), SimpleLibrary.SimpleLibraryValue);

        if (libraries != null && libraries.size() == 0) {
            libraries = null;
        }

        return new StateInit(splitDepth, special, code, data, libraries);
    }

    // ---------------------------------------------------------------------
    // storeStateInit(src) => (builder) => void
    // ---------------------------------------------------------------------
    public static Consumer<Builder> storeStateInit(StateInit src) {
        return (builder) -> {
            if (src.splitDepth != null) {
                builder.storeBit(true);
                builder.storeUint(src.splitDepth, 5);
            } else {
                builder.storeBit(false);
            }

            if (src.special != null) {
                builder.storeBit(true);
                builder.store(TickTock.storeTickTock(src.special));
            } else {
                builder.storeBit(false);
            }

            builder.storeMaybeRef(src.code);
            builder.storeMaybeRef(src.data);

            builder.storeDict(
                    src.libraries,
                    Dictionary.Keys.BigUint(256),
                    SimpleLibrary.SimpleLibraryValue
            );
        };
    }
}
