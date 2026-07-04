package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// JVM/SSR has no event dispatch -- this is a bare marker; applyEventListeners is inert on JVM
// (see ElementUpdater.jvm.kt). The rich onClick/onMouseDown/etc. surface stays jsMain-only.
actual interface EventsListenerScope {
    @ComposeWebInternalApi
    actual fun registerEventListener(listener: NamedEventListener)
}
