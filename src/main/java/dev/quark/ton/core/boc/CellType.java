package dev.quark.ton.core.boc;

public enum CellType {

    Ordinary(-1),
    PrunedBranch(1),
    Library(2),
    MerkleProof(3),
    MerkleUpdate(4);

    private final int code;

    CellType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CellType fromCode(int code) {
        for (CellType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown CellType code: " + code);
    }
}
