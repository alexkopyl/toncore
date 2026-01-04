package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.function.Consumer;

/**
 * TL-B:
 * extra_currencies$_ dict:(HashmapE 32 (VarUInteger 32)) = ExtraCurrencyCollection;
 * currencies$_ grams:Grams other:ExtraCurrencyCollection = CurrencyCollection;
 *
 * 1:1 port of CurrencyCollection.ts
 */
public final class CurrencyCollection {

    // other?: Maybe<Dictionary<number, bigint>>
    // TS number => Java Long (safe int)
    private final Dictionary<Long, BigInteger> other; // nullable

    // coins: bigint
    private final BigInteger coins;

    public CurrencyCollection(Dictionary<Long, BigInteger> other, BigInteger coins) {
        this.other = other;
        this.coins = coins;
    }

    public static CurrencyCollection of(Dictionary<Long, BigInteger> other, BigInteger coins) {
        return new CurrencyCollection(other, coins);
    }

    public Dictionary<Long, BigInteger> other() {
        return other;
    }

    public BigInteger coins() {
        return coins;
    }

    // ---------------------------------------------------------------------
    // loadCurrencyCollection(slice)
    // ---------------------------------------------------------------------
    public static CurrencyCollection loadCurrencyCollection(Slice slice) {
        BigInteger coins = slice.loadCoins();

        Dictionary<Long, BigInteger> other =
                slice.loadDict(
                        Dictionary.Keys.Uint(32),
                        Dictionary.Values.BigVarUint(5) // log2(32)
                );

        if (other != null && other.size() == 0) {
            return new CurrencyCollection(null, coins);
        } else {
            return new CurrencyCollection(other, coins);
        }
    }

    // ---------------------------------------------------------------------
    // storeCurrencyCollection(collection)
    // ---------------------------------------------------------------------
    // ---------------------------------------------------------------------
// storeCurrencyCollection(collection)
// ---------------------------------------------------------------------
    public static Consumer<Builder> storeCurrencyCollection(CurrencyCollection collection) {
        return builder -> {
            builder.storeCoins(collection.coins());
            if (collection.other() != null) {
                builder.storeDict(
                        collection.other(),
                        Dictionary.Keys.Uint(32),
                        Dictionary.Values.BigVarUint(5)
                );
            } else {
                builder.storeBit(0);
            }
        };
    }
}
