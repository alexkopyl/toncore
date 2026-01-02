package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;

public final class AccountStorage implements Writable {

    public final BigInteger lastTransLt;
    public final CurrencyCollection balance;
    public final AccountState state;

    public AccountStorage(BigInteger lastTransLt,
                          CurrencyCollection balance,
                          AccountState state) {
        this.lastTransLt = Objects.requireNonNull(lastTransLt);
        this.balance = Objects.requireNonNull(balance);
        this.state = Objects.requireNonNull(state);
    }

    public static AccountStorage loadAccountStorage(Slice slice) {
        return new AccountStorage(
                slice.loadUintBig(64),
                CurrencyCollection.loadCurrencyCollection(slice),
                AccountState.loadAccountState(slice)
        );
    }

    public void store(Builder builder) {
        builder.storeUint(lastTransLt, 64);
        builder.store(CurrencyCollection.storeCurrencyCollection(balance));
        builder.store(AccountState.storeAccountState(state));
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }
}
