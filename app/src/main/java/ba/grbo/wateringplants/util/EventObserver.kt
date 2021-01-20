package ba.grbo.wateringplants.util

import androidx.lifecycle.Observer

class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.content?.let { onEventUnhandledContent(it) }
    }
}