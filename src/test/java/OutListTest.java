import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class OutListTest {

    // Tags как в TS-спеке
    private static final long OUT_ACTION_SEND_MSG_TAG = 0x0ec3c86dL;
    private static final long OUT_ACTION_SET_CODE_TAG = 0xad4de08eL;
    private static final long OUT_ACTION_RESERVE_TAG = 0x36e6b809L;
    private static final long OUT_ACTION_CHANGE_LIBRARY_TAG = 0x26fa1dd4L;

    // Любые валидные значения (uint8/uint7)
    private static final int PAY_GAS_SEPARATELY = 1;
    private static final int IGNORE_ERRORS = 2;

    private static MessageRelaxed mockMessageRelaxed(int createdLt, int createdAt, int bodyByte) {
        var info = new CommonMessageInfoRelaxedTLB.ExternalOut(
                null,
                null,
                BigInteger.valueOf(createdLt),
                createdAt
        );

        Cell body = Builder.beginCell().storeUint(bodyByte, 8).endCell();
        return new MessageRelaxed(info, null, body);
    }

    private static CurrencyCollection currencyCoins(long coins) {
        return new CurrencyCollection(null, BigInteger.valueOf(coins));
    }

    // ----------------------------
    // OutAction: serialize
    // ----------------------------

    @Test
    void shouldSerialiseSendMsgAction() {
        MessageRelaxed msg = mockMessageRelaxed(0, 0, 0);
        OutList.OutAction action = new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg);

        Cell actual = Builder.beginCell().store(OutList.storeOutAction(action)).endCell();

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

        Cell actual = Builder.beginCell().store(OutList.storeOutAction(action)).endCell();

        Cell expected = Builder.beginCell()
                .storeUint(OUT_ACTION_SET_CODE_TAG, 32)
                .storeRef(mockSetCodeCell)
                .endCell();

        assertTrue(expected.equals(actual));
    }

    @Test
    void shouldSerialiseReserveAction() {
        ReserveMode mode = ReserveMode.AT_MOST_THIS_AMOUNT;

        OutList.OutAction action =
                new OutList.OutActionReserve(mode, currencyCoins(2_000_000));

        Cell actual = Builder.beginCell().store(OutList.storeOutAction(action)).endCell();

        Cell expected = Builder.beginCell()
                .storeUint(OUT_ACTION_RESERVE_TAG, 32)
                .storeUint(mode.value(), 8)
                .store(CurrencyCollection.storeCurrencyCollection(currencyCoins(2_000_000)))
                .endCell();

        assertTrue(expected.equals(actual));
    }

    @Test
    void shouldSerialiseChangeLibraryAction() {
        int mode = 0;
        Cell lib = Builder.beginCell().storeUint(1234, 16).endCell();
        LibRef libRef = new LibRef.LibRefRef(lib);

        OutList.OutAction action = new OutList.OutActionChangeLibrary(mode, libRef);

        Cell actual = Builder.beginCell().store(OutList.storeOutAction(action)).endCell();

        Cell expected = Builder.beginCell()
                .storeUint(OUT_ACTION_CHANGE_LIBRARY_TAG, 32)
                .storeUint(mode, 7)
                .store((Consumer<Builder>) b -> libRef.writeTo(b))
                .endCell();

        assertTrue(expected.equals(actual));
    }

    // ----------------------------
    // OutAction: deserialize
    // ----------------------------

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
    void shouldDeserializeReserveAction() {
        ReserveMode mode = ReserveMode.LEAVE_THIS_AMOUNT;

        Cell actionCell = Builder.beginCell()
                .storeUint(OUT_ACTION_RESERVE_TAG, 32)
                .storeUint(mode.value(), 8)
                .store(CurrencyCollection.storeCurrencyCollection(currencyCoins(3_000_000)))
                .endCell();

        OutList.OutAction actual = OutList.loadOutAction(actionCell.beginParse());
        assertTrue(actual instanceof OutList.OutActionReserve);

        OutList.OutActionReserve a = (OutList.OutActionReserve) actual;
        assertEquals("reserve", a.type());
        assertEquals(mode.value(), a.mode);         // raw
        assertEquals(mode, a.modeEnum);             // enum mapping
        assertEquals(BigInteger.valueOf(3_000_000), a.currency.coins());
    }

    @Test
    void shouldDeserializeChangeLibraryAction() {
        int mode = 1;
        byte[] libHash = new byte[32];
        LibRef libRef = new LibRef.LibRefHash(libHash);

        Cell actionCell = Builder.beginCell()
                .storeUint(OUT_ACTION_CHANGE_LIBRARY_TAG, 32)
                .storeUint(mode, 7)
                .store((Consumer<Builder>) b -> libRef.writeTo(b))
                .endCell();

        OutList.OutAction actual = OutList.loadOutAction(actionCell.beginParse());
        assertTrue(actual instanceof OutList.OutActionChangeLibrary);

        OutList.OutActionChangeLibrary a = (OutList.OutActionChangeLibrary) actual;
        assertEquals("changeLibrary", a.type());
        assertEquals(mode, a.mode);
        assertEquals(libRef, a.libRef);
    }

    // ----------------------------
    // OutList: serialize / deserialize (как в TS)
    // ----------------------------

    @Test
    void shouldSerializeOutList_FullSpecVector() {
        MessageRelaxed msg1 = mockMessageRelaxed(0, 0, 0);
        MessageRelaxed msg2 = mockMessageRelaxed(1, 1, 1);
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();

        ReserveMode reserveMode = ReserveMode.LEAVE_THIS_AMOUNT;
        int changeLibraryMode = 1;
        LibRef libRef = new LibRef.LibRefRef(Builder.beginCell().storeUint(1234, 16).endCell());

        List<OutList.OutAction> actions = List.of(
                new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg1),
                new OutList.OutActionSendMsg(IGNORE_ERRORS, msg2),
                new OutList.OutActionSetCode(mockSetCodeCell),
                new OutList.OutActionReserve(reserveMode, currencyCoins(3_000_000)),
                new OutList.OutActionChangeLibrary(changeLibraryMode, libRef)
        );

        Cell actual = Builder.beginCell().store(OutList.storeOutList(actions)).endCell();

        Cell expected = Builder.beginCell()
                .storeRef(
                        Builder.beginCell()
                                .storeRef(
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
                                                .endCell()
                                )
                                .storeUint(OUT_ACTION_RESERVE_TAG, 32)
                                .storeUint(reserveMode.value(), 8)
                                .store(CurrencyCollection.storeCurrencyCollection(currencyCoins(3_000_000)))
                                .endCell()
                )
                .storeUint(OUT_ACTION_CHANGE_LIBRARY_TAG, 32)
                .storeUint(changeLibraryMode, 7)
                .store((Consumer<Builder>) b -> libRef.writeTo(b))
                .endCell();

        assertTrue(actual.equals(expected));
    }

    @Test
    void shouldDeserializeOutList_FullSpecVector() {
        MessageRelaxed msg1 = mockMessageRelaxed(0, 0, 0);
        MessageRelaxed msg2 = mockMessageRelaxed(1, 1, 1);
        Cell mockSetCodeCell = Builder.beginCell().storeUint(123, 8).endCell();

        ReserveMode reserveMode = ReserveMode.LEAVE_THIS_AMOUNT;
        int changeLibraryMode = 1;
        LibRef libRef = new LibRef.LibRefRef(Builder.beginCell().storeUint(1234, 16).endCell());

        List<OutList.OutAction> expected = List.of(
                new OutList.OutActionSendMsg(PAY_GAS_SEPARATELY, msg1),
                new OutList.OutActionSendMsg(IGNORE_ERRORS, msg2),
                new OutList.OutActionSetCode(mockSetCodeCell),
                new OutList.OutActionReserve(reserveMode, currencyCoins(3_000_000)),
                new OutList.OutActionChangeLibrary(changeLibraryMode, libRef)
        );

        Cell rawList = Builder.beginCell()
                .storeRef(
                        Builder.beginCell()
                                .storeRef(
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
                                                .endCell()
                                )
                                .storeUint(OUT_ACTION_RESERVE_TAG, 32)
                                .storeUint(reserveMode.value(), 8)
                                .store(CurrencyCollection.storeCurrencyCollection(currencyCoins(3_000_000)))
                                .endCell()
                )
                .storeUint(OUT_ACTION_CHANGE_LIBRARY_TAG, 32)
                .storeUint(changeLibraryMode, 7)
                .store((Consumer<Builder>) b -> libRef.writeTo(b))
                .endCell();

        List<OutList.OutAction> actual = OutList.loadOutList(rawList.beginParse());

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).type(), actual.get(i).type());
        }
    }
}
