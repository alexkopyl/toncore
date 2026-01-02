import dev.quark.ton.core.boc.CellType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellTypeTest {

    @Test
    void shouldMatchCppValues() {
        assertEquals(-1, CellType.Ordinary.getCode());
        assertEquals(1, CellType.PrunedBranch.getCode());
        assertEquals(2, CellType.Library.getCode());
        assertEquals(3, CellType.MerkleProof.getCode());
        assertEquals(4, CellType.MerkleUpdate.getCode());
    }
}
