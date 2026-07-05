/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

// Actualizes GenericStyleSheetBuilder (see commonMain StyleSheetBuilder.kt). buildRules/style are
// the only members in the expect contract (each concrete builder, e.g. StyleSheetBuilder /
// CSSBuilder, still supplies its own real implementation); the extension-sugar members below are
// plain (non-actual) default methods declared directly on this actual interface -- see the comment
// on the expect declaration for why they can't be part of the expect contract itself. JS keeps all
// four, including the `infix fun CSSSelector.style { ... }` convenience (real usage:
// jsTest/kotlin/CSSStylesheetTests.kt's `self style { ... }`); the JVM actual
// (StyleSheetBuilder.jvm.kt) omits that one member because it erases to the same JVM signature as
// the plain `style(selector, cssRule)` member ("Platform declaration clash").
actual interface GenericStyleSheetBuilder<TBuilder> : CSSRulesHolder, SelectorsScope {
    actual fun buildRules(
        rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
    ): CSSRuleDeclarationList

    actual fun style(selector: CSSSelector, cssRule: TBuilder.() -> Unit)

    operator fun CSSSelector.invoke(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    infix fun CSSSelector.style(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    operator fun String.invoke(cssRule: TBuilder.() -> Unit) {
        style(RawSelector(this), cssRule)
    }

    infix fun String.style(cssRule: TBuilder.() -> Unit) {
        style(RawSelector(this), cssRule)
    }
}
