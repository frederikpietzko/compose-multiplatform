package org.jetbrains.compose.web.internal.runtime

// JVM has no real event dispatch (SSR has no events) -- this is a bare marker;
// applyEventListeners is inert on JVM (see ElementUpdater.jvm.kt).
@ComposeWebInternalApi
actual interface NamedEventListener {
    actual val name: String
}
