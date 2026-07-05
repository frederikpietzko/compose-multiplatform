/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.svg

import org.jetbrains.compose.web.dom.DomElement

// SVG mirror of html-core's DomElementTypes.kt (task 2.1) -- see that file's header comment for
// the full derisking history of this `expect abstract class` form. The short version: JS
// actualizes each type to the identically-named browser type (org.w3c.dom.svg.* /
// org.w3c.css.masking.*), while JVM actualizes all of them to a real (mostly empty) subclass
// chain rooted at the common `DomElement` (which itself chains to
// org.jetbrains.compose.web.internal.runtime.JvmElement on JVM) -- see SvgElementTypes.js.kt /
// SvgElementTypes.jvm.kt.
//
// Real SVG DOM interfaces have a deeper hierarchy than what's declared here (e.g. the real
// SVGCircleElement extends SVGGeometryElement extends SVGGraphicsElement extends SVGElement), but
// html-core's own HTMLAudioElement/HTMLVideoElement (which really extend HTMLMediaElement, not
// HTMLElement, directly) already establish that a flattened one-level `expect` hierarchy is fine:
// `actual typealias` on JS doesn't require the declared supertype to be the *immediate* real
// supertype, only that the real type is assignable wherever the expect type is used, which holds
// transitively. All real org.w3c.dom.svg / org.w3c.css.masking classes referenced below are
// `external abstract class`, so every actual here uses matching `abstract` modality.
expect abstract class SVGElement : DomElement

expect abstract class SVGAElement : SVGElement
expect abstract class SVGCircleElement : SVGElement
expect abstract class SVGClipPathElement : SVGElement
expect abstract class SVGDefsElement : SVGElement
expect abstract class SVGDescElement : SVGElement
expect abstract class SVGEllipseElement : SVGElement
expect abstract class SVGImageElement : SVGElement
expect abstract class SVGLineElement : SVGElement
expect abstract class SVGLinearGradientElement : SVGElement
expect abstract class SVGMarkerElement : SVGElement
expect abstract class SVGMaskElement : SVGElement
expect abstract class SVGPathElement : SVGElement
expect abstract class SVGPatternElement : SVGElement
expect abstract class SVGPolygonElement : SVGElement
expect abstract class SVGPolylineElement : SVGElement
expect abstract class SVGRadialGradientElement : SVGElement
expect abstract class SVGRectElement : SVGElement
expect abstract class SVGStopElement : SVGElement
expect abstract class SVGSwitchElement : SVGElement
expect abstract class SVGSymbolElement : SVGElement
expect abstract class SVGTextElement : SVGElement
expect abstract class SVGTextPathElement : SVGElement
expect abstract class SVGTitleElement : SVGElement
expect abstract class SVGTSpanElement : SVGElement
expect abstract class SVGUseElement : SVGElement
expect abstract class SVGViewElement : SVGElement
