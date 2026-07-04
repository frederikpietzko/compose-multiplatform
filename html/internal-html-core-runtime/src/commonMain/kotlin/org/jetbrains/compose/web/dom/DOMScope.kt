/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.DisposableEffectScope
import org.jetbrains.compose.web.internal.runtime.NativeNode

// Bound loosened from org.w3c.dom.Element to the common NativeNode (task 2.3) so this type is
// usable from commonMain. renderComposable's `TElement : Element` (JS-only) still satisfies this.
interface DOMScope<out TElement : NativeNode> {
    val DisposableEffectScope.scopeElement: TElement
}
