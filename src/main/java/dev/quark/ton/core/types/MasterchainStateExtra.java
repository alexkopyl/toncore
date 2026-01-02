package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.Objects;

public final class MasterchainStateExtra {

    public final BigInteger configAddress;
    public final Dictionary<Long, Cell> config;   // <-- ВАЖНО: Long
    public final CurrencyCollection globalBalance;

    public MasterchainStateExtra(
            BigInteger configAddress,
            Dictionary<Long, Cell> config,
            CurrencyCollection globalBalance
    ) {
        this.configAddress = Objects.requireNonNull(configAddress, "configAddress");
        this.config = Objects.requireNonNull(config, "config");
        this.globalBalance = Objects.requireNonNull(globalBalance, "globalBalance");
    }

    public static MasterchainStateExtra loadMasterchainStateExtra(Slice cs) {

        // magic
        long magic = cs.loadUint(16);
        if (magic != 0xcc26L) {
            throw new IllegalArgumentException("Invalid McStateExtra magic: " + magic);
        }

        // Skip shard_hashes
        if (cs.loadBit()) {
            cs.loadRef();
        }

        // Config
        BigInteger configAddress = cs.loadUintBig(256);

        Dictionary<Long, Cell> config = Dictionary.load(
                Dictionary.Keys.Int(32),   // -> Long
                Dictionary.Values.Cell(),
                cs
        );

        // Global balance
        CurrencyCollection globalBalance =
                CurrencyCollection.loadCurrencyCollection(cs);

        return new MasterchainStateExtra(
                configAddress,
                config,
                globalBalance
        );
    }
}
