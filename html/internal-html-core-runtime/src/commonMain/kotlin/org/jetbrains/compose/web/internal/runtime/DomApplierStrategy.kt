/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

@ComposeWebInternalApi
interface DomApplierStrategy {
    fun enter(node: NativeNode)
    fun leave(node: NativeNode)
    fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode)
    fun clear(root: NativeNode)
    fun finish()
}

@ComposeWebInternalApi
object DefaultDomApplierStrategy : DomApplierStrategy {
    override fun enter(node: NativeNode) = Unit
    override fun leave(node: NativeNode) = Unit

    override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
        parent.insertChild(index, child)
    }

    override fun clear(root: NativeNode) {
        root.clearChildren()
    }

    override fun finish() = Unit
}
