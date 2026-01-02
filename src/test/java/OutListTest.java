import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.CommonMessageInfoRelaxedTLB;
import dev.quark.ton.core.types.MessageRelaxed;
import dev.quark.ton.core.types.OutList;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutListTest {

    private static final long OUT_ACTION_SEND_MSG_TAG = 0x0ec3c86dL;
    private static final long OUT_ACTION_SET_CODE_TAG = 0xad4de08eL;

    // В спеке SendMode.PAY_GAS_SEPARATELY и SendMode.IGNORE_ERRORS.
    // В Java-реализации mode — просто uint8/int. Чтобы не зависеть от enum, фиксируем любые валидные значения.
    private static final int PAY_GAS_SEPARATELY = 1;
    private static final int IGNORE_ERRORS = 2;

    private static MessageRelaxed mockMessageRelaxed(int createdLt, int createdAt, int bodyByte) {
        // TS mock: external-out, src:null, dest:null, init:null, body: cell(storeUint(x,8))
        var info = new CommonMessageInfoRelaxedTLB.ExternalOut(
                null,                  // src
                null,                  // dest
                BigInteger.valueOf(createdLt),
                createdAt
        );

        Cell body = Builder.beginCell().storeUint(bodyByte, 8).endCell();

        return new MessageRelaxed(info, null, body);
    }

    @Test
    void shouldSerialiseSendMsgAction() {
        MessageRelaxed msg = mockMessageRelaxed(0, 0, 0);

        OutList.OutAction action = new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg);

        Cell actual = Builder.beginCell()
                .store(OutList.storeOutAction(action))
                .endCell();

        Cell expected = Builder.beginCell()
                .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                .storeUint(PAY_GAS_SEPARATELY, 8)
                .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg, null)).endCell())
                .endCell();

        assertTrue(expected.equals(actual));
    }

    @Test
    void shouldSerialiseSetCodeAction() {
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();
        OutList.OutAction action = new OutList.OutActionSetCode(mockSetCodeCell);

        Cell actual = Builder.beginCell()
                .store(OutList.storeOutAction(action))
                .endCell();

        Cell expected = Builder.beginCell()
                .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                .storeRef(mockSetCodeCell)
                .endCell();

        assertTrue(expected.equals(actual));
    }

    @Test
    void shouldDeserializeSendMsgAction() {
        MessageRelaxed msg = mockMessageRelaxed(0, 0, 0);

        Cell actionCell = Builder.beginCell()
                .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                .storeUint(PAY_GAS_SEPARATELY, 8)
                .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg, null)).endCell())
                .endCell();

        OutList.OutAction actual = OutList.loadOutAction(actionCell.beginParse());
        assertTrue(actual instanceof OutList.OutActionSendMsg);

        OutList.OutActionSendMsg a = (OutList.OutActionSendMsg) actual;
        assertEquals("sendMsg", a.type());
        assertEquals(PAY_GAS_SEPARATELY, a.mode);

        assertTrue(msg.body.equals(a.outMsg.body));
        assertEquals(msg.init, a.outMsg.init);
        assertEquals(msg.info, a.outMsg.info);
    }

    @Test
    void shouldDeserializeSetCodeAction() {
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();

        Cell actionCell = Builder.beginCell()
                .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                .storeRef(mockSetCodeCell)
                .endCell();

        OutList.OutAction actual = OutList.loadOutAction(actionCell.beginParse());
        assertTrue(actual instanceof OutList.OutActionSetCode);

        OutList.OutActionSetCode a = (OutList.OutActionSetCode) actual;
        assertEquals("setCode", a.type());
        assertTrue(mockSetCodeCell.equals(a.newCode));
    }

    @Test
    void shouldSerializeOutList() {
        MessageRelaxed msg1 = mockMessageRelaxed(0, 0, 0);
        MessageRelaxed msg2 = mockMessageRelaxed(1, 1, 1);
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();

        List<OutList.OutAction> actions = List.of(
                new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg1),
                new OutList.OutActionSendMsg(IGNORE_ERRORS, msg2),
                new OutList.OutActionSetCode(mockSetCodeCell)
        );

        Cell actual = Builder.beginCell().store(OutList.storeOutList(actions)).endCell();

        // Expected как в TS-спеке (c5 reverse)
        Cell expected =
                Builder.beginCell()
                        .storeRef(
                                Builder.beginCell()
                                        .storeRef(
                                                Builder.beginCell()
                                                        .storeRef(Builder.beginCell().endCell())
                                                        .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                                                        .storeUint(PAY_GAS_SEPARATELY, 8)
                                                        .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg1, null)).endCell())
                                                        .endCell()
                                        )
                                        .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                                        .storeUint(IGNORE_ERRORS, 8)
                                        .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg2, null)).endCell())
                                        .endCell()
                        )
                        .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                        .storeRef(mockSetCodeCell)
                        .endCell();

        assertTrue(actual.equals(expected));
    }

    @Test
    void shouldDeserializeOutList() {
        MessageRelaxed msg1 = mockMessageRelaxed(0, 0, 0);
        MessageRelaxed msg2 = mockMessageRelaxed(1, 1, 1);
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();

        List<OutList.OutAction> expected = List.of(
                new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg1),
                new OutList.OutActionSendMsg(IGNORE_ERRORS, msg2),
                new OutList.OutActionSetCode(mockSetCodeCell)
        );

        Cell rawList =
                Builder.beginCell()
                        .storeRef(
                                Builder.beginCell()
                                        .storeRef(
                                                Builder.beginCell()
                                                        .storeRef(Builder.beginCell().endCell())
                                                        .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                                                        .storeUint(PAY_GAS_SEPARATELY, 8)
                                                        .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg1, null)).endCell())
                                                        .endCell()
                                        )
                                        .storeUint(OUT_ACTION_SEND_MSG_TAG, 32)
                                        .storeUint(IGNORE_ERRORS, 8)
                                        .storeRef(Builder.beginCell().store(MessageRelaxed.storeMessageRelaxed(msg2, null)).endCell())
                                        .endCell()
                        )
                        .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                        .storeRef(mockSetCodeCell)
                        .endCell();

        List<OutList.OutAction> actual = OutList.loadOutList(rawList.beginParse());

        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            OutList.OutAction e = expected.get(i);
            OutList.OutAction a = actual.get(i);

            assertEquals(e.type(), a.type());

            if (e instanceof OutList.OutActionSendMsg es && a instanceof OutList.OutActionSendMsg as) {
                assertEquals(es.mode, as.mode);
                assertTrue(es.outMsg.body.equals(as.outMsg.body));
                assertEquals(es.outMsg.info, as.outMsg.info);
                assertEquals(es.outMsg.init, as.outMsg.init);
            }

            if (e instanceof OutList.OutActionSetCode ec && a instanceof OutList.OutActionSetCode ac) {
                assertTrue(ec.newCode.equals(ac.newCode));
            }
        }
    }
}
