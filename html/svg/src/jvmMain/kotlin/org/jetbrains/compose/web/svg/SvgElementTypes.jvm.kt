/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.dom.DomElement

// See SvgElementTypes.kt for the full rationale. Mirrors html-core's DomElementTypes.jvm.kt:
// DomElement can't be constructed directly (it's `abstract`), so every leaf type here is a thin
// real subclass chained off the common DomElement, and a concrete leaf value is obtained via an
// anonymous object expression (see CreateNativeElementNS.jvm.kt).
actual abstract class SVGElement(tagName: String) : DomElement(tagName)

actual abstract class SVGAElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGCircleElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGClipPathElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGDefsElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGDescElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGEllipseElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGImageElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGLineElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGLinearGradientElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGMarkerElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGMaskElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGPathElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGPatternElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGPolygonElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGPolylineElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGRadialGradientElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGRectElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGStopElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGSwitchElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGSymbolElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGTextElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGTextPathElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGTitleElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGTSpanElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGUseElement(tagName: String) : SVGElement(tagName)
actual abstract class SVGViewElement(tagName: String) : SVGElement(tagName)
