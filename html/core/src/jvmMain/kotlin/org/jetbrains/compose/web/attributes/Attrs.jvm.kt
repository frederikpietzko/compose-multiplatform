/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.dom.HTMLInputElement
import org.jetbrains.compose.web.dom.HTMLTextAreaElement

// SSR never invokes AttrsScope.prop() against a live element, so these are inert no-ops -- an
// accepted v1 limitation for controlled-input SSR (see task-2.5 report).

internal actual val setInputValue: (HTMLInputElement, String) -> Unit = { _, _ -> }

internal actual val setTextAreaDefaultValue: (HTMLTextAreaElement, String) -> Unit = { _, _ -> }

internal actual val setCheckedValue: (HTMLInputElement, Boolean) -> Unit = { _, _ -> }
