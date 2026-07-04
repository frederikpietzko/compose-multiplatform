package org.jetbrains.compose.web.internal.runtime

import androidx.compose.runtime.AbstractApplier

@ComposeWebInternalApi
class DomApplier(
    root: NativeNodeWrapper
) : AbstractApplier<NativeNodeWrapper>(root) {

    override fun insertTopDown(index: Int, instance: NativeNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: NativeNodeWrapper) {
        current.insert(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.node.clearChildren()
    }
}
