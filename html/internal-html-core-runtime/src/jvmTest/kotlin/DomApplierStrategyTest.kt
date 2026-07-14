/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ComposeWebInternalApi::class)
class DomApplierStrategyTest {
    @Test
    fun delegatesTraversalReconciliationAndClear() {
        val root = JvmRootNode()
        val child = JvmElement("div")
        val calls = mutableListOf<String>()
        val strategy = object : DomApplierStrategy {
            override fun enter(node: NativeNode) { calls += "enter" }
            override fun leave(node: NativeNode) { calls += "leave" }
            override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
                assertSame(root, parent)
                calls += "reconcile:$index"
            }
            override fun clear(root: NativeNode) { calls += "clear" }
            override fun finish() { calls += "finish" }
        }
        val applier = DomApplier(NativeNodeWrapper(root), strategy)
        val wrapper = NativeNodeWrapper(child)

        applier.down(wrapper)
        applier.up()
        applier.insertBottomUp(0, wrapper)
        applier.clear()
        strategy.finish()

        assertEquals(
            listOf("enter", "leave", "reconcile:0", "clear", "finish"),
            calls
        )
    }

    @Test
    fun defaultStrategyPreservesInsertionAndClearing() {
        val root = JvmRootNode()
        val child = JvmElement("div")
        val applier = DomApplier(NativeNodeWrapper(root))

        applier.insertBottomUp(0, NativeNodeWrapper(child))
        assertSame(child, root.children.single())

        applier.clear()
        assertEquals(emptyList(), root.children)
    }
}
