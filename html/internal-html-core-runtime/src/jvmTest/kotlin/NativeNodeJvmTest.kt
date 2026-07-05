/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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

    @Test
    fun movesMultipleChildrenForward() {
        val root: NativeNode = JvmRootNode()
        val a = JvmElement("a"); val b = JvmElement("b"); val c = JvmElement("c"); val d = JvmElement("d")
        root.insertChild(0, a)
        root.insertChild(1, b)
        root.insertChild(2, c)
        root.insertChild(3, d)
        root.moveChildren(0, 2, 2) // move [a,b] to position 2
        assertEquals(listOf("c", "a", "b", "d"), root.children.map { (it as JvmElement).tagName })
    }

    @Test
    fun movesMultipleChildrenBackward() {
        val root: NativeNode = JvmRootNode()
        val a = JvmElement("a"); val b = JvmElement("b"); val c = JvmElement("c"); val d = JvmElement("d")
        root.insertChild(0, a)
        root.insertChild(1, b)
        root.insertChild(2, c)
        root.insertChild(3, d)
        root.moveChildren(2, 0, 2) // move [c,d] to position 0
        assertEquals(listOf("c", "d", "a", "b"), root.children.map { (it as JvmElement).tagName })
    }
}
