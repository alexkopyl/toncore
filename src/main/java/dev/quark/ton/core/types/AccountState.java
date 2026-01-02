package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public sealed interface AccountState
        permits AccountState.Uninit, AccountState.Active, AccountState.Frozen {

    static AccountState loadAccountState(Slice slice) {
        if (slice.loadBit()) {
            return new Active(StateInit.loadStateInit(slice));
        } else if (slice.loadBit()) {
            return new Frozen(slice.loadUintBig(256));
        } else {
            return new Uninit();
        }
    }

    static Consumer<Builder> storeAccountState(AccountState src) {
        return builder -> {
            if (src instanceof Active a) {
                builder.storeBit(true);
                builder.store(StateInit.storeStateInit(a.state));
            } else if (src instanceof Frozen f) {
                builder.storeBit(false);
                builder.storeBit(true);
                builder.storeUint(f.stateHash, 256);
            } else if (src instanceof Uninit) {
                builder.storeBit(false);
                builder.storeBit(false);
            } else {
                throw new IllegalArgumentException("Unknown AccountState");
            }
        };
    }

    final class Uninit implements AccountState {}

    final class Active implements AccountState {
        public final StateInit state;
        public Active(StateInit state) {
            this.state = Objects.requireNonNull(state);
        }
    }

    final class Frozen implements AccountState {
        public final BigInteger stateHash;
        public Frozen(BigInteger stateHash) {
            this.stateHash = Objects.requireNonNull(stateHash);
        }
    }
}
