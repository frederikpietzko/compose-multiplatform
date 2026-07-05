/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.NativeEvent
import androidx.compose.web.events.readCheckedOrNull
import androidx.compose.web.events.readInputValueOrNull
import androidx.compose.web.events.readValueAsNumberOrNull

// DirType/Draggable/InputMode split out of the former jsMain-only PredefinedAttrValues.kt (task
// 2.3) so AttrsScope's dir()/draggable()/inputMode() default methods can move to commonMain. Pure
// Kotlin value types with no browser dependency -- moved verbatim, unchanged.
//
// InputType and the remaining sealed marker classes (ATarget, ARel, ...) below were moved here in
// task 2.5, now that attributes/Attrs.kt and the attributes/builders/*.kt files that use them are
// moving to commonMain too. InputType.inputValue used to read org.w3c.dom.events.Event directly
// via asDynamic()/unsafeCast; it now reads the common NativeEvent/NativeEventTarget seam (see
// events/NativeEvent.kt) via the readInputValueOrNull/readCheckedOrNull/readValueAsNumberOrNull
// expect helpers, same values, same per-subtype dispatch, no behavior change.
//
// AutoComplete stays jsMain-only (attributes/PredefinedAttrValues.kt): it's a JS `external
// interface` zero-cost branding type (`value.unsafeCast<AutoComplete>()`), which needs a JVM
// boxed-holder redesign before it can move -- see task-2.5 report.

sealed class DirType(val dirStr: String) {
    object Ltr : DirType("ltr")
    object Rtl : DirType("rtl")
    object Auto : DirType("auto")
}

enum class Draggable(val str: String) {
    True("true"), False("false"), Auto("auto");
}

/**
 * see https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode
 */
enum class InputMode(val str: String) {
    None("none"),
    Text("text"), // default
    Decimal("decimal"),
    Numeric("numeric"),
    Tel("tel"),
    Search("search"),
    Email("email"),
    Url("url"),
    ;
}

sealed class InputType<T>(val typeStr: String) {

    object Button : InputTypeWithUnitValue("button")
    object Checkbox : InputTypeCheckedValue("checkbox")
    object Color : InputTypeWithStringValue("color")
    object Date : InputTypeWithStringValue("date")
    object DateTimeLocal : InputTypeWithStringValue("datetime-local")
    object Email : InputTypeWithStringValue("email")
    object File : InputTypeWithStringValue("file")
    object Hidden : InputTypeWithStringValue("hidden")
    object Month : InputTypeWithStringValue("month")
    object Number : InputTypeNumberValue("number")
    object Password : InputTypeWithStringValue("password")
    object Radio : InputTypeCheckedValue("radio")
    object Range : InputTypeNumberValue("range")
    object Search : InputTypeWithStringValue("search")
    object Submit : InputTypeWithUnitValue("submit")
    object Tel : InputTypeWithStringValue("tel")
    object Text : InputTypeWithStringValue("text")
    object Time : InputTypeWithStringValue("time")
    object Url : InputTypeWithStringValue("url")
    object Week : InputTypeWithStringValue("week")

    open class InputTypeWithStringValue(name: String) : InputType<String>(name) {
        override fun inputValue(event: NativeEvent) = Week.valueAsString(event)
    }

    open class InputTypeWithUnitValue(name: String) : InputType<Unit>(name) {
        override fun inputValue(event: NativeEvent) = Unit
    }

    open class InputTypeCheckedValue(name: String) : InputType<Boolean>(name) {
        override fun inputValue(event: NativeEvent): Boolean {
            return event.target.readCheckedOrNull() ?: false
        }
    }

    open class InputTypeNumberValue(name: String) : InputType<kotlin.Number?>(name) {
        override fun inputValue(event: NativeEvent): kotlin.Number? {
            return event.target.readValueAsNumberOrNull()
        }
    }

    abstract fun inputValue(event: NativeEvent): T

    protected fun valueAsString(event: NativeEvent): String {
        return event.target.readInputValueOrNull() ?: ""
    }

    companion object {
        internal fun fromString(type: String): InputType<*> {
            return when (type) {
                "button" -> Button
                "checkbox" -> Checkbox
                "color" -> Color
                "date" -> Date
                "datetime-local" -> DateTimeLocal
                "email" -> Email
                "file" -> File
                "hidden" -> Hidden
                "month" -> Month
                "number" -> Number
                "password" -> Password
                "radio" -> Radio
                "range" -> Range
                "search" -> Search
                "submit" -> Submit
                "tel" -> Tel
                "text" -> Text
                "time" -> Time
                "url" -> Url
                "week" -> Week
                else -> error("fromString got unknown type - $type")
            }
        }
    }
}

sealed class ATarget(val targetStr: String) {
    object Blank : ATarget("_blank")
    object Parent : ATarget("_parent")
    object Self : ATarget("_self")
    object Top : ATarget("_top")
}

sealed class ARel(val relStr: String) {
    object Alternate : ARel("alternate")
    object Author : ARel("author")
    object Bookmark : ARel("bookmark")
    object External : ARel("external")
    object Help : ARel("help")
    object License : ARel("license")
    object Next : ARel("next")
    object First : ARel("first")
    object Prev : ARel("prev")
    object Last : ARel("last")
    object NoFollow : ARel("nofollow")
    object NoOpener : ARel("noopener")
    object NoReferrer : ARel("noreferrer")
    object Opener : ARel("opener")
    object Search : ARel("search")
    object Tag : ARel("tag")

    class CustomARel(value: String) : ARel(value)
}

enum class ButtonType(val str: String) {
    Button("button"), Reset("reset"), Submit("submit")
}

sealed class ButtonFormTarget(val targetStr: String) {
    object Blank : ButtonFormTarget("_blank")
    object Parent : ButtonFormTarget("_parent")
    object Self : ButtonFormTarget("_self")
    object Top : ButtonFormTarget("_top")
}

enum class ButtonFormMethod(val methodStr: String) {
    Get("get"), Post("post")
}

enum class ButtonFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class FormTarget(val targetStr: String) {
    object Blank : FormTarget("_blank")
    object Parent : FormTarget("_parent")
    object Self : FormTarget("_self")
    object Top : FormTarget("_top")
}

enum class InputFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class InputFormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class InputFormTarget(val targetStr: String) {
    object Blank : InputFormTarget("_blank")
    object Parent : InputFormTarget("_parent")
    object Self : InputFormTarget("_self")
    object Top : InputFormTarget("_top")
}

enum class TextAreaWrap(val str: String) {
    Hard("hard"),
    Soft("soft"),
    Off("off")
}

enum class Scope(val str: String) {
    Row("row"),
    Rowgroup("rowgroup"),
    Col("col"),
    Colgroup("colgroup")
}
