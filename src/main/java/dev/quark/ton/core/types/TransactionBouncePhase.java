package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public sealed interface TransactionBouncePhase permits
        TransactionBouncePhase.NegativeFunds,
        TransactionBouncePhase.NoFunds,
        TransactionBouncePhase.Ok {

    static TransactionBouncePhase load(Slice slice) {

        // Ok: leading 1
        if (slice.loadBit()) {
            StorageUsed messageSize = StorageUsed.loadStorageUsed(slice);
            BigInteger messageFees = slice.loadCoins();
            BigInteger forwardFees = slice.loadCoins();
            return new Ok(messageSize, messageFees, forwardFees);
        }

        // No funds: 01
        if (slice.loadBit()) {
            StorageUsed messageSize = StorageUsed.loadStorageUsed(slice);
            BigInteger requiredForwardFees = slice.loadCoins();
            return new NoFunds(messageSize, requiredForwardFees);
        }

        // Negative funds: 00
        return new NegativeFunds();
    }

    static Consumer<Builder> storeTransactionBouncePhase(TransactionBouncePhase src) {
        Objects.requireNonNull(src, "src");
        return b -> {
            if (src instanceof Ok ok) {
                b.storeBit(true);
                b.store(StorageUsed.storeStorageUsed(ok.messageSize));
                b.storeCoins(ok.messageFees);
                b.storeCoins(ok.forwardFees);

            } else if (src instanceof NoFunds nf) {
                b.storeBit(false);
                b.storeBit(true);
                b.store(StorageUsed.storeStorageUsed(nf.messageSize));
                b.storeCoins(nf.requiredForwardFees);

            } else if (src instanceof NegativeFunds) {
                b.storeBit(false);
                b.storeBit(false);

            } else {
                throw new IllegalArgumentException("Invalid TransactionBouncePhase: " + src);
            }
        };
    }

    // --- Variants ---

    final class NegativeFunds implements TransactionBouncePhase { }

    final class NoFunds implements TransactionBouncePhase {
        public final StorageUsed messageSize;
        public final BigInteger requiredForwardFees;

        public NoFunds(StorageUsed messageSize, BigInteger requiredForwardFees) {
            this.messageSize = Objects.requireNonNull(messageSize, "messageSize");
            this.requiredForwardFees = Objects.requireNonNull(requiredForwardFees, "requiredForwardFees");
        }
    }

    final class Ok implements TransactionBouncePhase {
        public final StorageUsed messageSize;
        public final BigInteger messageFees;
        public final BigInteger forwardFees;

        public Ok(StorageUsed messageSize, BigInteger messageFees, BigInteger forwardFees) {
            this.messageSize = Objects.requireNonNull(messageSize, "messageSize");
            this.messageFees = Objects.requireNonNull(messageFees, "messageFees");
            this.forwardFees = Objects.requireNonNull(forwardFees, "forwardFees");
        }
    }
}
