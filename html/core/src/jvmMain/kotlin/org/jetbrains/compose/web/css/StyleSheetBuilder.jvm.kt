/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

// Actualizes GenericStyleSheetBuilder (see commonMain StyleSheetBuilder.kt). buildRules/style are
// the only members in the expect contract; the extension-sugar members below are plain
// (non-actual) default methods declared directly on this actual interface -- see the comment on
// the expect declaration for why they can't be part of the expect contract itself. The JS actual
// (StyleSheetBuilder.js.kt) additionally declares `infix fun CSSSelector.style { ... }`; that
// member is omitted here because on the JVM target it erases to the exact same JVM signature as
// the plain `style(selector, cssRule)` member below ("Platform declaration clash") -- use
// `style(selector) { ... }` (the two-argument member) instead.
actual interface GenericStyleSheetBuilder<TBuilder> : CSSRulesHolder, SelectorsScope {
    actual fun buildRules(
        rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
    ): CSSRuleDeclarationList

    actual fun style(selector: CSSSelector, cssRule: TBuilder.() -> Unit)

    operator fun CSSSelector.invoke(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    operator fun String.invoke(cssRule: TBuilder.() -> Unit) {
        style(RawSelector(this), cssRule)
    }

    infix fun String.style(cssRule: TBuilder.() -> Unit) {
        style(RawSelector(this), cssRule)
    }
}
