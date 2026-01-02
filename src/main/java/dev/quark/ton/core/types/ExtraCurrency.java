package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of ton-core/src/types/ExtraCurrency.ts
 *
 * TS:
 *   export type ExtraCurrency = { [k: number]: bigint };
 *
 * Java:
 *   Map<Long, BigInteger> (recommended, because Uint(32) can exceed signed int).
 */
public final class ExtraCurrency {

    private ExtraCurrency() {}

    // ------------------------------
    // loadExtraCurrency
    // ------------------------------

    /** TS: loadExtraCurrency(data: Slice | Cell | Dictionary<number, bigint>) */
    public static Map<Long, BigInteger> loadExtraCurrency(Slice data) {
        Dictionary<Long, BigInteger> ecDict = Dictionary.loadDirect(
                Dictionary.Keys.Uint(32),
                Dictionary.Values.BigVarUint(5),
                data
        );
        return dictToMap(ecDict);
    }

    public static Map<Long, BigInteger> loadExtraCurrency(Cell data) {
        // Если у тебя есть overload loadDirect(..., Cell) — можешь использовать его.
        return loadExtraCurrency(data.beginParse());
    }

    public static Map<Long, BigInteger> loadExtraCurrency(Dictionary<Long, BigInteger> data) {
        return dictToMap(data);
    }

    // ------------------------------
    // loadMaybeExtraCurrency
    // ------------------------------

    /** TS: loadMaybeExtraCurrency(data: Slice) */
    public static Map<Long, BigInteger> loadMaybeExtraCurrency(Slice data) {
        Cell ecData = data.loadMaybeRef();
        return ecData == null ? null : loadExtraCurrency(ecData);
    }

    // ------------------------------
    // storeExtraCurrency
    // ------------------------------

    /** TS: storeExtraCurrency(extracurrency) => (builder) => builder.storeDict(packExtraCurrencyDict(extracurrency)) */
    public static Writable storeExtraCurrency(Map<Long, BigInteger> extracurrency) {
        return (Builder builder) -> builder.storeDict(
                packExtraCurrencyDict(extracurrency),
                Dictionary.Keys.Uint(32),
                Dictionary.Values.BigVarUint(5)
        );
    }

    // ------------------------------
    // packExtraCurrencyDict / packExtraCurrencyCell
    // ------------------------------

    /** TS: packExtraCurrencyDict(extracurrency) */
    public static Dictionary<Long, BigInteger> packExtraCurrencyDict(Map<Long, BigInteger> extracurrency) {
        Dictionary<Long, BigInteger> res = Dictionary.empty(
                Dictionary.Keys.Uint(32),
                Dictionary.Values.BigVarUint(5)
        );

        if (extracurrency != null) {
            for (Map.Entry<Long, BigInteger> e : extracurrency.entrySet()) {
                res.set(e.getKey(), e.getValue());
            }
        }

        return res;
    }

    /** TS: packExtraCurrencyCell(extracurrency) */
    public static Cell packExtraCurrencyCell(Map<Long, BigInteger> extracurrency) {
        return Builder.beginCell()
                .storeDictDirect(
                        packExtraCurrencyDict(extracurrency),
                        Dictionary.Keys.Uint(32),
                        Dictionary.Values.BigVarUint(5)
                )
                .endCell();
    }

    // ------------------------------
    // internal helper
    // ------------------------------

    private static Map<Long, BigInteger> dictToMap(Dictionary<Long, BigInteger> ecDict) {
        Map<Long, BigInteger> ecMap = new LinkedHashMap<>();
        for (var kv : ecDict) {
            // предполагаю, что итератор отдаёт пары (k,v) как entry/Pair.
            // Если у тебя другой API итерации — подставь его.
            ecMap.put(kv.getKey(), kv.getValue());
        }
        return ecMap;
    }
}
