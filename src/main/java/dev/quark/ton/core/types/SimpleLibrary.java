package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.util.function.Consumer;

/**
 * TL-B: simple_lib$_ public:Bool root:^Cell = SimpleLib;
 * 1:1 port of SimpleLibrary.ts
 */
public final class SimpleLibrary {
    public final boolean isPublic; // TS field name: public
    public final Cell root;

    public SimpleLibrary(boolean isPublic, Cell root) {
        this.isPublic = isPublic;
        this.root = root;
    }

    public static SimpleLibrary loadSimpleLibrary(Slice slice) {
        return new SimpleLibrary(slice.loadBit(), slice.loadRef());
    }

    public static Consumer<Builder> storeSimpleLibrary(SimpleLibrary src) {
        return (builder) -> {
            builder.storeBit(src.isPublic);
            builder.storeRef(src.root);
        };
    }

    /**
     * TS: export const SimpleLibraryValue: DictionaryValue<SimpleLibrary>
     */
    public static final Dictionary.DictionaryValue<SimpleLibrary> SimpleLibraryValue =
            new Dictionary.DictionaryValue<>() {
                @Override
                public void serialize(SimpleLibrary src, Builder builder) {
                    storeSimpleLibrary(src).accept(builder);
                }

                @Override
                public SimpleLibrary parse(Slice src) {
                    return loadSimpleLibrary(src);
                }
            };
}
