import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.min

class NumberHandler {
    fun padBinary(binNum: String, digits: Int): String {
        val diff = digits - binNum.length
        if (diff == 0)
            return binNum
        if (diff < 0)
            return binNum.substring(0, digits)
        val ret = StringBuilder(digits)
        for (i in 0 until diff)
            ret.append('0')
        ret.append(binNum)
        return ret.toString()
    }

    /**
     * Produces a binary string from the given hex string.
     * @param hexNum the input hexadecimal to generate binary for
     * @param digits the number of expected bits. If the hexadecimal string is too short, the binary will be padded with
     * zeros. If the hexadecimal string is too long, it will be truncated.
     * @return the equivalent binary from the input hex
     */
    fun fromHex(hexNum: String, digits: Int): String {
        val bin = StringBuilder(digits)
        val hex = if (hexNum.length * 4 > digits)
            hexNum.substring(0, digits / 4)
        else
            hexNum
        for (c in hex) {
            val cc = if (c <= 'Z') c else (c - ('a' - 'A'))
            bin.append(when (cc) {
                '0' -> "0000"
                '1' -> "0001"
                '2' -> "0010"
                '3' -> "0011"
                '4' -> "0100"
                '5' -> "0101"
                '6' -> "0110"
                '7' -> "0111"
                '8' -> "1000"
                '9' -> "1001"
                'A' -> "1010"
                'B' -> "1011"
                'C' -> "1100"
                'D' -> "1101"
                'E' -> "1110"
                'F' -> "1111"
                else -> throw RuntimeException("Invalid char found in hex string!")
            })
        }
        return padBinary(bin.toString(), digits)
    }

    /**
     * Produces a binary string equivalent to the input decimal number (given as a string).
     * @param decNum the decimal number to convert into float binary
     * @param nexp the number of exponent bits given by the floating point configuration
     * @param nmant the number of mantissa bits given by the floating point configuration
     * @return the equivalent string in binary. It is assumed that one bit is allowed for the sign and that the three
     * components (sign, exponent, mantissa), are in that order.
     */
    fun fromDecimal(decNum: String, nexp: Int, nmant: Int): String {
        val bits = Array(nexp + nmant + 1) { false }
        var dec = decNum

        // the easiest bit is the sign
        if (dec.startsWith('-')) {
            bits[0] = true
            dec = decNum.substring(1)
        }
        var done = 0
        if (dec == "inf" || dec == "nan") {
            if (dec == "nan") {
                // Set one of the mantissa bits.
                // Since it doesn't make any difference which, set the first
                bits[nexp + 1] = true
            }
            done = 2
        }else if (dec.isEmpty() || dec.contains('i') || dec.contains('n')
            || dec.contains('f') || dec.contains('a'))
            dec = "0" // error turns to 0

        process@while (done == 0) {
            // Convert the whole number to binary
            var decc = BigDecimal(dec)
            val zero = BigDecimal(0)
            // If our decimal number is 0, we are done. 0 = all off
            if (decc.compareTo(zero) != 0) {
                val one = BigDecimal(1)
                val two = BigDecimal(2)
                var run = one
                val cache = ArrayList<BigDecimal>(nexp + 1)
                cache.add(one)
                while (decc >= run) {
                    run *= two
                    cache.add(run)
                }
                val bin = ArrayList<Boolean>()
                var denormal = true
                var earlyBreak = 0
                // No entry at size. Skip size-1 since it is larger than the input.
                // Therefore, start with size-2
                for (i in cache.size - 2 downTo 0) {
                    if (bin.size > nmant) { // don't go further than our precision allows
                        earlyBreak = i + 1
                        break
                    }else if (decc >= cache[i]) {
                        bin.add(true)
                        decc -= cache[i]
                        denormal = false
                    }else
                        bin.add(false)
                }
                // The decimal is at bin.size, but we need to move it to scientific notation,
                // which leaves just a 1 (or a 0 for denormal mode) before it
                var decMove = bin.size + earlyBreak - 1
                if (decc > zero) {
                    // Continue where possible to refine the current mantissa value
                    if (earlyBreak == 0) {
                        // If we did not break early, we may have more precision bits to use. Thus, go into the decimal
                        run = one
                        while (decc > zero) {
                            run = run.divide(two, MathContext.UNLIMITED)
                            // Don't need to go further than our precision will allow
                            if (bin.size > nmant)
                                break

                            if (decc >= run) {
                                decc -= run
                                bin.add(true)
                                denormal = false // no longer counting decimal moves
                            } else {
                                if (denormal)
                                    decMove--
                                else
                                    bin.add(false)
                            }
                        }
                    }else
                        run = cache[earlyBreak - 1]

                    // If we still aren't exactly there, try rounding the bits we currently have
                    if (decc > zero && decc >= run) {
                        for (i in bin.size - 1 downTo 0) {
                            // look for a 0
                            if (!bin[i]) {
                                // Flip this bit and set all after to !add
                                bin[i] = true
                                for (j in i + 1 until bin.size)
                                    bin[j] = false
                                break
                            }
                        }
                        // If all the bin bits were true, we cannot round (next num is inf)
                    }
                }

                // Now we need to create the exponent:
                // (2 ^ nexp) - 1 + decMove = exponent
                // If exponent >= 2 ^ (nexp + 1) - 1, we round to infinity
                // (Recall we cannot have all exponent bits on since that is inf or nan.)

                if (decMove >= 0) { // if not denormal
                    // We kept a cache of powers of two earlier. Use it to fetch the values needed now
                    run = cache[cache.size - 1]
                    // extend the cache as needed
                    while (cache.size < nexp + 1) {
                        run *= two
                        cache.add(run)
                    }
                    val expMax = cache[nexp] - one
                    var exp = (cache[nexp - 1] - one) + BigDecimal(decMove)
                    if (exp >= expMax) {
                        done = 2 // round up to infinity
                        break@process
                    } else {
                        // Convert exp into binary while translating value into bits array
                        for (i in nexp - 1 downTo 0) {
                            if (exp >= cache[i]) {
                                exp -= cache[i]
                                bits[nexp - i] = true
                            }
                        }
                    }
                } else {

                }
                // finally, set the mantissa, which is a straight copy across from bin (except leading 1)
                for (i in 1 until min(bin.size, nmant + 1))
                    bits[nexp + i] = bin[i]
            }
            done = 1
        }

        if (done == 2) {
            // Set all exponent bits
            for (i in 1..nexp)
                bits[i] = true
        }

        val ret = StringBuilder(bits.size)
        for (bit in bits)
            ret.append(if (bit) '1' else '0')
        return ret.toString()
    }

    /**
     * Produces a decimal value (as a string) representing the given binary, with the given number of exponent and
     * mantissa bits (and the assumed 1 sign bit)
     * @param binNum the number in binary to translate into decimal
     * @param nexp the number of exponent bits in the floating point configuration. (The number of mantissa bits is
     * deduced since there must be 1 sign bit and all other bits must be mantissa.)
     * @param the decimal value (as a string) equivalent to the input binary float
     */
    fun toDecimal(binNum: String, nexp: Int): String {
        val nmant = binNum.length - (1 + nexp)

        var expBits = BigInteger("0")
        var expRun = BigInteger("1")
        val itwo = BigInteger("2")
        var special = true
        var denormal = true
        for (i in nexp downTo 1) {
            if (binNum[i] == '1') {
                expBits += expRun
                denormal = false
            }else
                special = false
            if (i > 1) // do not double on last iteration
                expRun *= itwo
        }
        if (special) {
            // All exponent bits set signals special
            // If any of the mantissa bits are set, this is nan, else, inf or -inf
            for (i in (nexp + 1) until (nexp + nmant + 1)) {
                if (binNum[i] == '1')
                    return "nan" // there is no such thing as -nan, so don't check sign bit
            }
            // If we get here, no mantissa bits were set
            return if (binNum[0] == '0') "inf" else "-inf"
        }
        val one = BigInteger("1")
        val expDiff = expRun - one  // expDiff = 2^nexp - 1
        var exp = expBits - expDiff
        // If the number is a denormal, the exp is actually one higher
        if (denormal) // In other words, the min exp is 1 - expDiff (shared by 0 and 1)
            exp += one
        // expVal = 2 ^ exp
        var expVal = BigDecimal(1)
        var count = BigInteger("0")
        val two = BigDecimal(2)
        if (exp >= count) {
            while (exp > count) {
                expVal *= two
                count += one
            }
        }else {
            while (exp < count) {
                expVal = expVal.divide(two, MathContext.UNLIMITED)
                count -= one
            }
        }

        var mantissa = if (denormal) BigDecimal(0) else BigDecimal(1)
        var mantRun = BigDecimal("0.5")
        for (i in (nexp + 1) .. (nexp + nmant)) {
            if (binNum[i] == '1')
                mantissa += mantRun
            mantRun = mantRun.divide(two, MathContext.UNLIMITED)
        }

        // BigDecimal will print to string for us, using plain avoids scientific notation
        var combo = (expVal * mantissa).toPlainString()
        // Clean up some of its weird output:
        // remove trailing 0's, if any
        if (combo.indexOf('.') != -1) {
            var trail = combo.length - 1
            for (i in trail downTo 0) {
                trail = i
                if (combo[i] == '.') {
                    trail = i - 1 // delete the trailing . then quit
                    break
                }
                if (combo[i] != '0')
                    break
            }
            combo = combo.substring(0, trail + 1)
        }
        if (binNum[0] == '1')
            combo = "-$combo"
        return combo
    }

    /**
     * The smallest denormal value
     */
    fun denormLow(exponents: Int, mantissas: Int): String {
        val num = StringBuilder(1 + exponents + mantissas)
        num.append('0')
        for (i in 0 until exponents)
            num.append('0')
        for (i in 0 until mantissas - 1)
            num.append('0')
        num.append('1')
        return num.toString()
    }

    /**
     * The largest denormal value
     */
    fun denormHigh(exponents: Int, mantissas: Int): String {
        val num = StringBuilder(1 + exponents + mantissas)
        num.append('0')
        for (i in 0 until exponents)
            num.append('0')
        for (i in 0 until mantissas)
            num.append('1')
        return num.toString()
    }

    /**
     * The smallest (positive) non-denormal value
     */
    fun low(exponents: Int, mantissas: Int): String {
        val num = StringBuilder(1 + exponents + mantissas)
        num.append('0')
        for (i in 0 until exponents - 1)
            num.append('0')
        num.append('1') // this will be inf if there is only 1 exponent bit
        for (i in 0 until mantissas)
            num.append('0')
        return num.toString()
    }

    /**
     * The largest value representable by the float precision settings
     */
    fun max(exponents: Int, mantissas: Int): String {
        val num = StringBuilder(1 + exponents + mantissas)
        num.append('0')
        for (i in 0 until exponents - 1)
            num.append('1')
        num.append('0')
        for (i in 0 until mantissas)
            num.append('1')
        return num.toString()
    }

    /**
     * Return the lowest point, above which, there is no subnormal representation.
     * In other words, for FP16, you can represent 1023.5, but not 1024.5, since there are 10 exponent bits,
     * and 2^10 = 1024
     */
    fun decHigh(mantissas: Int): String {
        val two = BigInteger.valueOf(2)
        return two.pow(mantissas).toString()
    }

    /**
     * Return the lowest point, above which, there is not guaranteed representation for each integer
     */
    fun intHigh(mantissas: Int): String {
        val two = BigInteger.valueOf(2)
        return two.pow(mantissas + 1).toString()
    }

    /**
     * Returns the binary float which is one unit of least precision different from the binary float source. In some
     * special cases (such as inf or nan), the return should be the same as the source.
     * Note that this is not a simple binary addition/subtraction because NaN cannot be added or subtracted from, inf
     * and -inf are the max and min, respectively, and there is a jump in the binary representation between 0 and -0
     * (which in this context count as 1 ULP difference).
     * @param binSrc the binary float to increment/decrement
     * @param exponents the number of exponent bits in the float configuration. There must be 1 leading sign bit, and
     * the number of mantissa bits is assumed from the length of the binary source
     * @param direction the direction to increment. If direction == true, +1. If direction == false, -1.
     */
    fun increment(binSrc: String, exponents: Int, direction: Boolean): String {
        val bin = binSrc.filter {it != ' '}
        val bits = BooleanArray(bin.length) {bin[it] == '1'}

        /* Scale of representable float numbers (direction from top to bottom):
        -inf  = 1 1..1 0..0
        -max  = 1 1..10 1..1
        ..
        -d_lo = 1 0..0 0..01
        -0    = 1 0..0 0..0
        +0    = 0 0..0 0..0
        +d_lo = 0 0..0 0..1
        ..
        +max  = 0 1..10 1..1
        +inf  = 0 1..1 0..0
        (NaN is not included on the number line since it isn't a number.)

        Positive increment:
        - [-inf, -0] -> subtract one
        - -0 -> go to 0
        - [0, +inf) -> add one
        - +inf -> do nothing
        Negative increment: (the inverse sign of above)
        */

        val neg = bits[0]
        val add = (neg != direction)

        // First, check for inf (for add) and 0 (for sub)
        val stop = if (add) (exponents + 1) else bits.size
        var special = true
        for (i in 1 until stop) {
            if (bits[i] != add) {
                // For add, at least one of the bits is off = no inf
                // For sub, at least one of the bits is on  = no zero
                special = false
                break
            }
        }

        if (special) {
            if (!add) {
                // 0 <-> -0
                bits[0] = !neg
            }
            // Else, add on infinity, which should be ignored
        }else {
            // Regular case to either add or subtract (without worry of rollover)
            // do not allow modification of the 0th/sign bit (prefix size = 1)
            for (i in bits.size - 1 downTo 1) {
                // If add, look for a 0
                // If sub, look for a 1
                if (bits[i] != add) {
                    // Flip this bit and set all after to !add
                    bits[i] = !bits[i]
                    for (j in i + 1 until bits.size)
                        bits[j] = !add
                    break
                }
            }
        }

        // Create a new string from the mutable bits
        val str = StringBuilder(bits.size)
        for (i in bits)
            str.append(if (i) '1' else '0')
        return str.toString()
    }
}