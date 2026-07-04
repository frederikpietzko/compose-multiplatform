package org.jetbrains.compose.web.internal.runtime

// Common shell for a named DOM event listener (task 2.3).
//
// JS actualizes this to additionally implement org.w3c.dom.events.EventListener, so instances can
// be passed directly to Node.addEventListener/removeEventListener (see NativeNode.js.kt /
// ElementUpdater.js.kt). JVM has no real event dispatch (SSR has no events), so its actual is a
// bare marker -- applyEventListeners is inert there.
@ComposeWebInternalApi
expect interface NamedEventListener {
    val name: String
}
