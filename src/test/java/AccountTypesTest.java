import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.AccountState;
import dev.quark.ton.core.types.AccountStatus;
import dev.quark.ton.core.types.AccountStatusChange;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class AccountTypesTest {

    @Test
    void accountStatus_roundtrip() {
        for (AccountStatus s : AccountStatus.values()) {
            Cell c = Builder.beginCell().store(AccountStatus.storeAccountStatus(s)).endCell();
            AccountStatus r = AccountStatus.loadAccountStatus(c.beginParse());
            assertEquals(s, r);
        }
    }

    @Test
    void accountState_uninit_roundtrip() {
        AccountState st = new AccountState.Uninit();
        Cell c = Builder.beginCell().store(AccountState.storeAccountState(st)).endCell();
        AccountState r = AccountState.loadAccountState(c.beginParse());
        assertTrue(r instanceof AccountState.Uninit);
    }

    @Test
    void accountState_frozen_roundtrip() {
        AccountState st = new AccountState.Frozen(new BigInteger("123"));
        Cell c = Builder.beginCell().store(AccountState.storeAccountState(st)).endCell();
        AccountState r = AccountState.loadAccountState(c.beginParse());
        assertTrue(r instanceof AccountState.Frozen);
        assertEquals(new BigInteger("123"), ((AccountState.Frozen) r).stateHash);
    }

    @Test
    void accountStatusChange_roundtrip() {
        for (AccountStatusChange s : AccountStatusChange.values()) {
            Cell c = Builder.beginCell().store(AccountStatusChange.storeAccountStatusChange(s)).endCell();
            AccountStatusChange r = AccountStatusChange.load(c.beginParse());
            assertEquals(s, r);
        }
    }

}
