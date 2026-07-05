package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.w3c.dom.css.CSSGroupingRule
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleRule
import org.jetbrains.compose.web.css.*
import org.w3c.dom.css.CSSStyleSheet

// Moved from jsMain Style.kt (task 2.5): stringPresentation() was split out to commonMain
// dom/Style.kt (pure string serialization, no browser API). Everything remaining here is
// genuinely CSSOM-only and now also backs the JS actual of the StyleTagContent seam declared in
// Elements.kt (the seam that lets Style() inject rules on JS vs. render them as text on JVM).

internal fun CSSStyleSheet.setCSSRules(cssRules: CSSRuleDeclarationList) {
    cssRules.forEach { cssRule ->
        addRule(cssRule)
    }
}

private fun CSSStyleSheet.addRule(cssRule: String): CSSRule? {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules.item(cssRuleIndex)
}

private fun CSSKeyframesRule.addRule(cssRule: String): CSSRule? {
    appendRule(cssRule)
    return this.cssRules.item(this.cssRules.length - 1)
}

private fun CSSStyleSheet.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun CSSGroupingRule.addRule(cssRule: String): CSSRule? {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules.item(cssRuleIndex)
}

private fun CSSGroupingRule.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun CSSKeyframesRule.addRule(cssRuleDeclaration: CSSKeyframeRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun fillRule(
    cssRuleDeclaration: CSSRuleDeclaration,
    cssRule: CSSRule
) {
    when (cssRuleDeclaration) {
        is CSSStyledRuleDeclaration -> {
            val cssStyleRule = cssRule.unsafeCast<CSSStyleRule>()
            cssRuleDeclaration.style.properties.forEach { (name, value, important) ->
                setProperty(cssStyleRule.style, name, value, important)
            }
            cssRuleDeclaration.style.variables.forEach { (name, value) ->
                setVariable(cssStyleRule.style, name, value)
            }
        }
        is CSSGroupingRuleDeclaration -> {
            val cssGroupingRule = cssRule.unsafeCast<CSSGroupingRule>()
            cssRuleDeclaration.rules.forEach { childRuleDeclaration ->
                cssGroupingRule.addRule(childRuleDeclaration)
            }
        }
        is CSSKeyframesRuleDeclaration -> {
            val cssGroupingRule = cssRule.unsafeCast<CSSKeyframesRule>()
            cssRuleDeclaration.keys.forEach { childRuleDeclaration ->
                cssGroupingRule.addRule(childRuleDeclaration)
            }
        }
    }
}

internal fun setProperty(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue,
    important: Boolean
) {
    style.setProperty(name, value.toString(), if (important) "important" else "")
}

internal fun setVariable(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty("--$name", value.toString())
}

@OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
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
        is CSSKeyframesRuleDeclaration -> {
            cssRuleDeclaration.keys.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
    }
    strings.add("$baseIndent}")
    return strings.joinToString(delimiter)
}

@Composable
internal actual fun ElementScope<HTMLStyleElement>.StyleTagContent(cssRules: CSSRuleDeclarationList) {
    DisposableEffect(cssRules, cssRules.size) {
        val cssStylesheet = scopeElement.sheet as? CSSStyleSheet
        cssStylesheet?.setCSSRules(cssRules)
        onDispose {
            cssStylesheet?.clearCSSRules()
        }
    }
}

private fun CSSStyleSheet.clearCSSRules() {
    repeat(cssRules.length) {
        deleteRule(0)
    }
}
