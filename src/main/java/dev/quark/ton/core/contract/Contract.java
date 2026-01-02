package dev.quark.ton.core.contract;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Cell;

/**
 * Port of ton-core/src/contract/Contract.ts
 */
public interface Contract {

    Address address();

    /**
     * Maybe<{ code: Cell, data: Cell }>
     * Java: nullable
     */
    StateInit init();

    /**
     * Maybe<ContractABI>
     * Java: nullable
     */
    ContractABI abi();

    /**
     * TS init object
     */
    final class StateInit {
        private final Cell code;
        private final Cell data;

        public StateInit(Cell code, Cell data) {
            this.code = code;
            this.data = data;
        }

        public Cell code() { return code; }
        public Cell data() { return data; }
    }
}
