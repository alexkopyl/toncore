package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static dev.quark.ton.core.boc.Builder.beginCell;

/**
 * 1:1 port of OutList.ts (also contains OutAction types).
 *
 * TL-B:
 * action_send_msg#0ec3c86d mode:(## 8) out_msg:^(MessageRelaxed Any) = OutAction;
 * action_set_code#ad4de08e new_code:^Cell = OutAction;
 *
 * out_list_empty$_ = OutList 0;
 * out_list$_ {n:#} prev:^(OutList n) action:OutAction = OutList (n + 1);
 */
public final class OutList {

    private OutList() { }

    // Tags from TS
    private static final long OUT_ACTION_SEND_MSG_TAG = 0x0ec3c86dL;
    private static final long OUT_ACTION_SET_CODE_TAG = 0xad4de08eL;

    // ===== OutAction union =====

    public sealed interface OutAction permits OutActionSendMsg, OutActionSetCode {
        String type();
    }

    public static final class OutActionSendMsg implements OutAction {
        public final int mode; // SendMode (uint8 in cell)
        public final MessageRelaxed outMsg;

        public OutActionSendMsg(int mode, MessageRelaxed outMsg) {
            this.mode = mode;
            this.outMsg = Objects.requireNonNull(outMsg, "outMsg");
        }

        @Override
        public String type() { return "sendMsg"; }
    }

    public static final class OutActionSetCode implements OutAction {
        public final Cell newCode;

        public OutActionSetCode(Cell newCode) {
            this.newCode = Objects.requireNonNull(newCode, "newCode");
        }

        @Override
        public String type() { return "setCode"; }
    }

    // ===== OutAction load/store =====

    public static OutAction loadOutAction(Slice slice) {
        long tag = slice.loadUint(32);

        if (tag == OUT_ACTION_SEND_MSG_TAG) {
            int mode = (int) slice.loadUint(8);
            MessageRelaxed outMsg = MessageRelaxed.loadMessageRelaxed(slice.loadRef().beginParse());
            return new OutActionSendMsg(mode, outMsg);
        }

        if (tag == OUT_ACTION_SET_CODE_TAG) {
            Cell newCode = slice.loadRef();
            return new OutActionSetCode(newCode);
        }

        throw new IllegalArgumentException("Unknown out action tag 0x" + Long.toHexString(tag));
    }

    public static Consumer<Builder> storeOutAction(OutAction action) {
        Objects.requireNonNull(action, "action");

        if (action instanceof OutActionSendMsg sendMsg) {
            return storeOutActionSendMsg(sendMsg);
        } else if (action instanceof OutActionSetCode setCode) {
            return storeOutActionSetCode(setCode);
        } else {
            throw new IllegalArgumentException("Unknown action type " + action.type());
        }
    }

    /*
     * action_send_msg#0ec3c86d mode:(## 8) out_msg:^(MessageRelaxed Any) = OutAction;
     */
    private static Consumer<Builder> storeOutActionSendMsg(OutActionSendMsg action) {
        return (builder) -> builder
                .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                .storeUint(action.mode, 8)
                .storeRef(beginCell().store(MessageRelaxed.storeMessageRelaxed(action.outMsg, null)).endCell());
    }

    /*
     * action_set_code#ad4de08e new_code:^Cell = OutAction;
     */
    private static Consumer<Builder> storeOutActionSetCode(OutActionSetCode action) {
        return (builder) -> builder
                .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                .storeRef(action.newCode);
    }

    // ===== OutList load/store =====

    /**
     * TS storeOutList:
     * actions.reduce((cell, action) => beginCell().storeRef(cell).store(storeOutAction(action)).endCell(),
     *                beginCell().endCell())
     * builder.storeSlice(cell.beginParse())
     */
    public static Consumer<Builder> storeOutList(List<OutAction> actions) {
        Objects.requireNonNull(actions, "actions");

        Cell cell = beginCell().endCell(); // out_list_empty

        for (OutAction action : actions) {
            cell = beginCell()
                    .storeRef(cell)
                    .store(storeOutAction(action))
                    .endCell();
        }

        final Cell finalCell = cell;
        return (builder) -> builder.storeSlice(finalCell.beginParse());
    }

    /**
     * TS loadOutList:
     * while (slice.remainingRefs) { nextCell = slice.loadRef(); actions.push(loadOutAction(slice)); slice = nextCell.beginParse(); }
     * return actions.reverse();
     */
    public static List<OutAction> loadOutList(Slice slice) {
        Objects.requireNonNull(slice, "slice");

        List<OutAction> actions = new ArrayList<>();
        while (slice.remainingRefs() > 0) {
            Cell nextCell = slice.loadRef();
            actions.add(loadOutAction(slice));
            slice = nextCell.beginParse();
        }
        Collections.reverse(actions);
        return actions;
    }
}
