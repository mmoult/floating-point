import moulton.scalable.clickables.Button
import moulton.scalable.clickables.Clickable
import moulton.scalable.clickables.EventAction
import moulton.scalable.containers.MenuManager
import moulton.scalable.containers.Panel
import moulton.scalable.texts.Alignment
import moulton.scalable.texts.Caption
import moulton.scalable.texts.TextBox
import moulton.scalable.texts.TextFormat
import java.awt.Color
import java.awt.Font
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class FpMenuManager(game: FloatingPoint): MenuManager(game) {
    private var boxes : Array<TextBox>? = null
    private var exponent : TextBox? = null
    private var mantissa : TextBox? = null

    override fun createMenu() {
        this.menu = Panel.createRoot(Color.WHITE)
        val font = Font("Arial", Font.PLAIN, 18)
        val topPanel = Panel(menu, "0", "0", "width", "height/3", null)
        val precPanel = Panel(topPanel, 0, 0, Color.YELLOW)
        exponent = addPrecComps(precPanel, "E", 0)
        mantissa = addPrecComps(precPanel, "M", 1)

        val botTopPanel = Panel(topPanel, 0, 1, null)
        val quickPanel = Panel(botTopPanel, 0, 0, null)
        val space = "5"
        quickPanel.gridFormatter.setFrame(space, space)
        quickPanel.gridFormatter.setMargin(space, space)
        val fp32 = Button(null, "FP32", quickPanel, 1, 0, font, Color.GREEN)
        addTouchComponent(fp32)
        fp32.clickAction = EventAction {
            exponent!!.message = "8"
            mantissa!!.message = "23"
            refresh()
            true
        }
        val fp16 = Button(null, "FP16", quickPanel, 0, 0, font, Color.GREEN)
        addTouchComponent(fp16)
        fp16.clickAction = EventAction {
            exponent!!.message = "5"
            mantissa!!.message = "10"
            refresh()
            true
        }
        val fp64 = Button(null, "FP64", quickPanel, 2, 0, font, Color.GREEN)
        addTouchComponent(fp64)
        fp64.clickAction = EventAction {
            exponent!!.message = "11"
            mantissa!!.message = "52"
            refresh()
            true
        }
        val valPanel = Panel(botTopPanel, 1, 0, null)
        val inf = Button(null, "inf", valPanel, 0, 0, font, Color.ORANGE)
        addTouchComponent(inf)
        inf.clickAction = EventAction {
            boxes!![3].message = "inf"
            refresh()
            true
        }
        val ninf = Button(null, "-inf", valPanel, 1, 0, font, Color.ORANGE)
        addTouchComponent(ninf)
        ninf.clickAction = EventAction {
            boxes!![3].message = "-inf"
            refresh()
            true
        }
        val nan = Button(null, "nan", valPanel, 2, 0, font, Color.ORANGE)
        addTouchComponent(nan)
        nan.clickAction = EventAction {
            boxes!![3].message = "nan"
            refresh()
            true
        }
        val zero = Button(null, "0", valPanel, 3, 0, font, Color.ORANGE)
        addTouchComponent(zero)
        zero.clickAction = EventAction {
            boxes!![3].message = "0"
            refresh()
            true
        }

        valPanel.gridFormatter.setFrame(space, space)
        valPanel.gridFormatter.setMargin(space, space)

        val gridPan = Panel(this.menu, "0", "height/3", "width", "?height", null)
        val partition = "120"
        val captions = Panel(gridPan, "0", "0", partition, "?height", Color.WHITE)
        val boxes = Panel(gridPan, partition, "0", "?width", "?height", Color.WHITE)
        val bin = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean = c == '0' || c == '1' || c == ' '
            override fun emptyText(): String = "0"
        }
        val hex = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean =
                (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F') || c == ' '
            override fun emptyText(): String = "0"
        }
        val dec = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean =
                (c in '0'..'9') || c == '-' || c == '.' || c == 'E' || c == 'e'
                        // to allow for inf and nan
                        || c == 'i' || c == 'n' || c == 'f' || c == 'a'
            override fun emptyText(): String = "0"
        }
        val decBox = addRepresentation(captions, boxes, 2, "Decimal")
        decBox.isEnabled = false
        this.boxes = arrayOf(
            addRepresentation(captions, boxes, 0, "Binary", bin),
            addRepresentation(captions, boxes, 1, "Hexadecimal", hex),
            decBox,
            addRepresentation(captions, boxes, 3, "Pretty", dec))

        // start with FP32
        exponent!!.message = "8"
        mantissa!!.message = "23"
        refresh(3, false)
    }

    private fun addPrecComps(precPanel: Panel, label: String, offs: Int): TextBox {
        val font = Font("Arial", Font.PLAIN, 18)
        val idx = offs * 3
        Caption("$label:", precPanel, idx, 0, font, Alignment.CENTER_ALIGNMENT)
        val box = TextBox("precision", "1", precPanel, idx + 1, 0, font, Color.LIGHT_GRAY)
        box.acceptEnter(false)
        box.deselectOnEnter(true)
        box.message = "0"
        box.textFormat = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean = c in '0'..'9'
            override fun emptyText(): String = "1" // precisions must >= 1
        }
        addTouchComponent(box)
        precPanel.gridFormatter.specifyColumnWeight(idx + 1, 3.0)
        val updownPanel = Panel(precPanel, idx + 2, 0, null)
        val up = Button(null, "^", updownPanel, 0, 0, font, Color.GRAY)
        up.clickAction = EventAction {
            box.message = (box.message.toInt() + 1).toString()
            refresh(3)
            true
        }
        addTouchComponent(up)
        val down = Button(null, "v", updownPanel, 0, 1, font, Color.GRAY)
        down.clickAction = EventAction {
            val num = box.message.toInt()
            if (num > 1)
                box.message = (num - 1).toString()
            refresh(3)
            true
        }
        addTouchComponent(down)
        return box
    }

    private fun addRepresentation(captions: Panel, boxes: Panel, i: Int, type: String, tf: TextFormat): TextBox {
        val box = addRepresentation(captions, boxes, i, type)
        box.textFormat = tf
        return box
    }
    private fun addRepresentation(captions: Panel, boxes: Panel, i: Int, type: String): TextBox {
        val font = Font("Arial", Font.PLAIN, 18)
        Caption(" $type:", captions, 0, i, font, Alignment.LEFT_ALIGNMENT)
        val box = TextBox("box$i", "", boxes, 0, i, font, Color.LIGHT_GRAY)
        addTouchComponent(box)
        box.acceptEnter(false)
        box.deselectOnEnter(true)
        box.message = "0"
        return box
    }

    private fun refresh(trigger: Int = 3, setExact: Boolean = false) {
        val msg = boxes!![trigger].message.filterNot { it.isWhitespace() }
        val exponents = this.exponent!!.message.toInt()
        val mantissas = this.mantissa!!.message.toInt()
        val digits = 1 + exponents + mantissas
        // binMsg must have the correct length: pad if necessary
        val binMsg = when (trigger) {
            0 -> padBinary(msg, digits) // fetch the binary directly
            1 -> fromHex(msg, digits) // hex
            3 -> fromDecimal(msg, exponents, mantissas) // exact decimal
            else -> throw RuntimeException("Unhandled trigger type!")
        }
        // update all fields except exact (if it was the trigger)
        val inDec = toDecimal(binMsg, exponents, mantissas)
        for (i in 0..3) {
            when (i) {
                0 -> {
                    // Now we want to split it up to isolate the three components: sign, exponent, mantissa
                    val buf = StringBuilder()
                    buf.append(binMsg[0]) // sign is always 1 bit
                    buf.append(' ')
                    buf.append(binMsg.substring(1..exponents))
                    buf.append(' ')
                    buf.append(binMsg.substring(exponents + 1))
                    boxes!![0].message = buf.toString()
                }
                1 -> {
                    val buf = StringBuilder()
                    for (j in binMsg.indices step 4) {
                        val read = binMsg.substring(j until minOf(binMsg.length, j + 4))
                        var hex = 0
                        var run = 1
                        for (k in read.length - 1 downTo 0) {
                            if (read[k] == '1')
                                hex += run
                            run *= 2
                        }
                       if (j > 0 && j % 16 == 0)
                           buf.append(' ')

                        if (hex < 10)
                            buf.append('0' + hex)
                        else
                            buf.append('A' + (hex - 10))
                    }
                    boxes!![1].message = buf.toString()
                }
                2 -> boxes!![2].message = inDec
                3 -> {
                    if (!setExact && trigger == 3)
                        continue
                    boxes!![3].message = inDec
                }
            }
        }
    }

    private fun padBinary(binNum: String, digits: Int): String {
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

    private fun fromHex(hexNum: String, digits: Int): String {
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

    private fun fromDecimal(decNum: String, nexp: Int, nmant: Int): String {
        val bits = Array(nexp + nmant + 1) { false }
        var dec = decNum

        // the easiest bit is the sign
        if (dec.startsWith('-')) {
            bits[0] = true
            dec = decNum.substring(1)
        }
        var done = false
        if (dec == "inf" || dec == "nan") {
            // Set all exponent bits
            for (i in 1..nexp)
                bits[i] = true

            if (dec == "nan") {
                // Set one of the mantissa bits too.
                // Since it doesn't make any difference which, set the first
                bits[nexp + 1] = true
            }
            done = true
        }

        if (!done) {
            //TODO("non-trivial decimal to binary conv")
        }

        val ret = StringBuilder(bits.size)
        for (bit in bits)
            ret.append(if (bit) '1' else '0')
        return ret.toString()
    }

    private fun toDecimal(binNum: String, nexp: Int, nmant: Int): String {
        var expBits = BigInteger("0")
        var expRun = BigInteger("1")
        val one = BigInteger("1")
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
            for (i in (nexp + 1) until (nexp + nmant)) {
                if (binNum[i] == '1')
                    return "nan" // there is no such thing as -nan, so don't check sign bit
            }
            // If we get here, no mantissa bits were set
            return if (binNum[0] == '0') "inf" else "-inf"
        }
        val expDiff = expRun - one
        val exp = expBits - expDiff
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
        for (i in (nexp + 1) until (nexp + nmant)) {
            if (binNum[i] == '1')
                mantissa += mantRun
            mantRun = mantRun.divide(two, MathContext.UNLIMITED)
        }

        var combo = (expVal * mantissa).toString()
        if (combo.startsWith("0E-"))
            combo = "0"
        if (binNum[0] == '1')
            combo = "-$combo"
        return combo
    }

    override fun clickableAction(c: Clickable?) {}

    override fun lostFocusAction(c: Clickable?) {
        val cc = c!!
        if (cc.id == null)
            return
        if (cc.id.equals("precision"))
            refresh()
        if (cc.id.startsWith("box")) {
            refresh(cc.id.substring(3).toInt())
        }
    }
}