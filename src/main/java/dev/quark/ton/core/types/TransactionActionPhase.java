package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public final class TransactionActionPhase implements Writable {

    public final boolean success;
    public final boolean valid;
    public final boolean noFunds;
    public final AccountStatusChange statusChange;

    public final BigInteger totalFwdFees;    // nullable
    public final BigInteger totalActionFees; // nullable

    public final int resultCode;
    public final Integer resultArg; // nullable

    public final int totalActions;
    public final int specActions;
    public final int skippedActions;
    public final int messagesCreated;

    public final BigInteger actionListHash; // uint256
    public final StorageUsed totalMessageSize;

    public TransactionActionPhase(
            boolean success,
            boolean valid,
            boolean noFunds,
            AccountStatusChange statusChange,
            BigInteger totalFwdFees,
            BigInteger totalActionFees,
            int resultCode,
            Integer resultArg,
            int totalActions,
            int specActions,
            int skippedActions,
            int messagesCreated,
            BigInteger actionListHash,
            StorageUsed totalMessageSize
    ) {
        this.success = success;
        this.valid = valid;
        this.noFunds = noFunds;
        this.statusChange = Objects.requireNonNull(statusChange, "statusChange");
        this.totalFwdFees = totalFwdFees;
        this.totalActionFees = totalActionFees;
        this.resultCode = resultCode;
        this.resultArg = resultArg;
        this.totalActions = totalActions;
        this.specActions = specActions;
        this.skippedActions = skippedActions;
        this.messagesCreated = messagesCreated;
        this.actionListHash = Objects.requireNonNull(actionListHash, "actionListHash");
        this.totalMessageSize = Objects.requireNonNull(totalMessageSize, "totalMessageSize");
    }

    public static TransactionActionPhase load(Slice slice) {
        boolean success = slice.loadBit();
        boolean valid = slice.loadBit();
        boolean noFunds = slice.loadBit();

        AccountStatusChange statusChange = AccountStatusChange.load(slice);

        BigInteger totalFwdFees = slice.loadBit() ? slice.loadCoins() : null;
        BigInteger totalActionFees = slice.loadBit() ? slice.loadCoins() : null;

        int resultCode = (int) slice.loadInt(32);

        Integer resultArg = null;
        if (slice.loadBit()) {
            resultArg = Math.toIntExact(slice.loadInt(32));
        }

        int totalActions = (int) slice.loadUint(16);
        int specActions = (int) slice.loadUint(16);
        int skippedActions = (int) slice.loadUint(16);
        int messagesCreated = (int) slice.loadUint(16);

        BigInteger actionListHash = slice.loadUintBig(256);
        StorageUsed totalMessageSize = StorageUsed.loadStorageUsed(slice);

        return new TransactionActionPhase(
                success, valid, noFunds,
                statusChange,
                totalFwdFees, totalActionFees,
                resultCode, resultArg,
                totalActions, specActions, skippedActions, messagesCreated,
                actionListHash, totalMessageSize
        );
    }

    public void store(Builder builder) {
        builder.storeBit(success);
        builder.storeBit(valid);
        builder.storeBit(noFunds);

        builder.store(AccountStatusChange.storeAccountStatusChange(statusChange));

        builder.storeMaybeCoins(totalFwdFees);
        builder.storeMaybeCoins(totalActionFees);

        builder.storeInt(resultCode, 32);

        // ВАЖНО: если null — должен записаться только presence-bit=0
        builder.storeMaybeInt(resultArg == null ? null : (long) resultArg, 32);

        builder.storeUint(totalActions, 16);
        builder.storeUint(specActions, 16);
        builder.storeUint(skippedActions, 16);
        builder.storeUint(messagesCreated, 16);

        builder.storeUint(actionListHash, 256);
        builder.store(StorageUsed.storeStorageUsed(totalMessageSize));
    }


    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeTransactionActionPhase(TransactionActionPhase src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
