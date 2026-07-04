import org.jetbrains.compose.web.internal.runtime.*
import org.jetbrains.compose.web.internal.runtime.applyAttrs
import org.jetbrains.compose.web.internal.runtime.applyClasses
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementUpdaterJvmTest {
    @Test fun writesAttrsAndClasses() {
        val el = JvmElement("div")
        (el as NativeNode).applyAttrs(mapOf("id" to "main"))
        el.applyClasses(listOf("a", "b"))
        assertEquals("main", el.attributes["id"])
        assertEquals(listOf("a", "b"), el.classes)
    }
}
