package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 1:1 port of Message.ts
 */
public final class Message {

    public final CommonMessageInfoTLB.CommonMessageInfo info;
    public final StateInit init; // nullable
    public final Cell body;

    public Message(CommonMessageInfoTLB.CommonMessageInfo info, StateInit init, Cell body) {
        this.info = Objects.requireNonNull(info, "info");
        this.init = init;
        this.body = Objects.requireNonNull(body, "body");
    }

    public static Message loadMessage(Slice slice) {
        var info = CommonMessageInfoTLB.loadCommonMessageInfo(slice);

        StateInit init = null;
        if (slice.loadBit()) {
            if (!slice.loadBit()) {
                init = StateInit.loadStateInit(slice);
            } else {
                init = StateInit.loadStateInit(slice.loadRef().beginParse());
            }
        }

        Cell body = slice.loadBit() ? slice.loadRef() : slice.asCell();
        return new Message(info, init, body);
    }

    public static final class StoreOptions {
        public final boolean forceRef;
        public StoreOptions(boolean forceRef) { this.forceRef = forceRef; }
        public static StoreOptions none() { return new StoreOptions(false); }
        public static StoreOptions forceRef() { return new StoreOptions(true); }
    }

    public static Consumer<Builder> storeMessage(Message message, StoreOptions opts) {
        final StoreOptions options = (opts == null) ? StoreOptions.none() : opts;

        return (builder) -> {

            // Store CommonMsgInfo
            builder.store(CommonMessageInfoTLB.storeCommonMessageInfo(message.info));

            // Store init
            // Store init
            if (message.init != null) {
                builder.storeBit(true);

                Builder initBuilder = Builder.beginCell().store(StateInit.storeStateInit(message.init));

                boolean needRef;
                if (options.forceRef) {
                    needRef = true;
                } else {
                    boolean fitsBits = (builder.availableBits() - 1) >= initBuilder.bits();     // <- ключевая правка
                    boolean fitsRefs = (builder.refs() + initBuilder.refs()) <= 4;              // <- добавили refs check
                    needRef = !(fitsBits && fitsRefs);
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
                int bodyBits = message.body.bits.length();       // если у тебя bits поле — поправишь на bits.length
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

    // TS: export const MessageValue: DictionaryValue<Message>
    public static final Dictionary.DictionaryValue<Message> MessageValue =
            new Dictionary.DictionaryValue<>() {
                @Override
                public void serialize(Message src, Builder builder) {
                    builder.storeRef(
                            Builder.beginCell()
                                    .store(Message.storeMessage(src, StoreOptions.none()))
                                    .endCell()
                    );
                }

                @Override
                public Message parse(Slice slice) {
                    return Message.loadMessage(slice.loadRef().beginParse());
                }
            };
}
