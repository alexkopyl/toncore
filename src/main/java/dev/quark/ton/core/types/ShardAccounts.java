package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * _ (HashmapAugE 256 ShardAccount DepthBalanceInfo) = ShardAccounts;
 *
 * 1:1 port of ShardAccounts.ts
 */
public final class ShardAccounts {

    private ShardAccounts() {}

    public static final class ShardAccountRef {
        public final ShardAccount shardAccount;
        public final DepthBalanceInfo depthBalanceInfo;

        public ShardAccountRef(ShardAccount shardAccount, DepthBalanceInfo depthBalanceInfo) {
            this.shardAccount = Objects.requireNonNull(shardAccount, "shardAccount");
            this.depthBalanceInfo = Objects.requireNonNull(depthBalanceInfo, "depthBalanceInfo");
        }
    }

    public static final Dictionary.DictionaryValue<ShardAccountRef> ShardAccountRefValue =
            new Dictionary.DictionaryValue<>() {
                @Override
                public ShardAccountRef parse(Slice cs) {
                    DepthBalanceInfo depthBalanceInfo = DepthBalanceInfo.loadDepthBalanceInfo(cs);
                    ShardAccount shardAccount = ShardAccount.loadShardAccount(cs);
                    return new ShardAccountRef(shardAccount, depthBalanceInfo);
                }

                @Override
                public void serialize(ShardAccountRef src, Builder builder) {
                    builder.store(DepthBalanceInfo.storeDepthBalanceInfo(src.depthBalanceInfo));
                    builder.store(ShardAccount.storeShardAccount(src.shardAccount));
                }
            };

    public static Dictionary<BigInteger, ShardAccountRef> loadShardAccounts(Slice cs) {
        return Dictionary.load(Dictionary.Keys.BigUint(256), ShardAccountRefValue, cs);
    }

    public static Consumer<Builder> storeShardAccounts(Dictionary<BigInteger, ShardAccountRef> src) {
        Objects.requireNonNull(src, "src");
        return (builder) -> builder.storeDict(src, null, null);
    }
}
