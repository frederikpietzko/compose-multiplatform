/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import kotlinx.browser.document
import org.jetbrains.compose.web.internal.runtime.NativeNode

actual fun createNativeElementNS(namespace: String, tagName: String): NativeNode =
    document.createElementNS(namespace, tagName)
