package dev.quark.ton.core.contract;

import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.SendMode;
import dev.quark.ton.core.tuple.Tuple;
import dev.quark.ton.core.tuple.TupleReader;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Port of ton-core/src/contract/ContractProvider.ts
 */
public interface ContractProvider {



    // TS: getState(): Promise<ContractState>
    CompletableFuture<ContractState> getState();

    // TS: get(name: string, args: TupleItem[]): Promise<ContractGetMethodResult>
    CompletableFuture<ContractGetMethodResult> get(String name, List<Tuple.TupleItem> args);
    CompletableFuture<ContractGetMethodResult> get(int methodId, List<Tuple.TupleItem> args);


    // TS: external(message: Cell): Promise<void>
    CompletableFuture<Void> external(Cell message);

    /**
     * TS:
     * internal(via: Sender, args: { value: bigint | string, bounce?: Maybe<boolean>, sendMode?: SendMode, body?: Maybe<Cell | string> }): Promise<void>
     *
     * Java: переносим 1:1 через DTO, value/body допускают разные типы.
     */
    CompletableFuture<Void> internal(Sender via, InternalArgs args);

    // ---------------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------------


    final class ContractGetMethodResult {
        private final TupleReader stack;
        private final BigInteger gasUsed; // nullable
        private final String logs;        // nullable
        private final Map<Long, BigInteger> extracurrency; // nullable
        public Map<Long, BigInteger> extracurrency() { return extracurrency; }


        public ContractGetMethodResult(TupleReader stack, BigInteger gasUsed, String logs, Map<Long, BigInteger> extracurrency) {
            this.stack = stack;
            this.gasUsed = gasUsed;
            this.logs = logs;
            this.extracurrency = extracurrency;
        }

        public TupleReader stack() { return stack; }
        public BigInteger gasUsed() { return gasUsed; }
        public String logs() { return logs; }
    }

    final class InternalArgs {
        /**
         * TS: bigint | string
         * Java: BigInteger or String
         */
        private final Object value;

        private final Boolean bounce;   // nullable
        private final SendMode sendMode; // nullable

        /**
         * TS: Cell | string
         * Java: Cell or String
         */
        private final Object body; // nullable

        public InternalArgs(Object value, Boolean bounce, SendMode sendMode, Object body) {
            this.value = value;
            this.bounce = bounce;
            this.sendMode = sendMode;
            this.body = body;
        }

        public Object value() { return value; }
        public Boolean bounce() { return bounce; }
        public SendMode sendMode() { return sendMode; }
        public Object body() { return body; }
    }
}
