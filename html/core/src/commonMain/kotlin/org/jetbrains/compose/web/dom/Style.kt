/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.css.CSSRuleDeclaration

// Split out of the former (jsMain-only) Style.kt (task 2.5). stringPresentation() itself is pure
// string serialization with no browser-API dependency, but its jsMain body pattern-matches on
// CSSKeyframesRuleDeclaration (dom/../css/CSSKeyframeRule.kt), which stays jsMain-only: it's part
// of the CSS unit/enum primitive cluster (CSSUnit/CSSUnits.kt-style `external interface` +
// `unsafeCast` typed values) that task 2.3 already flagged out of scope for porting. Since a
// CSSKeyframesRuleDeclaration instance can therefore never exist in JVM-reachable code (there's no
// commonMain/JVM `keyframes {}` builder that could construct one), stringPresentation is declared
// expect/actual: the JS actual is byte-for-byte the original body; the JVM actual omits the
// CSSKeyframesRuleDeclaration branch as unreachable on that platform. The remaining CSSOM-mutating
// helpers (setCSSRules/addRule/fillRule/setProperty/setVariable) also stay jsMain-only, in the
// renamed Style.js.kt; together with the JVM actual below they back the StyleTagContent seam
// declared in Elements.kt.
expect fun CSSRuleDeclaration.stringPresentation(
    baseIndent: String = "",
    indent: String = "    ",
    delimiter: String = "\n"
): String
