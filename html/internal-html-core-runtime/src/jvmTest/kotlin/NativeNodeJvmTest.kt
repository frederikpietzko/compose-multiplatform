import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeNodeJvmTest {
    @Test
    fun insertsAndMovesChildren() {
        val root: NativeNode = JvmRootNode()
        val a = JvmElement("a"); val b = JvmElement("b"); val c = JvmElement("c")
        root.insertChild(0, a)
        root.insertChild(1, b)
        root.insertChild(2, c)
        root.moveChildren(0, 2, 1) // move a after b
        assertEquals(listOf("b", "a", "c"), (root as JvmRootNode).children.map { (it as JvmElement).tagName })
        root.removeChildren(0, 1)
        assertEquals(listOf("a", "c"), root.children.map { (it as JvmElement).tagName })
    }
}
