package dev.quark.ton.core.contract;

import dev.quark.ton.core.address.Address;

import java.util.concurrent.CompletableFuture;

/**
 * Port of ton-core/src/contract/Sender.ts (Sender interface)
 */
public interface Sender {

    /**
     * TS: readonly address?: Address;
     * Java: nullable (может быть null).
     */
    Address address();

    /**
     * TS: send(args): Promise<void>
     */
    CompletableFuture<Void> send(SenderArguments args);
}
