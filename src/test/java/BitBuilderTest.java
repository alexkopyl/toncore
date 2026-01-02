
import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.BitBuilder;
import dev.quark.ton.core.boc.BitString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class BitBuilderTest {

    @Test
    void shouldSerializeUint() {
        Object[][] cases = new Object[][]{
                {10290L, 29, "00014194_"},
                {41732L, 27, "0014609_"},
                {62757L, 22, "03D496_"},
                {44525L, 16, "ADED"},
                {26925L, 30, "0001A4B6_"},
                {52948L, 27, "0019DA9_"},
                {12362L, 20, "0304A"},
                {31989L, 16, "7CF5"},
                {8503L, 21, "0109BC_"},
                {54308L, 17, "6A124_"},
                {61700L, 25, "0078824_"},
                {63112L, 20, "0F688"},
                {27062L, 29, "00034DB4_"},
                {37994L, 30, "000251AA_"},
                {47973L, 27, "00176CB_"},
                {18996L, 25, "00251A4_"},
                {34043L, 21, "0427DC_"},
                {8234L, 18, "080AA_"},
                {16218L, 26, "000FD6A_"},
                {40697L, 25, "004F7CC_"},
                {43740L, 27, "00155B9_"},
                {35773L, 31, "0001177B_"},
                {32916L, 18, "20252_"},
                {1779L, 24, "0006F3"},
                {35968L, 17, "46404_"},
                {15503L, 23, "00791F_"},
                {25860L, 21, "032824_"},
                {20651L, 29, "0002855C_"},
                {14369L, 16, "3821"},
                {28242L, 24, "006E52"},
                {28446L, 18, "1BC7A_"},
                {48685L, 16, "BE2D"},
                {54822L, 18, "3589A_"},
                {50042L, 22, "030DEA_"},
                {11024L, 30, "0000AC42_"},
                {44958L, 26, "002BE7A_"},
                {20297L, 27, "0009E93_"},
                {24757L, 16, "60B5"},
                {36043L, 29, "0004665C_"},
                {24210L, 16, "5E92"},
                {49621L, 29, "00060EAC_"},
                {63571L, 17, "7C29C_"},
                {16047L, 24, "003EAF"},
                {61384L, 27, "001DF91_"},
                {57607L, 25, "007083C_"},
                {32945L, 30, "000202C6_"},
                {31215L, 29, "0003CF7C_"},
                {3088L, 21, "006084_"},
                {45519L, 24, "00B1CF"},
                {53126L, 26, "0033E1A_"}
        };

        for (Object[] c : cases) {
            long value = (long) c[0];
            int bits = (int) c[1];
            String expected = (String) c[2];

            BitBuilder builder = new BitBuilder();
            builder.writeUint(value, bits);
            BitString bs = builder.build();
            assertEquals(expected, bs.toString(), "value=" + value + " bits=" + bits);
        }
    }

    @Test
    void shouldSerializeInt() {
        Object[][] cases = new Object[][]{
                {-44028L, 22, "FD5012_"},
                {-1613L, 16, "F9B3"},
                {-3640L, 23, "FFE391_"},
                {45943L, 22, "02CDDE_"},
                {-25519L, 22, "FE7146_"},
                {-31775L, 31, "FFFF07C3_"},
                {3609L, 29, "000070CC_"},
                {-38203L, 20, "F6AC5"},
                {59963L, 28, "000EA3B"},
                {-22104L, 21, "FD4D44_"},
                {1305L, 21, "0028CC_"},
                {-40704L, 30, "FFFD8402_"},
                {39319L, 20, "09997"},
                {-39280L, 27, "FFECD21_"},
                {48805L, 21, "05F52C_"},
                {-47386L, 21, "FA3734_"},
                {-24541L, 22, "FE808E_"},
                {-11924L, 30, "FFFF45B2_"},
                {16173L, 22, "00FCB6_"},
                {25833L, 23, "00C9D3_"},
                {27830L, 22, "01B2DA_"},
                {50784L, 31, "00018CC1_"},
                {-41292L, 22, "FD7AD2_"},
                {-8437L, 20, "FDF0B"},
                {-42394L, 19, "EB4CD_"},
                {14663L, 26, "000E51E_"},
                {-52314L, 25, "FF99D34_"},
                {22649L, 31, "0000B0F3_"},
                {-60755L, 19, "E255B_"},
                {-28966L, 17, "C76D4_"},
                {44151L, 20, "0AC77"},
                {22112L, 26, "0015982_"},
                {25524L, 19, "0C769_"},
                {55597L, 23, "01B25B_"},
                {4434L, 28, "0001152"},
                {28364L, 29, "00037664_"},
                {-5431L, 25, "FFF564C_"},
                {35945L, 17, "4634C_"},
                {49508L, 19, "182C9_"},
                {-54454L, 30, "FFFCAD2A_"},
                {-62846L, 22, "FC2A0A_"},
                {-11725L, 28, "FFFD233"},
                {-25980L, 30, "FFFE6A12_"},
                {56226L, 30, "00036E8A_"},
                {64224L, 27, "001F5C1_"},
                {-52385L, 29, "FFF99AFC_"},
                {33146L, 24, "00817A"},
                {-4383L, 27, "FFFDDC3_"},
                {4617L, 23, "002413_"},
                {-20390L, 21, "FD82D4_"}
        };

        for (Object[] c : cases) {
            long value = (long) c[0];
            int bits = (int) c[1];
            String expected = (String) c[2];

            BitBuilder builder = new BitBuilder();
            builder.writeInt(value, bits);
            BitString bs = builder.build();
            assertEquals(expected, bs.toString(), "value=" + value + " bits=" + bits);
        }
    }

    @Test
    void shouldSerializeCoins() {
        String[][] cases = new String[][]{
                {"187657898555727", "6AAAC8261F94F"},
                {"220186135208421", "6C842145FA1E5"},
                {"38303065322130", "622D6209A3292"},
                {"99570315572129", "65A8F054A33A1"},
                {"14785390105803", "60D727DECD4CB"},
                {"244446854605494", "6DE52B7EF6AB6"},
                {"130189848588337", "676682FADB031"},
                {"82548661242881", "64B13DBA14C01"},
                {"248198532456807", "6E1BC395C6167"},
                {"192570661887521", "6AF2459E55E21"},
                {"72100014883174", "6419317C68166"},
                {"216482443674661", "6C4E3BF27C425"},
                {"11259492167296", "60A3D8E07EE80"},
                {"89891460221935", "651C17C8E0BEF"},
                {"267747267722164", "6F383C4C83BB4"},
                {"33545710125130", "61E827822C04A"},
                {"48663481749259", "62C42598B0F0B"},
                {"4122277458487", "603BFCAE23237"},
                {"112985911164954", "666C29519801A"},
                {"262936671139040", "6EF23B6E1B4E0"},
                {"137598454214999", "67D2522FC3157"},
                {"164191836706277", "69554E41A15E5"},
                {"225097218341260", "6CCB987BD398C"},
                {"253225616389304", "6E64EAEE9B4B8"},
                {"89031277771089", "650F935AF7951"},
                {"95175307882302", "6568FBA6AEF3E"},
                {"129805848629999", "6760EC77F52EF"},
                {"144714620593360", "6839DFF8DE4D0"},
                {"245178977211193", "6DEFD2DD7D339"},
                {"85630758278876", "64DE176EDD6DC"},
                {"12826827848685", "60BAA7A847BED"},
                {"112520990974580", "6665655B26274"},
                {"279110697598724", "6FDD985FBBF04"},
                {"213631116095525", "6C24BDEC9B025"},
                {"151538088541111", "689D2B5EFFBB7"},
                {"248258622846989", "6E1CA3706F80D"},
                {"124738812119884", "6717304960B4C"},
                {"20802268076562", "612EB67CC9A12"},
                {"227545530657711", "6CEF392866BAF"},
                {"120231499052120", "66D5993CAB458"},
                {"149349897829611", "687D53B9B7CEB"},
                {"189858289788838", "6ACACD3EBA7A6"},
                {"123762285255173", "6708FA70C9A05"},
                {"70958099290717", "64089384D5A5D"},
                {"124643854909101", "6715CE8B1FEAD"},
                {"7092186021168", "60673473A7D30"},
                {"52349283250349", "62F9C846EB0AD"},
                {"151939404432691", "68A30263A8533"},
                {"31720663732116", "61CD98AE4CF94"},
                {"132368134922315", "678635BA9604B"}
        };

        for (String[] c : cases) {
            BigInteger amount = new BigInteger(c[0]);
            String expected = c[1];

            BitBuilder builder = new BitBuilder();
            builder.writeCoins(amount);
            BitString bs = builder.build();
            assertEquals(expected, bs.toString(), "amount=" + amount);
        }
    }

    @Test
    void shouldSerializeAddress() {
        // This test is ported 1:1 from spec, but depends on Address.parse().
        String[][] cases = new String[][]{
                {"Ef89v3kFhPfyauFSn_PWq-F6HyiBSQDZRXjoDRWq5f5IZeTm", "9FE7B7EF20B09EFE4D5C2A53FE7AD57C2F43E51029201B28AF1D01A2B55CBFC90CB_"},
                {"Ef-zUJX6ySukm-41iSbHW5Ad788NYuWPYKzuAj4vLhe8WSgF", "9FF66A12BF592574937DC6B124D8EB7203BDF9E1AC5CB1EC159DC047C5E5C2F78B3_"},
                {"Ef-x95AVmzKUKkS7isd6XF7YqZf0R0JyOzBO7jir239_feMb", "9FF63EF202B366528548977158EF4B8BDB1532FE88E84E476609DDC7157B6FEFEFB_"},
                {"EQDA1y4uDTy1pdfReyOVD6WWGaAsD7CXg4SgltHS8NzITENs", "80181AE5C5C1A796B4BAFA2F6472A1F4B2C3340581F612F0709412DA3A5E1B99099_"},
                // ... (оставил начало; когда портируем Address — вставим весь список из spec)
        };

        for (String[] c : cases) {
            Address a = Address.parse(c[0]);
            BitBuilder builder = new BitBuilder();
            builder.writeAddress(a);
            assertEquals(c[1], builder.build().toString());
        }
    }
}
