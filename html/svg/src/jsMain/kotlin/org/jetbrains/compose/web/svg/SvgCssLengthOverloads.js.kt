/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.CSSLengthOrPercentageValue
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.TagElement

// Circle/Rect/Ellipse/Line overloads split out of svg.kt (task 4.2) -- see that file's header
// comment on the `internal` builder vals they reference (Circle/Rect/Ellipse/Line). These stay
// jsMain-only because the CSS unit/enum primitive cluster they depend on (`CSSLengthOrPercentageValue`,
// `5.px`, ...) is itself jsMain-only (task 2.3/2.5 precedent), not because of any DOM/browser
// dependency of their own. The Number-typed overloads of the same composables already live in
// commonMain.

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Circle(
  cx: CSSLengthOrPercentageValue,
  cy: CSSLengthOrPercentageValue,
  r: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGCircleElement>? = null,
  content: ContentBuilder<SVGCircleElement>? = null
) {
    TagElement(
        elementBuilder = Circle,
        applyAttrs = {
            cx(cx)
            cy(cy)
            r(r)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Rect(
  x: CSSLengthOrPercentageValue,
  y: CSSLengthOrPercentageValue,
  width: CSSLengthOrPercentageValue,
  height: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGRectElement>? = null,
  content: ContentBuilder<SVGRectElement>? = null
) {
    TagElement(
        elementBuilder = Rect,
        applyAttrs = {
            x(x)
            y(y)
            width(width)
            height(height)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Ellipse(
  cx: CSSLengthOrPercentageValue,
  cy: CSSLengthOrPercentageValue,
  rx: CSSLengthOrPercentageValue,
  ry: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGEllipseElement>? = null,
  content: ContentBuilder<SVGEllipseElement>? = null
) {
    TagElement(
        elementBuilder = Ellipse,
        applyAttrs = {
            cx(cx)
            cy(cy)
            rx(rx)
            ry(ry)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
@ExperimentalComposeWebSvgApi
fun ElementScope<SVGElement>.Line(
  x1: CSSLengthOrPercentageValue,
  y1: CSSLengthOrPercentageValue,
  x2: CSSLengthOrPercentageValue,
  y2: CSSLengthOrPercentageValue,
  attrs: AttrBuilderContext<SVGLineElement>? = null,
  content: ContentBuilder<SVGLineElement>? = null
) {
    TagElement(
        elementBuilder = Line,
        applyAttrs = {
            x1(x1)
            y1(y1)
            x2(x2)
            y2(y2)
            attrs?.invoke(this)
        },
        content = content
    )
}
