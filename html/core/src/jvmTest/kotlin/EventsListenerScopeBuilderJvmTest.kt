import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.EventsListenerScopeBuilder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals

// Regression test for the public-API loss discovered in task 2.4 review: EventsListenerScopeBuilder
// (and its deprecated alias EventsListenerBuilder) is a public, non-AttrsScope implementer of
// EventsListenerScope, and it must keep exposing the onXxx/addEventListener surface directly,
// not just via AttrsScope.
class EventsListenerScopeBuilderJvmTest {
    @OptIn(ComposeWebInternalApi::class)
    @Test
    fun onClickAndAddEventListenerAreAvailableDirectlyOnTheBuilder() {
        val builder = EventsListenerScopeBuilder()

        builder.onClick { }
        builder.addEventListener("custom-event") { _: SyntheticEvent<NativeEventTarget> -> }

        assertEquals(2, builder.collectListeners().size)
    }
}
