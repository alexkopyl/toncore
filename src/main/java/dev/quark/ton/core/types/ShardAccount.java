package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

import static dev.quark.ton.core.boc.Builder.beginCell;

/**
 * TL-B:
 * account_descr$_ account:^Account last_trans_hash:bits256 last_trans_lt:uint64 = ShardAccount;
 *
 * 1:1 port of ShardAccount.ts
 */
public final class ShardAccount implements Writable {

    public final Account account; // nullable (Maybe<Account>)
    public final BigInteger lastTransactionHash; // uint256
    public final BigInteger lastTransactionLt;   // uint64

    public ShardAccount(Account account, BigInteger lastTransactionHash, BigInteger lastTransactionLt) {
        this.account = account;
        this.lastTransactionHash = Objects.requireNonNull(lastTransactionHash, "lastTransactionHash");
        this.lastTransactionLt = Objects.requireNonNull(lastTransactionLt, "lastTransactionLt");
    }

    public static ShardAccount loadShardAccount(Slice slice) {
        Cell accountRef = slice.loadRef();

        Account account = null;
        if (!accountRef.isExotic()) {
            Slice accountSlice = accountRef.beginParse();
            if (accountSlice.loadBit()) {
                account = Account.loadAccount(accountSlice);
            }
        }

        BigInteger lastTransactionHash = slice.loadUintBig(256);
        BigInteger lastTransactionLt = slice.loadUintBig(64);

        return new ShardAccount(account, lastTransactionHash, lastTransactionLt);
    }

    public static Consumer<Builder> storeShardAccount(ShardAccount src) {
        Objects.requireNonNull(src, "src");
        return (builder) -> {
            if (src.account != null) {
                builder.storeRef(
                        beginCell()
                                .storeBit(true)
                                .store(src.account) // если у тебя нет такого overload — см. примечание ниже
                                .endCell()
                );
            } else {
                builder.storeRef(
                        beginCell()
                                .storeBit(false)
                                .endCell()
                );
            }

            builder.storeUint(src.lastTransactionHash, 256);
            builder.storeUint(src.lastTransactionLt, 64);
        };
    }

    /**
     * Удобный instance-store (по твоему стилю Writable).
     */
    public void store(Builder builder) {
        storeShardAccount(this).accept(builder);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }
}
