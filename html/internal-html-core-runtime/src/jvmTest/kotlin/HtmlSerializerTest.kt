import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlSerializerTest {
    @Test
    fun serializesElementWithAttrsClassesStyleAndText() {
        val root = JvmRootNode()
        val div = JvmElement("div").apply {
            classes += listOf("card", "big")
            styles["color"] = "red"
            attributes["id"] = "main"
        }
        val text = JvmTextNode("hi & <bye>")
        (div as NativeNode).children.add(text)
        (root as NativeNode).children.add(div)
        assertEquals(
            """<div class="card big" style="color:red;" id="main">hi &amp; &lt;bye&gt;</div>""",
            serializeToHtml(root)
        )
    }

    @Test
    fun voidElementHasNoClosingTag() {
        val root = JvmRootNode()
        (root as NativeNode).children.add(JvmElement("br"))
        assertEquals("<br>", serializeToHtml(root))
    }

    @Test
    fun serializesNestedElements() {
        val root = JvmRootNode()
        val outer = JvmElement("ul")
        val li1 = JvmElement("li")
        (li1 as NativeNode).children.add(JvmTextNode("one"))
        val li2 = JvmElement("li")
        (li2 as NativeNode).children.add(JvmTextNode("two"))
        (outer as NativeNode).children.add(li1)
        (outer as NativeNode).children.add(li2)
        (root as NativeNode).children.add(outer)
        assertEquals(
            "<ul><li>one</li><li>two</li></ul>",
            serializeToHtml(root)
        )
    }

    @Test
    fun serializesMultipleAttributesInInsertionOrder() {
        val root = JvmRootNode()
        val input = JvmElement("input").apply {
            attributes["type"] = "text"
            attributes["name"] = "q"
            attributes["placeholder"] = "he said \"hi\" & <bye>"
        }
        (root as NativeNode).children.add(input)
        assertEquals(
            """<input type="text" name="q" placeholder="he said &quot;hi&quot; &amp; &lt;bye&gt;">""",
            serializeToHtml(root)
        )
    }
}
