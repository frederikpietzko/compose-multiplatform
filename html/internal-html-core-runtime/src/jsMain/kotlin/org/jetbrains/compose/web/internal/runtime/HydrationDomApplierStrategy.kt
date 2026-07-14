/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ComposeWebInternalApi::class)

package org.jetbrains.compose.web.internal.runtime

internal class HydrationDomApplierStrategy(
    private val session: HydrationSession
) : DomApplierStrategy {
    private var hydrating = true

    override fun enter(node: NativeNode) {
        if (hydrating) session.enter(node)
    }

    override fun leave(node: NativeNode) {
        if (hydrating) session.leave(node)
    }

    override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
        if (hydrating) session.verifyChild(parent, index, child)
        else DefaultDomApplierStrategy.reconcileChild(parent, index, child)
    }

    override fun clear(root: NativeNode) {
        if (!(hydrating && session.aborted)) {
            DefaultDomApplierStrategy.clear(root)
        }
    }

    override fun finish() {
        session.finish()
        hydrating = false
    }
}
