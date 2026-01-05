import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.StorageExtraInfo;
import dev.quark.ton.core.types.StorageInfo;
import dev.quark.ton.core.types.StorageUsed;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class StorageInfoTest {

    // TS expected hashes:
    // 1) without storageExtra (null)
    private static final String HASH_NO_EXTRA =
            "9c7b98a341201e6492f2bcf144215e1ddbef6774126caa57784fbce25597d4f7";

    // 2) with storageExtra { dictHash: 0n }
    private static final String HASH_WITH_EXTRA_DICT0 =
            "7672b3010077582fa477bf2b070183412aa0d1a9b98ba8c437b9d90b37b6a559";

    @Test
    void shouldStoreStorageInfo_noExtra_matchTsHash() {
        StorageUsed used = makeUsed22_5705();

        StorageInfo info = new StorageInfo(
                used,
                null,               // storageExtra
                1748811232L,        // lastPaid
                null                // duePayment
        );

        Cell c = Builder.beginCell()
                .store(StorageInfo.storeStorageInfo(info))
                .endCell();

        assertEquals(HASH_NO_EXTRA, toHex(c.hash()));
    }

    @Test
    void shouldStoreStorageInfo_withExtraDictHash0_matchTsHash() {
        StorageUsed used = makeUsed22_5705();

        StorageInfo info = new StorageInfo(
                used,
                new StorageExtraInfo(BigInteger.ZERO), // dictHash = 0
                1748811232L,
                null
        );

        Cell c = Builder.beginCell()
                .store(StorageInfo.storeStorageInfo(info))
                .endCell();

        assertEquals(HASH_WITH_EXTRA_DICT0, toHex(c.hash()));
    }

    @Test
    void shouldParseAndStoreRoundtrip() {
        StorageUsed used = makeUsed22_5705();

        StorageInfo info = new StorageInfo(
                used,
                new StorageExtraInfo(BigInteger.ZERO),
                1748811232L,
                null
        );

        Cell c1 = Builder.beginCell()
                .store(StorageInfo.storeStorageInfo(info))
                .endCell();

        StorageInfo parsed = StorageInfo.loadStorageInfo(c1.beginParse());

        Cell c2 = Builder.beginCell()
                .store(StorageInfo.storeStorageInfo(parsed))
                .endCell();

        assertTrue(c1.equals(c2));
    }

    /**
     * Подстрой под твой StorageUsed API.
     * Часто это (cells,bits) как BigInteger/long.
     */
    private static StorageUsed makeUsed22_5705() {
        // В TS ожидается: used: { cells: 22n, bits: 5705n }
        // Если у тебя конструктор/фабрика другая — поправь здесь 1 строку.
        return new StorageUsed(BigInteger.valueOf(22), BigInteger.valueOf(5705));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
