package ba.grbo.wateringplants.util

class Event<out T>(content: T) {
    private var hasBeenHandled = false

    val content: T? = content
        get() = if (hasBeenHandled) null else {
            hasBeenHandled = true
            field
        }
}