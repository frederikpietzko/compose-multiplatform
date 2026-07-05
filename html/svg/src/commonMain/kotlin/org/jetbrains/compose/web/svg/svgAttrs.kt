/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.attributes.*

// The CSSLengthOrPercentageValue/CSSPercentageValue-typed overloads of width/height/fillOpacity/
// cx/cy/r/rx/ry/x/y/x1/y1/x2/y2 that used to live in this file stayed jsMain-only (see
// SvgAttrsCssLengthOverloads.js.kt): the CSS unit/enum primitive cluster (org.jetbrains.compose.web.css
// CSSUnits.kt et al, e.g. `5.px`, `CSSLengthOrPercentageValue`) is itself deliberately jsMain-only
// per task 2.3/2.5's binding precedent (see html-core's css/StylePropertyValue.kt and
// task-2.5-report.md) -- it uses Kotlin/JS-only `external interface` + `unsafeCast` and has no
// commonMain/JVM port yet. The Number- and String-typed overloads below have no such dependency
// and are fully portable.

fun AttrsScope<SVGElement>.width(px: Number) {
    attr("width", px.toString())
}

fun AttrsScope<SVGElement>.height(px: Number) {
    attr("height", px.toString())
}

fun AttrsScope<SVGElement>.xmlns(nameSpace: String) {
    attr("xmlns", nameSpace)
}

fun AttrsScope<SVGElement>.attributeName(attributeName: String) {
    attr("attributeName", attributeName)
}

fun AttrsScope<SVGElement>.fill(color: String) {
    attr("fill", color)
}

fun AttrsScope<SVGElement>.fillRule(fill: String) {
    attr("fill-rule", fill)
}

fun AttrsScope<SVGElement>.fillOpacity(fill: Number) {
    attr("fill-opacity", fill.toString())
}

fun AttrsScope<SVGElement>.href(href: String) {
    attr("href", href)
}

fun AttrsScope<SVGElement>.viewBox(viewBox: String) {
    attr("viewBox", viewBox)
}

fun AttrsScope<SVGElement>.transform(transform: String) {
    attr("transform", transform)
}

fun AttrsScope<SVGElement>.d(d: String) {
    attr("d", d)
}

fun AttrsScope<SVGElement>.points(points: String) {
    attr("points", points)
}

fun AttrsScope<SVGElement>.points(vararg points: Number) {
    attr("points", points.toList().chunked(2).joinToString(" ") { it.joinToString(",") })
}

fun AttrsScope<SVGElement>.cx(cx: Number) {
    attr("cx", cx.toString())
}

fun AttrsScope<SVGElement>.cy(cy: Number) {
    attr("cy", cy.toString())
}

fun AttrsScope<SVGElement>.r(r: Number) {
    attr("r", r.toString())
}

fun AttrsScope<SVGElement>.rx(rx: Number) {
    attr("rx", rx.toString())
}

fun AttrsScope<SVGElement>.ry(ry: Number) {
    attr("ry", ry.toString())
}

fun AttrsScope<SVGElement>.x(x: Number) {
    attr("x", x.toString())
}

fun AttrsScope<SVGElement>.y(y: Number) {
    attr("y", y.toString())
}

fun AttrsScope<SVGElement>.x1(x1: Number) {
    attr("x1", x1.toString())
}

fun AttrsScope<SVGElement>.y1(y1: Number) {
    attr("y1", y1.toString())
}

fun AttrsScope<SVGElement>.x2(x2: Number) {
    attr("x2", x2.toString())
}

fun AttrsScope<SVGElement>.y2(y2: Number) {
    attr("y2", y2.toString())
}

fun AttrsScope<SVGElement>.to(to: String) {
    attr("to", to)
}
