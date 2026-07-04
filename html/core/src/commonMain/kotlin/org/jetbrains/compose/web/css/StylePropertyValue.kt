/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// Split out of the former (jsMain-only) StylePropertyValue.kt so StyleScope/StyleHolder can move
// to commonMain (task 2.3). These three types are opaque markers with no members: on JS they
// actualize to the existing `external interface`s (zero runtime footprint, unchanged behavior);
// on JVM there's no native browser value to brand, so the factory functions wrap the raw
// string/number in a small boxed holder (see StylePropertyValue.jvm.kt).
//
// CSSVariableValueAs and the concrete CSS property extension functions (color(), width(), etc.)
// stay jsMain-only -- porting those is out of scope for this task.
expect interface StylePropertyValue

expect interface StylePropertyNumber : StylePropertyValue
expect interface StylePropertyString : StylePropertyValue

expect fun StylePropertyValue(value: String): StylePropertyString
expect fun StylePropertyValue(value: Number): StylePropertyNumber
