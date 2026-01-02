package dev.quark.ton.core.contract;

import java.util.List;
import java.util.Map;

/**
 * Port of ton-core/src/contract/ContractABI.ts
 *
 * Note:
 * - Maybe<T> -> nullable fields in Java
 * - TS unions -> sealed interfaces + implementations
 * - format fields in TS are (string | number | boolean) -> Object in Java for 1:1
 */
public final class ContractABI {

    private final String name;                 // nullable
    private final List<ABIType> types;         // nullable
    private final Map<Integer, ABIError> errors; // nullable
    private final List<ABIGetter> getters;     // nullable
    private final List<ABIReceiver> receivers; // nullable

    public ContractABI(
            String name,
            List<ABIType> types,
            Map<Integer, ABIError> errors,
            List<ABIGetter> getters,
            List<ABIReceiver> receivers
    ) {
        this.name = name;
        this.types = types;
        this.errors = errors;
        this.getters = getters;
        this.receivers = receivers;
    }

    public String name() { return name; }
    public List<ABIType> types() { return types; }
    public Map<Integer, ABIError> errors() { return errors; }
    public List<ABIGetter> getters() { return getters; }
    public List<ABIReceiver> receivers() { return receivers; }

    // ---------------------------------------------------------------------
    // ABIError
    // ---------------------------------------------------------------------

    public static final class ABIError {
        private final String message;

        public ABIError(String message) {
            this.message = message;
        }

        public String message() { return message; }
    }

    // ---------------------------------------------------------------------
    // ABITypeRef (union: simple | dict)
    // ---------------------------------------------------------------------

    public sealed interface ABITypeRef permits SimpleTypeRef, DictTypeRef {
        String kind();
        Object format(); // nullable
    }

    public static final class SimpleTypeRef implements ABITypeRef {
        private final String type;
        private final Boolean optional; // nullable
        private final Object format;    // nullable

        public SimpleTypeRef(String type, Boolean optional, Object format) {
            this.type = type;
            this.optional = optional;
            this.format = format;
        }

        @Override
        public String kind() { return "simple"; }

        public String type() { return type; }
        public Boolean optional() { return optional; }

        @Override
        public Object format() { return format; }
    }

    public static final class DictTypeRef implements ABITypeRef {
        private final Object format;      // nullable
        private final String key;
        private final Object keyFormat;   // nullable
        private final String value;
        private final Object valueFormat; // nullable

        public DictTypeRef(Object format, String key, Object keyFormat, String value, Object valueFormat) {
            this.format = format;
            this.key = key;
            this.keyFormat = keyFormat;
            this.value = value;
            this.valueFormat = valueFormat;
        }

        @Override
        public String kind() { return "dict"; }

        @Override
        public Object format() { return format; }

        public String key() { return key; }
        public Object keyFormat() { return keyFormat; }
        public String value() { return value; }
        public Object valueFormat() { return valueFormat; }
    }

    // ---------------------------------------------------------------------
    // ABIField, ABIType
    // ---------------------------------------------------------------------

    public static final class ABIField {
        private final String name;
        private final ABITypeRef type;

        public ABIField(String name, ABITypeRef type) {
            this.name = name;
            this.type = type;
        }

        public String name() { return name; }
        public ABITypeRef type() { return type; }
    }

    public static final class ABIType {
        private final String name;
        private final Integer header;         // nullable
        private final List<ABIField> fields;  // required in TS

        public ABIType(String name, Integer header, List<ABIField> fields) {
            this.name = name;
            this.header = header;
            this.fields = fields;
        }

        public String name() { return name; }
        public Integer header() { return header; }
        public List<ABIField> fields() { return fields; }
    }

    // ---------------------------------------------------------------------
    // ABIArgument, ABIGetter
    // ---------------------------------------------------------------------

    public static final class ABIArgument {
        private final String name;
        private final ABITypeRef type;

        public ABIArgument(String name, ABITypeRef type) {
            this.name = name;
            this.type = type;
        }

        public String name() { return name; }
        public ABITypeRef type() { return type; }
    }

    public static final class ABIGetter {
        private final String name;
        private final Integer methodId;              // nullable
        private final List<ABIArgument> arguments;   // nullable
        private final ABITypeRef returnType;         // nullable

        public ABIGetter(String name, Integer methodId, List<ABIArgument> arguments, ABITypeRef returnType) {
            this.name = name;
            this.methodId = methodId;
            this.arguments = arguments;
            this.returnType = returnType;
        }

        public String name() { return name; }
        public Integer methodId() { return methodId; }
        public List<ABIArgument> arguments() { return arguments; }
        public ABITypeRef returnType() { return returnType; }
    }

    // ---------------------------------------------------------------------
    // ABIReceiverMessage (union)
    // ---------------------------------------------------------------------

    public sealed interface ABIReceiverMessage
            permits ReceiverTypedMessage, ReceiverAnyMessage, ReceiverEmptyMessage, ReceiverTextMessage {
        String kind();
    }

    public static final class ReceiverTypedMessage implements ABIReceiverMessage {
        private final String type;

        public ReceiverTypedMessage(String type) {
            this.type = type;
        }

        @Override
        public String kind() { return "typed"; }

        public String type() { return type; }
    }

    public static final class ReceiverAnyMessage implements ABIReceiverMessage {
        @Override
        public String kind() { return "any"; }
    }

    public static final class ReceiverEmptyMessage implements ABIReceiverMessage {
        @Override
        public String kind() { return "empty"; }
    }

    public static final class ReceiverTextMessage implements ABIReceiverMessage {
        private final String text; // nullable

        public ReceiverTextMessage(String text) {
            this.text = text;
        }

        @Override
        public String kind() { return "text"; }

        public String text() { return text; }
    }

    // ---------------------------------------------------------------------
    // ABIReceiver
    // ---------------------------------------------------------------------

    public static final class ABIReceiver {
        public enum Receiver {
            INTERNAL, EXTERNAL
        }

        private final Receiver receiver;
        private final ABIReceiverMessage message;

        public ABIReceiver(Receiver receiver, ABIReceiverMessage message) {
            this.receiver = receiver;
            this.message = message;
        }

        public Receiver receiver() { return receiver; }
        public ABIReceiverMessage message() { return message; }
    }
}
