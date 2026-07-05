/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebInternalApi::class)
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

    @Test
    fun styleElementTextIsSerializedVerbatimAsRawText() {
        val root = JvmRootNode()
        val style = JvmElement("style")
        (style as NativeNode).children.add(JvmTextNode("div > p { color: red; }"))
        (root as NativeNode).children.add(style)
        assertEquals(
            "<style>div > p { color: red; }</style>",
            serializeToHtml(root)
        )
    }

    @Test
    fun scriptElementTextIsSerializedVerbatimAsRawText() {
        val root = JvmRootNode()
        val script = JvmElement("script")
        (script as NativeNode).children.add(JvmTextNode("if (a < b) {}"))
        (root as NativeNode).children.add(script)
        assertEquals(
            "<script>if (a < b) {}</script>",
            serializeToHtml(root)
        )
    }

    @Test
    fun normalElementTextIsStillEscaped() {
        val root = JvmRootNode()
        val div = JvmElement("div")
        (div as NativeNode).children.add(JvmTextNode("a > b"))
        (root as NativeNode).children.add(div)
        assertEquals(
            "<div>a &gt; b</div>",
            serializeToHtml(root)
        )
    }
}
