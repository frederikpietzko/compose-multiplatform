/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.CSSGroupingRuleDeclaration
import org.jetbrains.compose.web.css.CSSRuleDeclaration
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.CSSStyledRuleDeclaration
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

// JVM actual of the StyleTagContent seam (task 2.5). There is no CSSOM on JVM to inject rules
// into live, so instead the serialized CSS text is rendered as a child text node of the <style>
// element -- the JVM render tree ends up with a <style> element containing the plain CSS text,
// which is what a server-rendered/static HTML document needs anyway.
@Composable
internal actual fun ElementScope<HTMLStyleElement>.StyleTagContent(cssRules: CSSRuleDeclarationList) {
    Text(cssRules.joinToString("\n") { it.stringPresentation() })
}

// JVM actual of stringPresentation (see commonMain dom/Style.kt for why this is expect/actual).
// Identical to the JS actual except for the CSSKeyframesRuleDeclaration branch: that type is part
// of the CSS unit/enum primitive cluster that stays jsMain-only (no commonMain/JVM `keyframes {}`
// builder exists to construct one), so it can never appear here at runtime and is omitted.
@OptIn(ComposeWebInternalApi::class)
actual fun CSSRuleDeclaration.stringPresentation(
    baseIndent: String,
    indent: String,
    delimiter: String
): String {
    val cssRuleDeclaration = this
    val strings = mutableListOf<String>()
    strings.add("$baseIndent${cssRuleDeclaration.header} {")
    when (cssRuleDeclaration) {
        is CSSStyledRuleDeclaration -> {
            cssRuleDeclaration.style.properties.forEach { (name, value, important) ->
                strings.add("$baseIndent$indent$name: $value${if (important) " !important" else ""};")
            }
            cssRuleDeclaration.style.variables.forEach { (name, value) ->
                strings.add("$baseIndent$indent--$name: $value;")
            }
        }
        is CSSGroupingRuleDeclaration -> {
            cssRuleDeclaration.rules.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
    }
    strings.add("$baseIndent}")
    return strings.joinToString(delimiter)
}
