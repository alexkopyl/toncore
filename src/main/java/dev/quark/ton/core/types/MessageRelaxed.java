package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 1:1 port of MessageRelaxed.ts
 */
public final class MessageRelaxed {

    public final CommonMessageInfoRelaxedTLB.CommonMessageInfoRelaxed info;
    public final StateInit init; // nullable
    public final Cell body;

    public MessageRelaxed(CommonMessageInfoRelaxedTLB.CommonMessageInfoRelaxed info, StateInit init, Cell body) {
        this.info = Objects.requireNonNull(info, "info");
        this.init = init;
        this.body = Objects.requireNonNull(body, "body");
    }

    public static MessageRelaxed loadMessageRelaxed(Slice slice) {
        var info = CommonMessageInfoRelaxedTLB.loadCommonMessageInfoRelaxed(slice);

        StateInit init = null;
        if (slice.loadBit()) {
            if (!slice.loadBit()) {
                init = StateInit.loadStateInit(slice);
            } else {
                init = StateInit.loadStateInit(slice.loadRef().beginParse());
            }
        }

        Cell body = slice.loadBit() ? slice.loadRef() : slice.asCell();
        return new MessageRelaxed(info, init, body);
    }

    public static Consumer<Builder> storeMessageRelaxed(MessageRelaxed message, Message.StoreOptions opts) {
        final Message.StoreOptions options = (opts == null) ? Message.StoreOptions.none() : opts;

        return (builder) -> {

            // Store CommonMsgInfoRelaxed
            builder.store(CommonMessageInfoRelaxedTLB.storeCommonMessageInfoRelaxed(message.info));

            // Store init
            if (message.init != null) {
                builder.storeBit(true);

                Builder initBuilder = Builder.beginCell().store(StateInit.storeStateInit(message.init));

                boolean needRef;
                if (options.forceRef) {
                    needRef = true;
                } else if (builder.availableBits() - 2 >= initBuilder.bits()) {
                    needRef = false;
                } else {
                    needRef = true;
                }

                if (needRef) {
                    builder.storeBit(true);
                    builder.storeRef(initBuilder.endCell());
                } else {
                    builder.storeBit(false);
                    builder.storeBuilder(initBuilder);
                }
            } else {
                builder.storeBit(false);
            }

            // Store body
            boolean needRef;
            if (options.forceRef) {
                needRef = true;
            } else {
                int bodyBits = message.body.bits.length();
                int bodyRefs = message.body.refs.size();
                if (builder.availableBits() - 1 >= bodyBits && (builder.refs() + bodyRefs) <= 4) {
                    needRef = false;
                } else {
                    needRef = true;
                }
            }

            if (needRef) {
                builder.storeBit(true);
                builder.storeRef(message.body);
            } else {
                builder.storeBit(false);
                builder.storeBuilder(message.body.asBuilder());
            }
        };
    }
}
