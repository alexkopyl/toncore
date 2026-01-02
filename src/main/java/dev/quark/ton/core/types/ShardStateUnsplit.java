package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.Objects;

/**
 * TL-B:
 * shard_state#9023afe2 global_id:int32
 *   shard_id:ShardIdent
 *   seq_no:uint32 vert_seq_no:#
 *   gen_utime:uint32 gen_lt:uint64
 *   min_ref_mc_seqno:uint32
 *   out_msg_queue_info:^OutMsgQueueInfo
 *   before_split:(## 1)
 *   accounts:^ShardAccounts
 *   ^[ ... skipped ... ]
 *   custom:(Maybe ^McStateExtra)
 *   = ShardStateUnsplit;
 *
 * 1:1 port of ShardStateUnsplit.ts
 */
public final class ShardStateUnsplit {

    public final int globalId;
    public final ShardIdent shardId;
    public final long seqno;
    public final long vertSeqNo;
    public final long genUtime;
    public final BigInteger genLt;
    public final long minRefMcSeqno;
    public final boolean beforeSplit;
    public final Dictionary<BigInteger, ShardAccounts.ShardAccountRef> accounts; // nullable
    public final MasterchainStateExtra extras; // nullable

    public ShardStateUnsplit(
            int globalId,
            ShardIdent shardId,
            long seqno,
            long vertSeqNo,
            long genUtime,
            BigInteger genLt,
            long minRefMcSeqno,
            boolean beforeSplit,
            Dictionary<BigInteger, ShardAccounts.ShardAccountRef> accounts,
            MasterchainStateExtra extras
    ) {
        this.globalId = globalId;
        this.shardId = Objects.requireNonNull(shardId, "shardId");
        this.seqno = seqno;
        this.vertSeqNo = vertSeqNo;
        this.genUtime = genUtime;
        this.genLt = Objects.requireNonNull(genLt, "genLt");
        this.minRefMcSeqno = minRefMcSeqno;
        this.beforeSplit = beforeSplit;
        this.accounts = accounts;
        this.extras = extras;
    }

    public static ShardStateUnsplit loadShardStateUnsplit(Slice cs) {
        long magic = cs.loadUint(32);
        if (magic != 0x9023afe2L) {
            throw new IllegalArgumentException("Invalid ShardStateUnsplit magic: " + magic);
        }

        int globalId = (int) cs.loadInt(32);
        ShardIdent shardId = ShardIdent.loadShardIdent(cs);
        long seqno = cs.loadUint(32);
        long vertSeqNo = cs.loadUint(32);
        long genUtime = cs.loadUint(32);
        BigInteger genLt = cs.loadUintBig(64);
        long minRefMcSeqno = cs.loadUint(32);

        // Skip OutMsgQueueInfo (usually exotic)
        cs.loadRef();

        boolean beforeSplit = cs.loadBit();

        // Accounts
        Dictionary<BigInteger, ShardAccounts.ShardAccountRef> accounts = null;
        Cell accountsRef = cs.loadRef();
        if (!accountsRef.isExotic()) {
            accounts = ShardAccounts.loadShardAccounts(accountsRef.beginParse());
        }

        // Skip unused reference block
        cs.loadRef();

        // Extras (Maybe ^McStateExtra)
        MasterchainStateExtra extras = null;
        boolean hasExtras = cs.loadBit();
        if (hasExtras) {
            Cell ref = cs.loadRef();
            if (!ref.isExotic()) {
                extras = MasterchainStateExtra.loadMasterchainStateExtra(ref.beginParse());
            }
        }

        return new ShardStateUnsplit(
                globalId,
                shardId,
                seqno,
                vertSeqNo,
                genUtime,
                genLt,
                minRefMcSeqno,
                beforeSplit,
                accounts,
                extras
        );
    }
}
