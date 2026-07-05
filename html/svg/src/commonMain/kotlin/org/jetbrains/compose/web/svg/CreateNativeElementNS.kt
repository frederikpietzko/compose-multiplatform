/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.internal.runtime.NativeNode

/**
 * Creates the platform-native node for a namespaced (SVG) [tagName].
 *
 * Mirrors html-core's [org.jetbrains.compose.web.dom.createNativeElement] seam, but namespaced:
 *
 * JS: `document.createElementNS(namespace, tagName)` -- SVG elements need the real SVG namespace
 * on the actual DOM, or the browser renders them as unknown/opaque elements.
 *
 * JVM: a bare [org.jetbrains.compose.web.internal.runtime.JvmElement] -- namespace is irrelevant
 * for string serialization, since [org.jetbrains.compose.web.internal.runtime.serializeToHtml]
 * only ever emits the tag name.
 */
expect fun createNativeElementNS(namespace: String, tagName: String): NativeNode
