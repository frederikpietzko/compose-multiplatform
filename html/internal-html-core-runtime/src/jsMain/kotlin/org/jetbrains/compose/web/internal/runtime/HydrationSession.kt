/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ComposeWebInternalApi::class)

package org.jetbrains.compose.web.internal.runtime

import org.jetbrains.compose.web.HydrationMismatchException
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.events.EventTarget

private const val HTML_NAMESPACE = "http://www.w3.org/1999/xhtml"

internal class HydrationSession(root: Node) {
    private data class Frame(
        val node: Node,
        val path: String,
        var nextChild: Int = 0
    )

    private val frames = mutableListOf(Frame(root, "root"))
    private val paths = mutableMapOf<Node, String>()
    private val listeners = mutableMapOf<EventTarget, List<NamedEventListener>>()
    var aborted: Boolean = false
        private set

    private val current: Frame get() = frames.last()

    fun claimElement(tagName: String): Element {
        val index = current.nextChild++
        val path = "${current.path}/${tagName.lowercase()}[$index]"
        val actual = current.node.childNodes.item(index)
            ?: mismatch(path, "expected <$tagName>, found no node")
        if (actual.nodeType.toInt() != 1) {
            mismatch(path, "expected <$tagName>, found ${describe(actual)}")
        }
        val element = actual as Element
        if (element.namespaceURI != HTML_NAMESPACE || element.localName != tagName.lowercase()) {
            mismatch(path, "expected <$tagName>, found ${describe(element)}")
        }
        paths[element] = path
        return element
    }

    fun claimText(expected: String): Text {
        val index = current.nextChild++
        val path = "${current.path}/text()[$index]"
        val actual = current.node.childNodes.item(index)
            ?: mismatch(path, "expected text ${expected.quote()}, found no node")
        if (actual.nodeType.toInt() != 3) {
            mismatch(path, "expected text ${expected.quote()}, found ${describe(actual)}")
        }
        val text = actual as Text
        if (text.data != expected) {
            mismatch(path, "expected text ${expected.quote()}, found text ${text.data.quote()}")
        }
        paths[text] = path
        return text
    }

    fun enter(node: Node) {
        frames += Frame(node, paths[node] ?: mismatch(current.path, "entered unclaimed node"))
    }

    fun leave(node: Node) {
        val frame = current
        if (frame.node !== node) mismatch(frame.path, "left unexpected node")
        verifyComplete(frame)
        frames.removeAt(frames.lastIndex)
    }

    fun verifyChild(parent: Node, index: Int, child: Node) {
        if (parent.childNodes.item(index) !== child) {
            mismatch(paths[child] ?: current.path, "claimed child moved before reconciliation")
        }
    }

    fun finish() {
        if (frames.size != 1) mismatch(current.path, "composition ended inside node")
        verifyComplete(frames.single())
    }

    fun recordListeners(target: EventTarget, value: List<NamedEventListener>) {
        listeners[target] = value
    }

    fun abort() {
        aborted = true
        listeners.forEach { (target, value) ->
            value.forEach { target.removeEventListener(it.name, it) }
        }
        listeners.clear()
    }

    private fun verifyComplete(frame: Frame) {
        val extra = frame.node.childNodes.item(frame.nextChild) ?: return
        mismatch(frame.path, "expected end of node, found extra ${describe(extra)}")
    }

    private fun describe(node: Node): String = when (node.nodeType.toInt()) {
        1 -> {
            val element = node as Element
            if (element.namespaceURI == HTML_NAMESPACE) "<${element.localName}>"
            else "<${element.localName}> in namespace ${element.namespaceURI}"
        }
        3 -> "text ${(node as Text).data.quote()}"
        else -> "node type ${node.nodeType}"
    }

    private fun String.quote() = buildString {
        append('"')
        this@quote.forEach { append(if (it == '\n') "\\n" else it) }
        append('"')
    }

    private fun mismatch(path: String, detail: String): Nothing {
        throw HydrationMismatchException("Hydration mismatch at $path: $detail")
    }
}

private var activeHydrationSession: HydrationSession? = null

internal fun <T> withHydrationSession(session: HydrationSession, block: () -> T): T {
    check(activeHydrationSession == null) { "A hydration session is already active" }
    activeHydrationSession = session
    return try {
        block()
    } finally {
        activeHydrationSession = null
    }
}

@ComposeWebInternalApi
fun claimHydrationElement(tagName: String): NativeNode? =
    activeHydrationSession?.claimElement(tagName)

@ComposeWebInternalApi
fun claimHydrationText(text: String): NativeNode? =
    activeHydrationSession?.claimText(text)

@ComposeWebInternalApi
fun recordHydrationEventListeners(node: NativeNode, listeners: List<NamedEventListener>) {
    activeHydrationSession?.recordListeners(node as EventTarget, listeners)
}
