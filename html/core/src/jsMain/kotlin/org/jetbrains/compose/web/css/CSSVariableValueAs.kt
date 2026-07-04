/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.jetbrains.compose.web.css

// Split out of the former jsMain-only StyleScope.kt when StyleScope/StyleHolder/StyleScopeBuilder
// moved to commonMain (task 2.3). This advanced, optional user-extensibility hook (typed CSS
// variables via a marker supertype) is JS-only and stays jsMain-only -- it is not required by the
// AttrsScope/ElementUpdater seam and porting it is out of scope for this task.
external interface CSSVariableValueAs<out T : StylePropertyValue>

fun <TValue> CSSStyleVariable<TValue>.value(fallback: TValue? = null)
        where TValue : CSSVariableValueAs<TValue>,
              TValue : StylePropertyValue =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback
        )
    )
