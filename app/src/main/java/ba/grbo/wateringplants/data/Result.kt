package ba.grbo.wateringplants.data

sealed class Result<out R> {
    val succeeded
        get() = this is Success && data != null

    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    override fun toString(): String = when (this) {
        is Success<*> -> "Success[data=$data]"
        is Error -> "Error[exception=$exception]"
    }
}