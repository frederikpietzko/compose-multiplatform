/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

actual interface StylePropertyValue

actual interface StylePropertyNumber : StylePropertyValue
actual interface StylePropertyString : StylePropertyValue

// No native browser value to brand on JVM (SSR has no DOM) -- box the raw string/number so
// `.toString()` (the only operation ElementUpdater.jvm.kt performs on style values) round-trips.
private class JvmStylePropertyValue(private val raw: String) : StylePropertyNumber, StylePropertyString {
    override fun toString(): String = raw
}

actual fun StylePropertyValue(value: String): StylePropertyString = JvmStylePropertyValue(value)
actual fun StylePropertyValue(value: Number): StylePropertyNumber = JvmStylePropertyValue(value.toString())
