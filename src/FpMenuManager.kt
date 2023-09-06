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
        botTopPanel.gridFormatter.specifyColumnWeight(1, 0.8)
        val quickPanel = Panel(botTopPanel, 0, 0, null)
        val space = "5"
        quickPanel.gridFormatter.setFrame(space, space)
        quickPanel.gridFormatter.setMargin(space, space)
        val fp32 = Button(null, "FP32", quickPanel, 0, 0, font, Color.GREEN)
        addTouchComponent(fp32)
        fp32.clickAction = EventAction {
            exponent!!.message = "8"
            mantissa!!.message = "23"
            refresh()
            true
        }
        val fp16 = Button(null, "FP16", quickPanel, 1, 0, font, Color.GREEN)
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
        val ninf = Button(null, "-inf", valPanel, 1, 0, font, Color.ORANGE)
        addTouchComponent(ninf)
        val nan = Button(null, "nan", valPanel, 2, 0, font, Color.ORANGE)
        addTouchComponent(nan)
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
            override fun emptyText(): String = "0"
        }
        val decBox = addRepresentation(captions, boxes, 2, "Decimal")
        decBox.isEnabled = false
        this.boxes = arrayOf(
            addRepresentation(captions, boxes, 0, "Binary", bin),
            addRepresentation(captions, boxes, 1, "Hexadecimal", hex),
            decBox,
            addRepresentation(captions, boxes, 3, "Pretty", dec))

        fp32.clickAction.onEvent()
    }

    private fun addPrecComps(precPanel: Panel, label: String, offs: Int): TextBox {
        val font = Font("Arial", Font.PLAIN, 18)
        val idx = offs * 3
        Caption("$label:", precPanel, idx, 0, font, Alignment.CENTER_ALIGNMENT)
        val box = TextBox("precision", "0", precPanel, idx + 1, 0, font, Color.LIGHT_GRAY)
        box.acceptEnter(false)
        box.deselectOnEnter(true)
        box.message = "0"
        box.textFormat = object : TextFormat() {
            override fun isValidChar(c: Char): Boolean = c in '0'..'9'
            override fun emptyText(): String = "0"
        }
        addTouchComponent(box)
        precPanel.gridFormatter.specifyColumnWeight(idx + 1, 3.0)
        val updownPanel = Panel(precPanel, idx + 2, 0, null)
        val up = Button(null, "^", updownPanel, 0, 0, font, Color.GRAY)
        up.clickAction = EventAction {
            box.message = (box.message.toInt() + 1).toString()
            refresh()
            true
        }
        addTouchComponent(up)
        val down = Button(null, "v", updownPanel, 0, 1, font, Color.GRAY)
        down.clickAction = EventAction {
            val num = box.message.toInt()
            if (num > 0)
                box.message = (num - 1).toString()
            refresh()
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

    private fun refresh() = refresh(3, true)
    private fun refresh(trigger: Int, setExact: Boolean = false) {
        val msg = boxes!![trigger].message.filterNot { it.isWhitespace() }
        val num = when (trigger) {
            0 -> toVal(msg.toULong(2)) // binary
            1 -> toVal(msg.toULong(16)) // hex
            3 -> msg.toDouble() // parse decimal directly
            else -> 0.0
        }
        // update all fields except exact (if it was the trigger)
        for (i in 0..3) {
            when (i) {
                0 -> {
                    val exponents = this.exponent!!.message.toInt()
                    val mantissas = this.mantissa!!.message.toInt()
                    val binMsg = toBinary(num, exponents, mantissas)
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
                    val binMsg = toBinary(num, this.exponent!!.message.toInt(), this.mantissa!!.message.toInt())
                    val buf = StringBuilder()
                    for (j in binMsg.indices step 4) {
                        val read = binMsg.substring(j until minOf(binMsg.length, j + 4))
                        var hex = 0
                        for (k in read.indices) {
                            if (read[i] == '1')
                                hex += pow2(read.length - (k + 1))
                        }
                       if (j > 0 && j % 16 == 0)
                           buf.append(' ')

                        if (hex < 10)
                            buf.append(hex)
                        else
                            buf.append('A' + (hex - 10))
                    }
                    boxes!![1].message = buf.toString()
                }
                2 -> boxes!![2].message = num.toString()
                3 -> {
                    if (!setExact && trigger == 3)
                        continue
                    boxes!![i].message = num.toString()
                }
            }
        }
    }

    private fun toVal(num: ULong): Double {
        return 0.0
    }

    private fun pow2(pow: Int): Int {
        var res = 1
        for (i in 0 until pow)
            res *= 2
        return res
    }

    private fun toBinary(num : Double, exp: Int, mantissa: Int): String {
        val length = 1 + exp + mantissa
        val buf = StringBuilder(length)
        for (i in 0 until length)
            buf.append('0')
        return buf.toString()
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