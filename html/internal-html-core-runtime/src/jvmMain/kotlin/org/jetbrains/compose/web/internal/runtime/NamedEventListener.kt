/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

// JVM has no real event dispatch (SSR has no events) -- this is a bare marker;
// applyEventListeners is inert on JVM (see ElementUpdater.jvm.kt).
@ComposeWebInternalApi
actual interface NamedEventListener {
    actual val name: String
}
