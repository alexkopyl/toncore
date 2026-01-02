package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.Objects;

public final class Transaction {

    private final BigInteger address;              // uint256
    private final BigInteger lt;                   // uint64
    private final BigInteger prevTransactionHash;  // uint256
    private final BigInteger prevTransactionLt;    // uint64
    private final long now;                        // uint32
    private final int outMessagesCount;            // uint15
    private final AccountStatus oldStatus;
    private final AccountStatus endStatus;

    private final Message inMessage; // nullable
    private final Dictionary<BigInteger, Message> outMessages;

    private final CurrencyCollection totalFees;
    private final HashUpdate stateUpdate;
    private final TransactionDescription description;

    private final Cell raw;

    public Transaction(
            BigInteger address,
            BigInteger lt,
            BigInteger prevTransactionHash,
            BigInteger prevTransactionLt,
            long now,
            int outMessagesCount,
            AccountStatus oldStatus,
            AccountStatus endStatus,
            Message inMessage,
            Dictionary<BigInteger, Message> outMessages,
            CurrencyCollection totalFees,
            HashUpdate stateUpdate,
            TransactionDescription description,
            Cell raw
    ) {
        this.address = Objects.requireNonNull(address);
        this.lt = Objects.requireNonNull(lt);
        this.prevTransactionHash = Objects.requireNonNull(prevTransactionHash);
        this.prevTransactionLt = Objects.requireNonNull(prevTransactionLt);
        this.now = now;
        this.outMessagesCount = outMessagesCount;
        this.oldStatus = Objects.requireNonNull(oldStatus);
        this.endStatus = Objects.requireNonNull(endStatus);
        this.inMessage = inMessage;
        this.outMessages = Objects.requireNonNull(outMessages);
        this.totalFees = Objects.requireNonNull(totalFees);
        this.stateUpdate = Objects.requireNonNull(stateUpdate);
        this.description = Objects.requireNonNull(description);
        this.raw = Objects.requireNonNull(raw);
    }

    public BigInteger address() { return address; }
    public BigInteger lt() { return lt; }
    public BigInteger prevTransactionHash() { return prevTransactionHash; }
    public BigInteger prevTransactionLt() { return prevTransactionLt; }
    public long now() { return now; }
    public int outMessagesCount() { return outMessagesCount; }
    public AccountStatus oldStatus() { return oldStatus; }
    public AccountStatus endStatus() { return endStatus; }
    public Message inMessage() { return inMessage; } // nullable
    public Dictionary<BigInteger, Message> outMessages() { return outMessages; }
    public CurrencyCollection totalFees() { return totalFees; }
    public HashUpdate stateUpdate() { return stateUpdate; }
    public TransactionDescription description() { return description; }
    public Cell raw() { return raw; }
    public byte[] hash() { return raw.hash(); }

    public static Transaction load(Slice slice) {
        Cell raw = slice.asCell();

        long tag = slice.loadUint(4);
        if (tag != 0x07L) {
            throw new IllegalArgumentException("Invalid Transaction tag: " + tag);
        }

        BigInteger address = slice.loadUintBig(256);
        BigInteger lt = slice.loadUintBig(64);
        BigInteger prevTransactionHash = slice.loadUintBig(256);
        BigInteger prevTransactionLt = slice.loadUintBig(64);
        long now = slice.loadUint(32);
        int outMessagesCount = (int) slice.loadUint(15);

        AccountStatus oldStatus = AccountStatus.loadAccountStatus(slice);
        AccountStatus endStatus = AccountStatus.loadAccountStatus(slice);

        // ^[ in_msg:(Maybe ^(Message Any)) out_msgs:(HashmapE 15 ^(Message Any)) ]
        Cell msgRef = slice.loadRef();
        Slice msgSlice = msgRef.beginParse();

        Message inMessage = null;
        boolean hasInMsg = msgSlice.loadBit();
        if (hasInMsg) {
            Cell inMsgRef = msgSlice.loadRef();
            inMessage = Message.loadMessage(inMsgRef.beginParse());
        }

        // out_msgs:(HashmapE 15 ^(Message Any))
        // Здесь предполагается, что у вас уже есть готовый ValueCodec/Adapter для Message, аналог MessageValue из TS.
        Dictionary<BigInteger, Message> outMessages =
                msgSlice.loadDict(Dictionary.Keys.BigUint(15), Message.MessageValue); // см. примечание ниже

        msgSlice.endParse();

        CurrencyCollection totalFees = CurrencyCollection.loadCurrencyCollection(slice);

        HashUpdate stateUpdate = HashUpdate.load(slice.loadRef().beginParse());
        TransactionDescription description = TransactionDescription.load(slice.loadRef().beginParse());

        return new Transaction(
                address,
                lt,
                prevTransactionHash,
                prevTransactionLt,
                now,
                outMessagesCount,
                oldStatus,
                endStatus,
                inMessage,
                outMessages,
                totalFees,
                stateUpdate,
                description,
                raw
        );
    }

    public void store(Builder builder) {
        builder.storeUint(0x07, 4);
        builder.storeUint(address, 256);
        builder.storeUint(lt, 64);
        builder.storeUint(prevTransactionHash, 256);
        builder.storeUint(prevTransactionLt, 64);
        builder.storeUint(now, 32);
        builder.storeUint(outMessagesCount, 15);

        builder.store(AccountStatus.storeAccountStatus(oldStatus));
        builder.store(AccountStatus.storeAccountStatus(endStatus));

        // ^[ in_msg:(Maybe ^Message) out_msgs:(HashmapE 15 ^Message) ]
        Builder msgBuilder = Builder.beginCell();

        if (inMessage != null) {
            msgBuilder.storeBit(true);
            msgBuilder.storeRef(
                    Builder.beginCell()
                            .store(Message.storeMessage(inMessage, Message.StoreOptions.none()))
            ); // storeRef(Builder) сам сделает endCell()
        } else {
            msgBuilder.storeBit(false);
        }

        msgBuilder.storeDict(outMessages, Dictionary.Keys.BigUint(15), Message.MessageValue);
        builder.storeRef(msgBuilder); // можно так же, как в TS

        builder.store(CurrencyCollection.storeCurrencyCollection(totalFees));

        builder.storeRef(Builder.beginCell().store(stateUpdate)); // HashUpdate = Writable, ок
        builder.storeRef(
                Builder.beginCell().store(TransactionDescription.storeTransactionDescription(description))
        );
    }

}
