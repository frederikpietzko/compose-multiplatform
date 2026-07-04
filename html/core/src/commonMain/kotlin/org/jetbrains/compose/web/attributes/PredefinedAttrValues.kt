package org.jetbrains.compose.web.attributes

// DirType/Draggable/InputMode split out of the former jsMain-only PredefinedAttrValues.kt (task
// 2.3) so AttrsScope's dir()/draggable()/inputMode() default methods can move to commonMain. Pure
// Kotlin value types with no browser dependency -- moved verbatim, unchanged. The rest of that
// file (InputType, AutoComplete, ATarget, ARel, ...) stays jsMain-only: it's either tied to
// org.w3c.dom.events.Event/unsafeCast, or (for the sealed marker classes like ATarget/ARel used
// only by jsMain-only builder scopes) simply not needed by the AttrsScope/ElementUpdater seam.

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
