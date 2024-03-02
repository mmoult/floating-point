import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class NumberHandlerTest {
    private val handler = NumberHandler()

    private class Match(
        val dec: String,
        val bin: String,
        val exponents: Int,
        val mantissas: Int = bin.length - (1 + exponents),
    ) {}

    private val bidirectional = arrayOf( // exact matches in both directions
        Match("0", "0000000000000000", 5),
        Match("0", "00000000000000000000000000000000", 8),
        Match("-0", "1000000000000000", 5),
        Match("-0", "10000000000000000000000000000000", 8),

        Match("1","0011110000000000", 5),
        Match("1","00111111100000000000000000000000", 8),
        Match("2","0100000000000000", 5),
        Match("2","01000000000000000000000000000000", 8),

        Match("0.000000059604644775390625", "0000000000000001", 5),
        Match("0.000030517578125", "0000001000000000", 5),
        Match("-65504", "1111101111111111", 5),
        Match("0.0999755859375", "0010111001100110", 5),
        Match("0.125", "0011000000000000", 5),
        Match("-6259853398707798016", "11011110101011011011111011101111", 8), // 0xDEADBEEF

        Match("inf", "0111110000000000", 5),
        Match("inf", "01111111100000000000000000000000", 8),
        Match("-inf", "1111110000000000", 5),
        Match("-inf", "11111111100000000000000000000000", 8),
    )

    @Test
    fun fromDecimal() {
        // do the bidirectional tests
        for (match in bidirectional)
            assertEquals(match.bin, handler.fromDecimal(match.dec, match.exponents, match.mantissas))

        // There are plenty of one-directional tests since not every decimal can be evenly mapped onto the float config
        assertEquals("00111101110011001100110011001101", // demonstrates rounding of subnormal
            handler.fromDecimal("0.1", 8, 23))
        assertEquals("01001110100100110010110000000110", // reach precision limit of large mantissa
            handler.fromDecimal("1234567890", 8, 23))
        assertEquals("01001011001111000110000101010000", // mantissa limit at edge (i == 0)
            handler.fromDecimal("12345679.5", 8, 23))
        assertEquals("0111110000000000", // too big, rounded to inf
            handler.fromDecimal("70000", 5, 10))
    }

    @Test
    fun toDecimal() {
        // do the bidirectional tests
        for (match in bidirectional)
            assertEquals(match.dec, handler.toDecimal(match.bin, match.exponents))

        // Test a few values which are one directional or not necessarily reversible
        assertEquals("nan", handler.toDecimal("0111110100000000", 5))
        assertEquals("nan", handler.toDecimal("1111110000000001", 5)) // no such -nan
        assertEquals("nan", handler.toDecimal("01111111100000100000000000010000", 8))
    }

    @Test
    fun increment() {
        // TODO
    }
}
