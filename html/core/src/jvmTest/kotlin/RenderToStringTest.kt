import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.dom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderToStringTest {
    @Test fun rendersSimpleTree() {
        val html = renderToString {
            Div({ classes("greeting") }) {
                Text("Hello")
                Span { Text("!") }
            }
        }
        assertEquals("""<div class="greeting">Hello<span>!</span></div>""", html)
    }

    @Test fun rendersConditionalStateAfterSettle() {
        val html = renderToString {
            var shown = androidx.compose.runtime.remember { true }
            if (shown) P { Text("visible") }
        }
        assertEquals("<p>visible</p>", html)
    }
}
