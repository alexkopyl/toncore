package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.util.Objects;
import java.util.function.Consumer;

import static dev.quark.ton.core.boc.Builder.beginCell;

/**
 * 1:1 port of TransactionDescription.ts
 * Supports only types 0x00..0x05, exactly like the TS file provided.
 */
public sealed interface TransactionDescription permits
        TransactionDescription.Generic,
        TransactionDescription.Storage,
        TransactionDescription.TickTock,
        TransactionDescription.SplitPrepare,
        TransactionDescription.SplitInstall {

    static TransactionDescription load(Slice slice) {
        int type = (int) slice.loadUint(4);

        // 0x00: generic
        if (type == 0x00) {
            boolean creditFirst = slice.loadBit();

            TransactionStoragePhase storagePhase = slice.loadBit() ? TransactionStoragePhase.load(slice) : null;
            TransactionCreditPhase creditPhase = slice.loadBit() ? TransactionCreditPhase.load(slice) : null;

            TransactionComputePhase computePhase = TransactionComputePhase.load(slice);

            TransactionActionPhase actionPhase = null;
            if (slice.loadBit()) {
                actionPhase = TransactionActionPhase.load(slice.loadRef().beginParse());
            }

            boolean aborted = slice.loadBit();

            TransactionBouncePhase bouncePhase = slice.loadBit()
                    ? TransactionBouncePhase.load(slice)
                    : null;

            boolean destroyed = slice.loadBit();

            return new Generic(
                    creditFirst,
                    storagePhase,
                    creditPhase,
                    computePhase,
                    actionPhase,
                    bouncePhase,
                    aborted,
                    destroyed
            );
        }

        // 0x01: storage
        if (type == 0x01) {
            return new Storage(TransactionStoragePhase.load(slice));
        }

        // 0x02/0x03: tick-tock
        if (type == 0x02 || type == 0x03) {
            boolean isTock = (type == 0x03);

            TransactionStoragePhase storagePhase = TransactionStoragePhase.load(slice);
            TransactionComputePhase computePhase = TransactionComputePhase.load(slice);

            TransactionActionPhase actionPhase = null;
            if (slice.loadBit()) {
                actionPhase = TransactionActionPhase.load(slice.loadRef().beginParse());
            }

            boolean aborted = slice.loadBit();
            boolean destroyed = slice.loadBit();

            return new TickTock(
                    isTock,
                    storagePhase,
                    computePhase,
                    actionPhase,
                    aborted,
                    destroyed
            );
        }

        // 0x04: split-prepare
        if (type == 0x04) {
            SplitMergeInfo splitInfo = SplitMergeInfo.load(slice);

            TransactionStoragePhase storagePhase = slice.loadBit() ? TransactionStoragePhase.load(slice) : null;
            TransactionComputePhase computePhase = TransactionComputePhase.load(slice);

            TransactionActionPhase actionPhase = null;
            if (slice.loadBit()) {
                actionPhase = TransactionActionPhase.load(slice.loadRef().beginParse());
            }

            boolean aborted = slice.loadBit();
            boolean destroyed = slice.loadBit();

            return new SplitPrepare(
                    splitInfo,
                    storagePhase,
                    computePhase,
                    actionPhase,
                    aborted,
                    destroyed
            );
        }

        // 0x05: split-install
        if (type == 0x05) {
            SplitMergeInfo splitInfo = SplitMergeInfo.load(slice);
            Transaction prepareTransaction = Transaction.load(slice.loadRef().beginParse());
            boolean installed = slice.loadBit();
            return new SplitInstall(splitInfo, prepareTransaction, installed);
        }

        throw new IllegalArgumentException("Unsupported transaction description type " + type);
    }

    static Consumer<Builder> storeTransactionDescription(TransactionDescription src) {
        Objects.requireNonNull(src, "src");
        return (builder) -> {

            // generic
            if (src instanceof Generic g) {
                builder.storeUint(0x00, 4);
                builder.storeBit(g.creditFirst);

                if (g.storagePhase != null) {
                    builder.storeBit(true);
                    builder.store(TransactionStoragePhase.storeTransactionsStoragePhase(g.storagePhase));
                } else {
                    builder.storeBit(false);
                }

                if (g.creditPhase != null) {
                    builder.storeBit(true);
                    builder.store(TransactionCreditPhase.storeTransactionCreditPhase(g.creditPhase));
                } else {
                    builder.storeBit(false);
                }

                builder.store(TransactionComputePhase.storeTransactionComputePhase(g.computePhase));

                if (g.actionPhase != null) {
                    builder.storeBit(true);
                    Cell ref = beginCell().store(TransactionActionPhase.storeTransactionActionPhase(g.actionPhase)).endCell();
                    builder.storeRef(ref);
                } else {
                    builder.storeBit(false);
                }

                builder.storeBit(g.aborted);

                if (g.bouncePhase != null) {
                    builder.storeBit(true);
                    builder.store(TransactionBouncePhase.storeTransactionBouncePhase(g.bouncePhase));
                } else {
                    builder.storeBit(false);
                }

                builder.storeBit(g.destroyed);
                return;
            }

            // storage
            if (src instanceof Storage s) {
                builder.storeUint(0x01, 4);
                builder.store(TransactionStoragePhase.storeTransactionsStoragePhase(s.storagePhase));
                return;
            }

            // tick-tock
            if (src instanceof TickTock tt) {
                builder.storeUint(tt.isTock ? 0x03 : 0x02, 4);
                builder.store(TransactionStoragePhase.storeTransactionsStoragePhase(tt.storagePhase));
                builder.store(TransactionComputePhase.storeTransactionComputePhase(tt.computePhase));

                if (tt.actionPhase != null) {
                    builder.storeBit(true);
                    Cell ref = beginCell().store(TransactionActionPhase.storeTransactionActionPhase(tt.actionPhase)).endCell();
                    builder.storeRef(ref);
                } else {
                    builder.storeBit(false);
                }

                builder.storeBit(tt.aborted);
                builder.storeBit(tt.destroyed);
                return;
            }

            // split-prepare
            if (src instanceof SplitPrepare sp) {
                builder.storeUint(0x04, 4);
                builder.store((Consumer<Builder>) sp.splitInfo::store);

                if (sp.storagePhase != null) {
                    builder.storeBit(true);
                    builder.store(TransactionStoragePhase.storeTransactionsStoragePhase(sp.storagePhase));
                } else {
                    builder.storeBit(false);
                }

                builder.store(TransactionComputePhase.storeTransactionComputePhase(sp.computePhase));

                if (sp.actionPhase != null) {
                    builder.storeBit(true);
                    Cell ref = beginCell()
                            .store(TransactionActionPhase.storeTransactionActionPhase(sp.actionPhase))
                            .endCell();
                    builder.storeRef(ref);
                } else {
                    builder.storeBit(false);
                }

                builder.storeBit(sp.aborted);
                builder.storeBit(sp.destroyed);
                return;
            }

            // split-install
            if (src instanceof SplitInstall si) {
                builder.storeUint(0x05, 4);
                builder.store((Consumer<Builder>) si.splitInfo::store);

                // ^Transaction
                Cell ref = beginCell()
                        .store((java.util.function.Consumer<Builder>) si.prepareTransaction::store)
                        .endCell();
                builder.storeRef(ref);

                builder.storeBit(si.installed);
                return;
            }

            throw new IllegalArgumentException("Unsupported transaction description type " + src);
        };
    }

    // ===== Variants =====

    final class Generic implements TransactionDescription {
        public final boolean creditFirst;
        public final TransactionStoragePhase storagePhase; // nullable
        public final TransactionCreditPhase creditPhase;   // nullable
        public final TransactionComputePhase computePhase;
        public final TransactionActionPhase actionPhase;   // nullable
        public final TransactionBouncePhase bouncePhase;   // nullable
        public final boolean aborted;
        public final boolean destroyed;

        public Generic(boolean creditFirst,
                       TransactionStoragePhase storagePhase,
                       TransactionCreditPhase creditPhase,
                       TransactionComputePhase computePhase,
                       TransactionActionPhase actionPhase,
                       TransactionBouncePhase bouncePhase,
                       boolean aborted,
                       boolean destroyed) {
            this.creditFirst = creditFirst;
            this.storagePhase = storagePhase;
            this.creditPhase = creditPhase;
            this.computePhase = Objects.requireNonNull(computePhase, "computePhase");
            this.actionPhase = actionPhase;
            this.bouncePhase = bouncePhase;
            this.aborted = aborted;
            this.destroyed = destroyed;
        }
    }

    final class Storage implements TransactionDescription {
        public final TransactionStoragePhase storagePhase;

        public Storage(TransactionStoragePhase storagePhase) {
            this.storagePhase = Objects.requireNonNull(storagePhase, "storagePhase");
        }
    }

    final class TickTock implements TransactionDescription {
        public final boolean isTock;
        public final TransactionStoragePhase storagePhase;
        public final TransactionComputePhase computePhase;
        public final TransactionActionPhase actionPhase; // nullable
        public final boolean aborted;
        public final boolean destroyed;

        public TickTock(boolean isTock,
                        TransactionStoragePhase storagePhase,
                        TransactionComputePhase computePhase,
                        TransactionActionPhase actionPhase,
                        boolean aborted,
                        boolean destroyed) {
            this.isTock = isTock;
            this.storagePhase = Objects.requireNonNull(storagePhase, "storagePhase");
            this.computePhase = Objects.requireNonNull(computePhase, "computePhase");
            this.actionPhase = actionPhase;
            this.aborted = aborted;
            this.destroyed = destroyed;
        }
    }

    final class SplitPrepare implements TransactionDescription {
        public final SplitMergeInfo splitInfo;
        public final TransactionStoragePhase storagePhase; // nullable
        public final TransactionComputePhase computePhase;
        public final TransactionActionPhase actionPhase;   // nullable
        public final boolean aborted;
        public final boolean destroyed;

        public SplitPrepare(SplitMergeInfo splitInfo,
                            TransactionStoragePhase storagePhase,
                            TransactionComputePhase computePhase,
                            TransactionActionPhase actionPhase,
                            boolean aborted,
                            boolean destroyed) {
            this.splitInfo = Objects.requireNonNull(splitInfo, "splitInfo");
            this.storagePhase = storagePhase;
            this.computePhase = Objects.requireNonNull(computePhase, "computePhase");
            this.actionPhase = actionPhase;
            this.aborted = aborted;
            this.destroyed = destroyed;
        }
    }

    final class SplitInstall implements TransactionDescription {
        public final SplitMergeInfo splitInfo;
        public final Transaction prepareTransaction;
        public final boolean installed;

        public SplitInstall(SplitMergeInfo splitInfo, Transaction prepareTransaction, boolean installed) {
            this.splitInfo = Objects.requireNonNull(splitInfo, "splitInfo");
            this.prepareTransaction = Objects.requireNonNull(prepareTransaction, "prepareTransaction");
            this.installed = installed;
        }
    }
}
