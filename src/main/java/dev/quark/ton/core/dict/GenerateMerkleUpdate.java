package dev.quark.ton.core.dict;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.BitString;

import java.util.List;

public final class GenerateMerkleUpdate {

    private GenerateMerkleUpdate() {}

    private static Cell convertToMerkleUpdate(Cell c1, Cell c2) {
        // TS:
        // exotic: true
        // bits: beginCell()
        //   .storeUint(4,8)
        //   .storeBuffer(c1.hash(0))
        //   .storeBuffer(c2.hash(0))
        //   .storeUint(c1.depth(0),16)
        //   .storeUint(c2.depth(0),16)
        //   .endCell().beginParse().loadBits(552)
        // refs: [c1, c2]
        BitString bits = Builder.beginCell()
                .storeUint(4, 8)
                .storeBuffer(c1.hash(0))
                .storeBuffer(c2.hash(0))
                .storeUint(c1.depth(0), 16)
                .storeUint(c2.depth(0), 16)
                .endCell()
                .beginParse()
                .loadBits(552);

        return Cell.exotic(bits, List.of(c1, c2));
    }

    public static <K, V> Cell generateMerkleUpdate(
            Dictionary<K, V> dict,
            K key,
            Dictionary.DictionaryKey<K> keyObject,
            Dictionary.DictionaryValue<V> valueObject,
            V newValue
    ) {
        // TS: const oldProof = generateMerkleProof(...).refs[0];
        Cell oldProof = GenerateMerkleProof
                .generateMerkleProof(dict, List.of(key), keyObject, valueObject)
                .refs.get(0);

        dict.set(key, newValue);

        Cell newProof = GenerateMerkleProof
                .generateMerkleProof(dict, List.of(key), keyObject, valueObject)
                .refs.get(0);

        return convertToMerkleUpdate(oldProof, newProof);
    }

}
