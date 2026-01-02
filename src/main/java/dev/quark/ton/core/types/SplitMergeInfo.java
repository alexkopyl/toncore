package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;

// TL-B:
// split_merge_info$_ cur_shard_pfx_len:(## 6)
//   acc_split_depth:(## 6) this_addr:bits256 sibling_addr:bits256
//   = SplitMergeInfo;
public final class SplitMergeInfo implements Writable {

    public final int currentShardPrefixLength; // uint6
    public final int accountSplitDepth;        // uint6
    public final BigInteger thisAddress;       // uint256
    public final BigInteger siblingAddress;    // uint256

    public SplitMergeInfo(int currentShardPrefixLength,
                          int accountSplitDepth,
                          BigInteger thisAddress,
                          BigInteger siblingAddress) {
        this.currentShardPrefixLength = currentShardPrefixLength;
        this.accountSplitDepth = accountSplitDepth;
        this.thisAddress = Objects.requireNonNull(thisAddress, "thisAddress");
        this.siblingAddress = Objects.requireNonNull(siblingAddress, "siblingAddress");
    }

    public static SplitMergeInfo load(Slice slice) {
        int currentShardPrefixLength = (int) slice.loadUint(6);
        int accountSplitDepth = (int) slice.loadUint(6);
        BigInteger thisAddress = slice.loadUintBig(256);
        BigInteger siblingAddress = slice.loadUintBig(256);
        return new SplitMergeInfo(currentShardPrefixLength, accountSplitDepth, thisAddress, siblingAddress);
    }

    public void store(Builder builder) {
        builder.storeUint(currentShardPrefixLength, 6);
        builder.storeUint(accountSplitDepth, 6);
        builder.storeUint(thisAddress, 256);
        builder.storeUint(siblingAddress, 256);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }
}
