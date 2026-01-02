package dev.quark.ton.core.address;

import dev.quark.ton.core.utils.Crc16;

import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Ported 1:1 from ton-core/src/address/Address.ts
 */
public final class Address {

    private static final int BOUNCEABLE_TAG = 0x11;
    private static final int NON_BOUNCEABLE_TAG = 0x51;
    private static final int TEST_FLAG = 0x80;

    private static final Pattern FRIENDLY_RE = Pattern.compile("^[A-Za-z0-9+/_-]+$");
    private static final Pattern RAW_HASH_RE = Pattern.compile("^[a-f0-9]+$");

    public static boolean isAddress(Object src) {
        return src instanceof Address;
    }

    public static boolean isFriendly(String source) {
        if (source == null || source.length() != 48) {
            return false;
        }
        return FRIENDLY_RE.matcher(source).matches();
    }

    public static boolean isRaw(String source) {
        if (source == null || source.indexOf(':') == -1) {
            return false;
        }

        String[] parts = source.split(":");
        if (parts.length != 2) return false;

        String wc = parts[0];
        String hash = parts[1];

        try {
            double d = Double.parseDouble(wc);
            if (d != Math.rint(d)) return false;
        } catch (NumberFormatException e) {
            return false;
        }

        if (hash.length() != 64) return false;
        return RAW_HASH_RE.matcher(hash.toLowerCase()).matches();
    }

    public static String normalize(Object source) {
        if (source instanceof String s) {
            return Address.parse(s).toString();
        } else if (source instanceof Address a) {
            return a.toString();
        } else {
            throw new IllegalArgumentException("Unknown address type: " + source);
        }
    }

    public static Address parse(String source) {
        if (Address.isFriendly(source)) {
            return Address.parseFriendly(source).address;
        } else if (Address.isRaw(source)) {
            return Address.parseRaw(source);
        } else {
            throw new IllegalArgumentException("Unknown address type: " + source);
        }
    }

    public static Address parseRaw(String source) {
        String[] parts = source.split(":");
        int workChain = Integer.parseInt(parts[0]);
        byte[] hash = hexToBytes(parts[1]);
        return new Address(workChain, hash);
    }

    public static ParseFriendlyResult parseFriendly(Object source) {
        if (source instanceof byte[] b) {
            ParsedFriendlyInternal r = parseFriendlyAddress(b, "<buffer>");
            return new ParseFriendlyResult(r.isBounceable, r.isTestOnly, new Address(r.workchain, r.hashPart));
        } else if (source instanceof String s) {
            // Convert from url-friendly to true base64
            String addr = s.replace('-', '+').replace('_', '/');
            ParsedFriendlyInternal r = parseFriendlyAddress(addr, s);
            return new ParseFriendlyResult(r.isBounceable, r.isTestOnly, new Address(r.workchain, r.hashPart));
        } else {
            throw new IllegalArgumentException("Unknown address type");
        }
    }

    /**
     * TS export function address(src: string) { return Address.parse(src); }
     */
    public static Address address(String src) {
        return Address.parse(src);
    }

    public final int workChain;
    public final byte[] hash;

    public Address(int workChain, byte[] hash) {
        if (hash.length != 32) {
            throw new IllegalArgumentException("Invalid address hash length: " + hash.length);
        }
        this.workChain = workChain;
        this.hash = Arrays.copyOf(hash, hash.length);
    }

    public String toRawString() {
        return this.workChain + ":" + bytesToHex(this.hash);
    }

    public boolean equals(Address src) {
        if (src.workChain != this.workChain) {
            return false;
        }
        return Arrays.equals(src.hash, this.hash);
    }

    /**
     * TS: toRaw()
     * addressWithChecksum = Buffer.alloc(36);
     * addressWithChecksum.set(this.hash);
     * addressWithChecksum.set([this.workChain,...] at 32)
     */
    public byte[] toRaw() {
        byte[] addressWithChecksum = new byte[36];
        System.arraycopy(this.hash, 0, addressWithChecksum, 0, 32);
        byte wc = (byte) this.workChain;
        addressWithChecksum[32] = wc;
        addressWithChecksum[33] = wc;
        addressWithChecksum[34] = wc;
        addressWithChecksum[35] = wc;
        return addressWithChecksum;
    }

    public byte[] toStringBuffer() {
        return toStringBuffer(new ToStringBufferArgs());
    }

    public byte[] toStringBuffer(ToStringBufferArgs args) {
        boolean testOnly = (args != null && args.testOnly != null) ? args.testOnly : false;
        boolean bounceable = (args != null && args.bounceable != null) ? args.bounceable : true;

        int tag = bounceable ? BOUNCEABLE_TAG : NON_BOUNCEABLE_TAG;
        if (testOnly) {
            tag |= TEST_FLAG;
        }

        byte[] addr = new byte[34];
        addr[0] = (byte) tag;
        addr[1] = (byte) this.workChain; // TS behavior (for -1 => 0xFF)
        System.arraycopy(this.hash, 0, addr, 2, 32);

        byte[] addressWithChecksum = new byte[36];
        System.arraycopy(addr, 0, addressWithChecksum, 0, 34);

        byte[] crc = Crc16.crc16(addr);
        addressWithChecksum[34] = crc[0];
        addressWithChecksum[35] = crc[1];

        return addressWithChecksum;
    }


    public String toString(ToStringArgs args) {
        boolean urlSafe = (args != null && args.urlSafe != null) ? args.urlSafe : true;

        byte[] buffer = toStringBuffer(args == null ? null : args.toBufferArgs());

        String b64 = Base64.getEncoder().encodeToString(buffer);
        if (urlSafe) {
            return b64.replace('+', '-').replace('/', '_');
        } else {
            return b64;
        }
    }

    @Override
    public String toString() {
        return toString(new ToStringArgs());
    }

    /* ============================================================ */
    /* ====================== internal parsing ==================== */
    /* ============================================================ */

    private static ParsedFriendlyInternal parseFriendlyAddress(byte[] data, String originalForError) {

        // 1byte tag + 1byte workchain + 32 bytes hash + 2 byte crc
        if (data.length != 36) {
            throw new IllegalArgumentException("Unknown address type: byte length is not equal to 36");
        }

        byte[] addr = Arrays.copyOfRange(data, 0, 34);
        byte[] crc = Arrays.copyOfRange(data, 34, 36);

        byte[] calcedCrc = Crc16.crc16(addr);
        if (!(calcedCrc[0] == crc[0] && calcedCrc[1] == crc[1])) {
            throw new IllegalArgumentException("Invalid checksum: " + originalForError);
        }

        // Parse tag
        int tag = addr[0] & 0xFF;
        boolean isTestOnly = false;
        boolean isBounceable;

        if ( (tag & TEST_FLAG) != 0) {
            isTestOnly = true;
            tag = tag ^ TEST_FLAG;
        }

        if (tag != BOUNCEABLE_TAG && tag != NON_BOUNCEABLE_TAG) {
            throw new IllegalArgumentException("Unknown address tag");
        }

        isBounceable = (tag == BOUNCEABLE_TAG);

        Integer workchain;
        int wcByte = addr[1] & 0xFF;
        if (wcByte == 0xFF) {
            workchain = -1; // TS note: should read signed, but they special-case 0xFF
        } else {
            workchain = wcByte;
        }

        byte[] hashPart = Arrays.copyOfRange(addr, 2, 34);

        return new ParsedFriendlyInternal(isTestOnly, isBounceable, workchain, hashPart);
    }

    private static ParsedFriendlyInternal parseFriendlyAddress(String base64, String originalForError) {

        if (base64 instanceof String s && !Address.isFriendly(originalForError)) {
            throw new IllegalArgumentException("Unknown address type");
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            // will behave as unknown address type in TS path if not base64
            throw new IllegalArgumentException("Unknown address type");
        }

        return parseFriendlyAddress(data, originalForError);
    }

    private record ParsedFriendlyInternal(boolean isTestOnly, boolean isBounceable, int workchain, byte[] hashPart) {}

    /* ============================================================ */
    /* ========================== results ========================= */
    /* ============================================================ */

    public static final class ParseFriendlyResult {
        public final boolean isBounceable;
        public final boolean isTestOnly;
        public final Address address;

        public ParseFriendlyResult(boolean isBounceable, boolean isTestOnly, Address address) {
            this.isBounceable = isBounceable;
            this.isTestOnly = isTestOnly;
            this.address = address;
        }
    }

    public static final class ToStringBufferArgs {
        public Boolean bounceable;
        public Boolean testOnly;
    }

    public static final class ToStringArgs {
        public Boolean urlSafe;
        public Boolean bounceable;
        public Boolean testOnly;

        private ToStringBufferArgs toBufferArgs() {
            ToStringBufferArgs r = new ToStringBufferArgs();
            r.bounceable = this.bounceable;
            r.testOnly = this.testOnly;
            return r;
        }
    }

    /* ============================================================ */
    /* ============================ hex =========================== */
    /* ============================================================ */

    private static byte[] hexToBytes(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        String h = hex.toLowerCase();

        // Node Buffer.from(str, 'hex') effectively ignores trailing nibble
        if ((h.length() & 1) != 0) {
            h = h.substring(0, h.length() - 1);
        }

        int maxLen = h.length() / 2;
        byte[] tmp = new byte[maxLen];
        int out = 0;

        for (int i = 0; i < maxLen; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);

            // Node-like: stop parsing on invalid hex
            if (hi < 0 || lo < 0) {
                break;
            }

            tmp[out++] = (byte) ((hi << 4) | lo);
        }

        return (out == tmp.length) ? tmp : Arrays.copyOf(tmp, out);
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
