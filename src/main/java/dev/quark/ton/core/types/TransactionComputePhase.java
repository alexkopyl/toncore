package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

import static dev.quark.ton.core.boc.Builder.beginCell;

public sealed interface TransactionComputePhase permits
        TransactionComputePhase.Skipped,
        TransactionComputePhase.Vm {

    static TransactionComputePhase load(Slice slice) {

        // Skipped: leading 0
        if (!slice.loadBit()) {
            ComputeSkipReason reason = ComputeSkipReason.load(slice);
            return new Skipped(reason);
        }

        boolean success = slice.loadBit();
        boolean messageStateUsed = slice.loadBit();
        boolean accountActivated = slice.loadBit();
        BigInteger gasFees = slice.loadCoins();

        Slice vmState = slice.loadRef().beginParse();
        BigInteger gasUsed = vmState.loadVarUintBig(3);
        BigInteger gasLimit = vmState.loadVarUintBig(3);
        BigInteger gasCredit = vmState.loadBit() ? vmState.loadVarUintBig(2) : null;
        int mode = (int) vmState.loadUint(8);
        int exitCode = (int) vmState.loadInt(32);

        Integer exitArg = null;
        if (vmState.loadBit()) {
            exitArg = Math.toIntExact(vmState.loadInt(32));
        }

        long vmSteps = vmState.loadUint(32);
        BigInteger vmInitStateHash = vmState.loadUintBig(256);
        BigInteger vmFinalStateHash = vmState.loadUintBig(256);

        return new Vm(
                success,
                messageStateUsed,
                accountActivated,
                gasFees,
                gasUsed,
                gasLimit,
                gasCredit,
                mode,
                exitCode,
                exitArg,
                vmSteps,
                vmInitStateHash,
                vmFinalStateHash
        );
    }

    static Consumer<Builder> storeTransactionComputePhase(TransactionComputePhase src) {
        Objects.requireNonNull(src, "src");
        return (builder) -> {
            if (src instanceof Skipped skipped) {
                builder.storeBit(0);
                builder.store(ComputeSkipReason.storeComputeSkipReason(skipped.reason));
                return;
            }

            Vm vm = (Vm) src;
            builder.storeBit(1);
            builder.storeBit(vm.success);
            builder.storeBit(vm.messageStateUsed);
            builder.storeBit(vm.accountActivated);
            builder.storeCoins(vm.gasFees);

            Cell vmCell = beginCell()
                    .storeVarUint(vm.gasUsed, 3)
                    .storeVarUint(vm.gasLimit, 3)
                    .store((Consumer<Builder>) b -> {
                        if (vm.gasCredit != null) {
                            b.storeBit(1);
                            b.storeVarUint(vm.gasCredit, 2);
                        } else {
                            b.storeBit(0);
                        }
                    })
                    .storeUint(vm.mode, 8)
                    .storeInt(vm.exitCode, 32)
                    .store((Consumer<Builder>) b -> {
                        if (vm.exitArg != null) {
                            b.storeBit(1);
                            b.storeInt(vm.exitArg, 32);
                        } else {
                            b.storeBit(0);
                        }
                    })
                    .storeUint(vm.vmSteps, 32)
                    .storeUint(vm.vmInitStateHash, 256)
                    .storeUint(vm.vmFinalStateHash, 256)
                    .endCell();

            builder.storeRef(vmCell);
        };
    }

    final class Skipped implements TransactionComputePhase {
        public final ComputeSkipReason reason;

        public Skipped(ComputeSkipReason reason) {
            this.reason = Objects.requireNonNull(reason, "reason");
        }
    }

    final class Vm implements TransactionComputePhase {
        public final boolean success;
        public final boolean messageStateUsed;
        public final boolean accountActivated;
        public final BigInteger gasFees;

        public final BigInteger gasUsed;
        public final BigInteger gasLimit;
        public final BigInteger gasCredit; // nullable

        public final int mode;
        public final int exitCode;
        public final Integer exitArg; // nullable

        public final long vmSteps; // uint32
        public final BigInteger vmInitStateHash;  // uint256
        public final BigInteger vmFinalStateHash; // uint256

        public Vm(boolean success,
                  boolean messageStateUsed,
                  boolean accountActivated,
                  BigInteger gasFees,
                  BigInteger gasUsed,
                  BigInteger gasLimit,
                  BigInteger gasCredit,
                  int mode,
                  int exitCode,
                  Integer exitArg,
                  long vmSteps,
                  BigInteger vmInitStateHash,
                  BigInteger vmFinalStateHash) {
            this.success = success;
            this.messageStateUsed = messageStateUsed;
            this.accountActivated = accountActivated;
            this.gasFees = Objects.requireNonNull(gasFees, "gasFees");
            this.gasUsed = Objects.requireNonNull(gasUsed, "gasUsed");
            this.gasLimit = Objects.requireNonNull(gasLimit, "gasLimit");
            this.gasCredit = gasCredit;
            this.mode = mode;
            this.exitCode = exitCode;
            this.exitArg = exitArg;
            this.vmSteps = vmSteps;
            this.vmInitStateHash = Objects.requireNonNull(vmInitStateHash, "vmInitStateHash");
            this.vmFinalStateHash = Objects.requireNonNull(vmFinalStateHash, "vmFinalStateHash");
        }
    }
}
