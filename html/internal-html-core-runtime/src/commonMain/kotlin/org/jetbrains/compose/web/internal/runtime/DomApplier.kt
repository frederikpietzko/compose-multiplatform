/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import androidx.compose.runtime.AbstractApplier

@ComposeWebInternalApi
class DomApplier(
    root: NativeNodeWrapper,
    private val strategy: DomApplierStrategy = DefaultDomApplierStrategy
) : AbstractApplier<NativeNodeWrapper>(root) {

    override fun down(node: NativeNodeWrapper) {
        strategy.enter(node.node)
        super.down(node)
    }

    override fun up() {
        strategy.leave(current.node)
        super.up()
    }

    override fun insertTopDown(index: Int, instance: NativeNodeWrapper) = Unit

    override fun insertBottomUp(index: Int, instance: NativeNodeWrapper) {
        strategy.reconcileChild(current.node, index, instance.node)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        strategy.clear(root.node)
    }
}
