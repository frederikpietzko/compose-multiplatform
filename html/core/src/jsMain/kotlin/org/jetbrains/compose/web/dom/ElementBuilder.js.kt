/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.document
import org.jetbrains.compose.web.internal.runtime.NativeNode
import org.jetbrains.compose.web.internal.runtime.claimHydrationElement
import org.jetbrains.compose.web.internal.runtime.claimHydrationText

actual fun createNativeElement(tagName: String): NativeNode =
    claimHydrationElement(tagName) ?: document.createElement(tagName)

actual fun createNativeText(text: String): NativeNode =
    claimHydrationText(text) ?: document.createTextNode(text)
