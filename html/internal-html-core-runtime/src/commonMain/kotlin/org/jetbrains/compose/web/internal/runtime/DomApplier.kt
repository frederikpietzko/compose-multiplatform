/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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
