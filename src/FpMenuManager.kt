import moulton.scalable.clickables.Button
import moulton.scalable.clickables.EventAction
import moulton.scalable.containers.MenuManager
import moulton.scalable.containers.Panel
import moulton.scalable.texts.Alignment
import moulton.scalable.texts.Caption
import moulton.scalable.texts.StaticTextBox
import moulton.scalable.texts.TextBox
import moulton.scalable.texts.TextFormat
import java.awt.Color
import java.awt.Font

class FpMenuManager(game: FloatingPoint): MenuManager(game) {
    private var boxes : Array<TextBox>? = null
    private var exponent : TextBox? = null
    private var mantissa : TextBox? = null

    override fun createMenu() {
        this.menu = Panel.createRoot(Color.WHITE)
        val font = Font("Arial", Font.PLAIN, 18)
        val topPanel = Panel(menu, 0, 0, null)
        // panel for holding precision controls
        val precPanel = Panel(topPanel, 0, 0, Color.YELLOW)
        exponent = addPrecComps(precPanel, "E", 0)
        mantissa = addPrecComps(precPanel, "M", 1)

        val buttonPanel = Panel(topPanel, 0, 1, null)
        topPanel.gridFormatter.specifyRowWeight(1, 2.0)
        val leftButtonsPanel = Panel(buttonPanel, 0, 0, null)
        val space = "5"
        leftButtonsPanel.gridFormatter.setFrame(space, space)
        leftButtonsPanel.gridFormatter.setMargin(space, space)
        val fp32 = Button("FP32", leftButtonsPanel, 1, 0, font, Color.GREEN)
        addTouchComponent(fp32)
        fp32.clickAction = EventAction {
            exponent!!.message = "8"
            mantissa!!.message = "23"
            refresh()
            true
        }
        val fp16 = Button("FP16", leftButtonsPanel, 0, 0, font, Color.GREEN)
        addTouchComponent(fp16)
        fp16.clickAction = EventAction {
            exponent!!.message = "5"
            mantissa!!.message = "10"
            refresh()
            true
        }
        val fp64 = Button("FP64", leftButtonsPanel, 2, 0, font, Color.GREEN)
        addTouchComponent(fp64)
        fp64.clickAction = EventAction {
            exponent!!.message = "11"
            mantissa!!.message = "52"
            refresh()
            true
        }
        val handler = NumberHandler()
        val negate = Button("-/+", leftButtonsPanel, 0, 1, font, Color.PINK)
        addTouchComponent(negate)
        negate.clickAction = EventAction {
            val prev = boxes!![3].message
            setBox(3, if (prev.startsWith("-"))
                prev.substring(1)
            else
                "-$prev")
            true
        }
        val addOne = Button("+1", leftButtonsPanel, 1, 1, font, Color.PINK)
        addTouchComponent(addOne)
        addOne.clickAction = EventAction {
            setBox(0, handler.increment(boxes!![0].message, exponent!!.message.toInt(), true))
            true
        }
        val subOne = Button("-1", leftButtonsPanel, 2, 1, font, Color.PINK)
        addTouchComponent(subOne)
        subOne.clickAction = EventAction {
            setBox(0, handler.increment(boxes!![0].message, exponent!!.message.toInt(), false))
            true
        }

        // Panel for quick select of useful values
        val rightButtonsPanel = Panel(buttonPanel, 1, 0, null)
        val inf = Button("inf", rightButtonsPanel, 0, 0, font, Color.ORANGE)
        addTouchComponent(inf)
        inf.clickAction = EventAction {
            setBox(3, "inf")
            true
        }
        val ninf = Button("-inf", rightButtonsPanel, 1, 0, font, Color.ORANGE)
        addTouchComponent(ninf)
        ninf.clickAction = EventAction {
            setBox(3, "-inf")
            true
        }
        val nan = Button("nan", rightButtonsPanel, 2, 0, font, Color.ORANGE)
        addTouchComponent(nan)
        nan.clickAction = EventAction {
            setBox(3, "nan")
            true
        }
        val zero = Button("0", rightButtonsPanel, 3, 0, font, Color.ORANGE)
        addTouchComponent(zero)
        zero.clickAction = EventAction {
            setBox(3, "0")
            true
        }
        // The largest value representable by the float
        val max = Button("max", rightButtonsPanel, 0, 1, font, Color.ORANGE)
        addTouchComponent(max)
        max.clickAction = EventAction {
            setBox(0, handler.max(exponent!!.message.toInt(), mantissa!!.message.toInt()))
            true
        }
        // The lowest point where ints can be exactly represented
        val integer = Button("int", rightButtonsPanel, 1, 1, font, Color.ORANGE)
        addTouchComponent(integer)
        integer.clickAction = EventAction {
            setBox(3, handler.intHigh(mantissa!!.message.toInt()))
            true
        }
        // The lowest point, above which, no deltas below 1 can be represented
        val decb = Button("dec", rightButtonsPanel, 2, 1, font, Color.ORANGE)
        addTouchComponent(decb)
        decb.clickAction = EventAction {
            setBox(3, handler.decHigh(mantissa!!.message.toInt()))
            true
        }
        // The lowest denorm value
        val low = Button("low", rightButtonsPanel, 3, 1, font, Color.ORANGE)
        addTouchComponent(low)
        low.clickAction = EventAction {
            setBox(0, handler.denormLow(exponent!!.message.toInt(), mantissa!!.message.toInt()))
            true
        }

        rightButtonsPanel.gridFormatter.setFrame(space, space)
        rightButtonsPanel.gridFormatter.setMargin(space, space)

        val gridPan = Panel(this.menu, 0, 1, null)
        this.menu.gridFormatter.specifyRowWeight(1, 1.5)
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
            var box : TextBox? = null

            override fun isValidChar(c: Char): Boolean {
                if (c in '0' .. '9')
                    return true
                var msg = box!!.message
                if (c == '.') {
                    // can only see one . in the whole message
                    for (cc in msg) {
                        if (cc == '.')
                            return false
                    }
                    return true
                }
                // - must be first, before anything
                var first = msg.isEmpty()
                if (first && c == '-')
                    return true
                if (msg.startsWith("-")) {
                    first = msg.length == 1 // ignore leading negative, doesn't affect first-ness
                    msg = msg.substring(1)
                }
                if (first) {
                    // can see ., i, n here
                    return c == 'i' || c == 'n'
                }else if (msg == "i" && c == 'n')
                    return true
                else if (msg == "in" && c == 'f')
                    return true
                else if (msg == "n" && c == 'a')
                    return true
                else if (msg == "na" && c == 'n')
                    return true
                return false
            }
            override fun emptyText(): String = "0"
        }
        val font2 = addRepresentation(captions, 2, "Decimal")
        this.boxes = arrayOf(
            addRepresentation(captions, boxes, 0, "Binary", bin),
            addRepresentation(captions, boxes, 1, "Hexadecimal", hex),
            StaticTextBox("0", boxes, 0, 2, font2, Color.WHITE),
            addRepresentation(captions, boxes, 3, "Pretty", dec))
        dec.box = this.boxes!![3]

        // start with FP32
        exponent!!.message = "8"
        mantissa!!.message = "23"
        refresh(3, false)
    }

    private fun addPrecComps(precPanel: Panel, label: String, offs: Int): TextBox {
        val font = Font("Arial", Font.PLAIN, 18)
        val idx = offs * 3
        Caption("$label:", precPanel, idx, 0, font, Alignment.CENTER_ALIGNMENT)
        val box = TextBox("1", precPanel, idx + 1, 0, font, Color.LIGHT_GRAY)
        box.acceptEnter = false
        box.deselectOnEnter = true
        box.message = "0"
        box.textFormat = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean = c in '0'..'9'
            override fun emptyText(): String = "1" // precisions must >= 1
        }
        box.lostFocusAction = EventAction {
            refresh()
            true
        }
        addTouchComponent(box)
        precPanel.gridFormatter.specifyColumnWeight(idx + 1, 3.0)
        val updownPanel = Panel(precPanel, idx + 2, 0, null)
        val up = Button("^", updownPanel, 0, 0, font, Color.GRAY)
        up.clickAction = EventAction {
            box.message = (box.message.toInt() + 1).toString()
            refresh(3)
            true
        }
        addTouchComponent(up)
        val down = Button("v", updownPanel, 0, 1, font, Color.GRAY)
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
        val font = addRepresentation(captions, i, type)
        val box = TextBox("0", boxes, 0, i, font, Color.LIGHT_GRAY)
        box.lostFocusAction = EventAction {
            refresh(i)
            true
        }
        addTouchComponent(box)
        box.acceptEnter = false
        box.deselectOnEnter = true
        box.textFormat = tf
        return box
    }
    private fun addRepresentation(captions: Panel, i: Int, type: String): Font {
        val font = Font("Arial", Font.PLAIN, 18)
        Caption(" $type:", captions, 0, i, font, Alignment.LEFT_ALIGNMENT)
        return font
    }

    private fun setBox(which: Int, msg: String) {
        boxes!![which].message = msg
        refresh(which)
    }

    private fun refresh(trigger: Int = 3, setExact: Boolean = false) {
        val msg = boxes!![trigger].message.filterNot { it.isWhitespace() }
        val exponents = this.exponent!!.message.toInt()
        val mantissas = this.mantissa!!.message.toInt()
        val digits = 1 + exponents + mantissas
        val handler = NumberHandler()
        // binMsg must have the correct length: pad if necessary
        val binMsg = when (trigger) {
            0 -> handler.padBinary(msg, digits) // fetch the binary directly
            1 -> handler.fromHex(msg, digits) // hex
            3 -> handler.fromDecimal(msg, exponents, mantissas) // exact decimal
            else -> throw RuntimeException("Unhandled trigger type!")
        }
        // update all fields except exact (if it was the trigger)
        val inDec = handler.toDecimal(binMsg, exponents, mantissas)
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
}