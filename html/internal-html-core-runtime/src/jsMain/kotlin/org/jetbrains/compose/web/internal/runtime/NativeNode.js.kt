/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.dom.clear
import org.w3c.dom.Node
import org.w3c.dom.get

actual typealias NativeNode = Node

actual fun NativeNode.insertChild(index: Int, child: NativeNode) {
    val length = childNodes.length
    if (index < length) {
        insertBefore(child, childNodes[index]!!)
    } else {
        appendChild(child)
    }
}

actual fun NativeNode.removeChildren(index: Int, count: Int) {
    repeat(count) { removeChild(childNodes[index]!!) }
}

actual fun NativeNode.moveChildren(from: Int, to: Int, count: Int) {
    if (from == to) return
    for (i in 0 until count) {
        val fromIndex = if (from > to) from + i else from
        val toIndex = if (from > to) to + i else to + count - 2
        val child = removeChild(childNodes[fromIndex]!!)
        insertBefore(child, childNodes[toIndex]!!)
    }
}

actual fun NativeNode.clearChildren() {
    clear()
}
