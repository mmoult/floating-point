import moulton.scalable.containers.Container
import moulton.scalable.containers.MenuManager
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.KeyboardFocusManager
import java.awt.event.*
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel

fun main() {
    FloatingPoint()
}

class FloatingPoint: JPanel(), Container, MouseListener, KeyListener, MouseMotionListener, MouseWheelListener {
    private val frame: JFrame = JFrame("floating-point")
    private var manager: MenuManager = FpMenuManager(this)

    private var running = false
    private var nextRender : Long = 0
    private val refresh = 250 // 1000 ms = 1 sec. 1000 / 250 = 4 fps

    init {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(this)
        manager.createMenu()
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.minimumSize = preferredSize
        frame.isVisible = true
        frame.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet())

        addMouseListener(this)
        addKeyListener(this)
        addMouseMotionListener(this)
        addMouseWheelListener(this)

        run() //start run loop so the screen can refresh
    }

    private fun run() {
        running = true

        while (running) {
            val currentTime = System.currentTimeMillis()
            if (currentTime >= nextRender) {
                this.repaint()
            } else {
                try {
                    val sleepFor = nextRender - currentTime
                    if (sleepFor > 10)
                        Thread.sleep(sleepFor)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        manager.render(g)
        nextRender = System.currentTimeMillis() + refresh
    }

    override fun getPreferredSize() = Dimension(500, 300)

    override fun getMenuWidth() = width

    override fun getMenuHeight() = height

    override fun setCursor(cursorType: Int) {
        frame.cursor = Cursor.getPredefinedCursor(cursorType)
    }

    override fun mouseClicked(e: MouseEvent?) {}

    override fun mousePressed(e: MouseEvent?) {
        manager.mousePressed(e!!.x, e.y)
        repaint()
    }

    override fun mouseReleased(e: MouseEvent?) {
        manager.mouseReleased(e!!.x, e.y)
        repaint()
    }

    override fun mouseEntered(e: MouseEvent?) {
        requestFocus()
    }

    override fun mouseExited(e: MouseEvent?) {}

    override fun keyTyped(e: KeyEvent?) {
        manager.keyTyped(e!!.keyChar)
        repaint()
    }

    override fun keyPressed(e: KeyEvent?) {
        manager.keyPressed(e!!.extendedKeyCode)
        repaint()
    }

    override fun keyReleased(e: KeyEvent?) {}

    override fun mouseDragged(e: MouseEvent?) {
        manager.mouseMoved(e!!.x, e.y)
        repaint()
    }

    override fun mouseMoved(e: MouseEvent?) {
        manager.mouseMoved(e!!.x, e.y)
        repaint()
    }

    override fun mouseWheelMoved(e: MouseWheelEvent?) {
        manager.mouseScrolled(e!!.x, e.y, e.wheelRotation)
        repaint()
    }
}
