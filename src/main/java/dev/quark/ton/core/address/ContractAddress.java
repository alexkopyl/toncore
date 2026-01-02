package dev.quark.ton.core.address;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.types.StateInit;

public final class ContractAddress {

    private ContractAddress() {
    }

    public static Address contractAddress(int workchain, StateInit init) {
        if (init == null) {
            throw new IllegalArgumentException("init is null");
        }

        byte[] hash = Builder.beginCell()
                .store(StateInit.storeStateInit(init))
                .endCell()
                .hash();

        return new Address(workchain, hash);
    }
}
