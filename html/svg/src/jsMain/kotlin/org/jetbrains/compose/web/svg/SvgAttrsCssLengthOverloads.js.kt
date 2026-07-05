/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSLengthOrPercentageValue
import org.jetbrains.compose.web.css.CSSPercentageValue

// CSSLengthOrPercentageValue/CSSPercentageValue-typed overloads split out of svgAttrs.kt (task
// 4.2) -- see the comment atop that file. These stay jsMain-only because the CSS unit/enum
// primitive cluster they depend on (`5.px`, `CSSLengthOrPercentageValue`, ...) is itself
// jsMain-only (task 2.3/2.5 precedent), not because of any DOM/browser dependency of their own.

fun AttrsScope<SVGElement>.width(value: CSSLengthOrPercentageValue) {
    attr("width", value.toString())
}

fun AttrsScope<SVGElement>.height(value: CSSLengthOrPercentageValue) {
    attr("height", value.toString())
}

fun AttrsScope<SVGElement>.fillOpacity(fill: CSSPercentageValue) {
    attr("fill-opacity", fill.toString())
}

fun AttrsScope<SVGElement>.cx(cx: CSSLengthOrPercentageValue) {
    attr("cx", cx.toString())
}

fun AttrsScope<SVGElement>.cy(cy: CSSLengthOrPercentageValue) {
    attr("cy", cy.toString())
}

fun AttrsScope<SVGElement>.r(r: CSSLengthOrPercentageValue) {
    attr("r", r.toString())
}

fun AttrsScope<SVGElement>.rx(rx: CSSLengthOrPercentageValue) {
    attr("rx", rx.toString())
}

fun AttrsScope<SVGElement>.ry(ry: CSSLengthOrPercentageValue) {
    attr("ry", ry.toString())
}

fun AttrsScope<SVGElement>.x(x: CSSLengthOrPercentageValue) {
    attr("x", x.toString())
}

fun AttrsScope<SVGElement>.y(y: CSSLengthOrPercentageValue) {
    attr("y", y.toString())
}

fun AttrsScope<SVGElement>.x1(x1: CSSLengthOrPercentageValue) {
    attr("x1", x1.toString())
}

fun AttrsScope<SVGElement>.y1(y1: CSSLengthOrPercentageValue) {
    attr("y1", y1.toString())
}

fun AttrsScope<SVGElement>.x2(x2: CSSLengthOrPercentageValue) {
    attr("x2", x2.toString())
}

fun AttrsScope<SVGElement>.y2(y2: CSSLengthOrPercentageValue) {
    attr("y2", y2.toString())
}
