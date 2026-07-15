/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

/** Thrown when serialized hydration state is invalid or cannot be encoded or restored. */
class HydrationStateException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
