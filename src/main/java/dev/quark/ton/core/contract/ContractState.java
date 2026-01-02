package dev.quark.ton.core.contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

/**
 * Port of ton-core/src/contract/ContractState.ts
 */
public final class ContractState {

    private final BigInteger balance;
    private final LastTransaction last; // nullable
    private final State state;          // required
    private final Map<Long, BigInteger> extracurrency; // nullable
    public Map<Long, BigInteger> extracurrency() { return extracurrency; }

    public ContractState(BigInteger balance, LastTransaction last, State state, Map<Long, BigInteger> extracurrency) {
        this.balance = balance;
        this.last = last;
        this.state = state;
        this.extracurrency = extracurrency;
    }

    public BigInteger balance() { return balance; }
    public LastTransaction last() { return last; }
    public State state() { return state; }

    // ---------------------------------------------------------------------
    // last: { lt: bigint, hash: Buffer } | null
    // ---------------------------------------------------------------------

    public static final class LastTransaction {
        private final BigInteger lt;
        private final byte[] hash;

        public LastTransaction(BigInteger lt, byte[] hash) {
            this.lt = lt;
            this.hash = hash == null ? null : Arrays.copyOf(hash, hash.length);
        }

        public BigInteger lt() { return lt; }
        public byte[] hash() { return hash == null ? null : Arrays.copyOf(hash, hash.length); }
    }

    // ---------------------------------------------------------------------
    // state union:
    // { type: 'uninit' }
    // | { type: 'active', code: Maybe<Buffer>, data: Maybe<Buffer> }
    // | { type: 'frozen', stateHash: Buffer }
    // ---------------------------------------------------------------------

    public sealed interface State permits Uninit, Active, Frozen {
        String type();
    }

    public static final class Uninit implements State {
        @Override
        public String type() { return "uninit"; }
    }

    public static final class Active implements State {
        private final byte[] code; // nullable
        private final byte[] data; // nullable

        public Active(byte[] code, byte[] data) {
            this.code = code == null ? null : Arrays.copyOf(code, code.length);
            this.data = data == null ? null : Arrays.copyOf(data, data.length);
        }

        @Override
        public String type() { return "active"; }

        public byte[] code() { return code == null ? null : Arrays.copyOf(code, code.length); }
        public byte[] data() { return data == null ? null : Arrays.copyOf(data, data.length); }
    }

    public static final class Frozen implements State {
        private final byte[] stateHash;

        public Frozen(byte[] stateHash) {
            this.stateHash = stateHash == null ? null : Arrays.copyOf(stateHash, stateHash.length);
        }

        @Override
        public String type() { return "frozen"; }

        public byte[] stateHash() { return stateHash == null ? null : Arrays.copyOf(stateHash, stateHash.length); }
    }
}
