/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.internal.runtime.NativeNode

// Namespace is irrelevant for string serialization (serializeToHtml only ever emits the tag
// name), so this ignores it and just creates the tagName-specific subtype -- same rationale as
// html-core's ElementBuilder.jvm.kt: the concrete subtype (not a generic fallback) is needed so
// that any downstream checkcast to a specific SVGXxxElement (from a call site where the type
// parameter has already been substituted concretely) succeeds rather than throwing
// ClassCastException.
actual fun createNativeElementNS(namespace: String, tagName: String): NativeNode = when (tagName) {
    "a" -> object : SVGAElement(tagName) {}
    "circle" -> object : SVGCircleElement(tagName) {}
    "clipPath" -> object : SVGClipPathElement(tagName) {}
    "defs" -> object : SVGDefsElement(tagName) {}
    "desc" -> object : SVGDescElement(tagName) {}
    "ellipse" -> object : SVGEllipseElement(tagName) {}
    "image" -> object : SVGImageElement(tagName) {}
    "line" -> object : SVGLineElement(tagName) {}
    "linearGradient" -> object : SVGLinearGradientElement(tagName) {}
    "marker" -> object : SVGMarkerElement(tagName) {}
    "mask" -> object : SVGMaskElement(tagName) {}
    "path" -> object : SVGPathElement(tagName) {}
    "pattern" -> object : SVGPatternElement(tagName) {}
    "polygon" -> object : SVGPolygonElement(tagName) {}
    "polyline" -> object : SVGPolylineElement(tagName) {}
    "radialGradient" -> object : SVGRadialGradientElement(tagName) {}
    "rect" -> object : SVGRectElement(tagName) {}
    "stop" -> object : SVGStopElement(tagName) {}
    "switch" -> object : SVGSwitchElement(tagName) {}
    "symbol" -> object : SVGSymbolElement(tagName) {}
    "text" -> object : SVGTextElement(tagName) {}
    "textPath" -> object : SVGTextPathElement(tagName) {}
    "title" -> object : SVGTitleElement(tagName) {}
    "tspan" -> object : SVGTSpanElement(tagName) {}
    "use" -> object : SVGUseElement(tagName) {}
    "view" -> object : SVGViewElement(tagName) {}
    else -> object : SVGElement(tagName) {}
}
