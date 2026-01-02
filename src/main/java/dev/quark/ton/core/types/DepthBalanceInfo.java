package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * depth_balance$_ split_depth:(#<= 30) balance:CurrencyCollection = DepthBalanceInfo;
 *
 * In TS they read splitDepth as loadUint(5).
 */
public final class DepthBalanceInfo implements Writable {

    public final int splitDepth;               // uint5
    public final CurrencyCollection balance;

    public DepthBalanceInfo(int splitDepth, CurrencyCollection balance) {
        this.splitDepth = splitDepth;
        this.balance = Objects.requireNonNull(balance, "balance");
    }

    public static DepthBalanceInfo loadDepthBalanceInfo(Slice slice) {
        int splitDepth = (int) slice.loadUint(5);
        CurrencyCollection balance = CurrencyCollection.loadCurrencyCollection(slice);
        return new DepthBalanceInfo(splitDepth, balance);
    }

    public void store(Builder builder) {
        builder.storeUint(splitDepth, 5);
        builder.store(CurrencyCollection.storeCurrencyCollection(balance));
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeDepthBalanceInfo(DepthBalanceInfo src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
