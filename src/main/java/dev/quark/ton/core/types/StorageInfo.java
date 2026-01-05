package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public final class StorageInfo implements Writable {

    public enum Format {
        OLD_NO_EXTRA,   // used + lastPaid + duePayment
        NEW_WITH_EXTRA  // used + storageExtra + lastPaid + duePayment
    }

    public final StorageUsed used;
    public final StorageExtraInfo storageExtra; // nullable
    public final long lastPaid;
    public final BigInteger duePayment; // nullable
    public final Format format;

    public StorageInfo(StorageUsed used,
                       StorageExtraInfo storageExtra,
                       long lastPaid,
                       BigInteger duePayment,
                       Format format) {
        this.used = Objects.requireNonNull(used, "used");
        this.storageExtra = storageExtra;
        this.lastPaid = lastPaid;
        this.duePayment = duePayment;
        this.format = Objects.requireNonNull(format, "format");
    }

    /** default for “new objects” */
    public StorageInfo(StorageUsed used,
                       StorageExtraInfo storageExtra,
                       long lastPaid,
                       BigInteger duePayment) {
        this(used, storageExtra, lastPaid, duePayment, Format.NEW_WITH_EXTRA);
    }

    public static StorageInfo loadStorageInfo(Slice slice) {
        Objects.requireNonNull(slice, "slice");

        // --- 1) пробуем NEW (used CURRENT: 2 поля) ---
        if (canParseNewWithCurrentUsed(slice)) {
            StorageUsed used = StorageUsed.loadStorageUsed(slice);
            StorageExtraInfo extra = StorageExtraInfo.loadStorageExtraInfo(slice);
            long lastPaid = slice.loadUint(32);
            BigInteger due = slice.loadMaybeCoins();
            return new StorageInfo(used, extra, lastPaid, due, Format.NEW_WITH_EXTRA);
        }

        // --- 2) иначе OLD (used LEGACY: 3 поля) ---
        // (tonkite-вектора почти всегда тут)
        if (canParseOldWithLegacyUsed(slice)) {
            StorageUsed used = StorageUsed.loadStorageUsedLegacy(slice);
            long lastPaid = slice.loadUint(32);
            BigInteger due = slice.loadMaybeCoins();
            return new StorageInfo(used, null, lastPaid, due, Format.OLD_NO_EXTRA);
        }

        // --- 3) крайний fallback: OLD + current used ---
        StorageUsed used = StorageUsed.loadStorageUsed(slice);
        long lastPaid = slice.loadUint(32);
        BigInteger due = slice.loadMaybeCoins();
        return new StorageInfo(used, null, lastPaid, due, Format.OLD_NO_EXTRA);
    }

    /**
     * NEW определяется так:
     * после used(2 поля) должен идти storage_extra header (3 бита), который обязан быть 0 или 1,
     * и при header=1 должны хватать бит на 256-битный dictHash.
     *
     * Это убирает ложное срабатывание на OLD, когда lastPaid начинается с '001'.
     */
    private static boolean canParseNewWithCurrentUsed(Slice slice) {
        Slice p = slice.clone();
        try {
            StorageUsed.loadStorageUsed(p);

            long header = p.loadUint(3);
            if (header == 0) {
                // нужно хотя бы lastPaid (32) + duePayment presence bit (1)
                return p.remainingBits() >= 33;
            } else if (header == 1) {
                // нужно 256 бит dictHash + lastPaid 32 + due presence bit 1
                return p.remainingBits() >= (256 + 33);
            } else {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean canParseOldWithLegacyUsed(Slice slice) {
        Slice p = slice.clone();
        try {
            StorageUsed.loadStorageUsedLegacy(p);
            // lastPaid 32 + due presence bit
            return p.remainingBits() >= 33;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void store(Builder builder) {
        if (format == Format.NEW_WITH_EXTRA) {
            // В NEW всегда current used (2 поля), как в TS
            used.storeCurrent(builder);
            builder.store(StorageExtraInfo.storeStorageExtraInfo(storageExtra));
        } else {
            // В OLD пишем used так же, как оно было прочитано
            if (used.publicCells != null) {
                used.storeLegacy(builder);
            } else {
                used.storeCurrent(builder);
            }
        }

        builder.storeUint(lastPaid, 32);
        builder.storeMaybeCoins(duePayment);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeStorageInfo(StorageInfo src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
