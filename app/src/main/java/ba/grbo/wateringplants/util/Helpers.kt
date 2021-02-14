package ba.grbo.wateringplants.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import ba.grbo.wateringplants.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

fun getAnimation(
    context: Context,
    animation: Int,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null
): Animation = AnimationUtils.loadAnimation(context, animation).apply {
    setAnimationListener(getAnimationListener(onAnimationStart, onAnimationEnd, onAnimationRepeat))
}

private fun getAnimationListener(
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null
) = object : Animation.AnimationListener {
    override fun onAnimationStart(animation: Animation?) {
        onAnimationStart?.invoke()
    }

    override fun onAnimationEnd(animation: Animation?) {
        onAnimationEnd?.invoke()
    }

    override fun onAnimationRepeat(animation: Animation?) {
        onAnimationRepeat?.invoke()
    }
}

fun onCreateAnimation(
    transit: Int,
    enter: Boolean,
    nextAnim: Int,
    context: Context,
    onEnterAnimationStart: () -> Unit,
    onEnterAnimationEnd: () -> Unit,
    superOnCreateAnimation: (Int, Boolean, Int) -> Animation?
): Animation? {
    return if (nextAnim == 0 || !enter) superOnCreateAnimation(transit, enter, nextAnim)
    else getAnimation(context, nextAnim, onEnterAnimationStart, onEnterAnimationEnd)
}

fun onReleaseFocus(
    view: View,
    event: MotionEvent,
    action: () -> Unit
) {
    val touchPoint = Point(event.rawX.roundToInt(), event.rawY.roundToInt())
    val viewTouched = isPointInsideViewBounds(view, touchPoint)
    if (!viewTouched) action()
}

private fun isPointInsideViewBounds(view: View, point: Point): Boolean = Rect().run {
    // get view rectangle
    view.getDrawingRect(this)

    // apply offset
    IntArray(2).also { locationOnScreen ->
        view.getLocationOnScreen(locationOnScreen)
        offset(locationOnScreen[0], locationOnScreen[1])
    }

    // check is rectangle contains point
    contains(point.x, point.y)
}

fun removeExcessiveSpace(text: String) = text
    .trim()
    .fold(StringBuilder()) { result, char ->
        if ((char != ' ' && char != '\n') || (char != '\n' && result[result.length - 1] != ' ')) {
            result.append(char)
        } else if ((char == '\n') && result[result.length - 1] != ' ') result.append(' ')
        result
    }
    .toString()

fun TextInputEditText.setCustomOnFocusChangeListener(
    showKeyboard: (View) -> Unit,
    hideKeyboard: (View) -> Unit,
    setOnTouchListener: (((MotionEvent) -> Unit)?) -> Unit,
    onEditTextReleaseFocus: () -> Unit
) {
    setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            showKeyboard(this)
            setOnTouchListener { onReleaseFocus(this, it, ::clearFocus) }
            if (id != R.id.watering_period_edit_text) onEditTextReleaseFocus()
        } else {
            hideKeyboard(this)
            setOnTouchListener(null)
            when (id) {
                R.id.plant_name_edit_text,
                R.id.plant_description_edit_text -> {
                    val text = text.toString()
                    if (text.isNotEmpty()) setText(removeExcessiveSpace(text))
                }
            }
        }
    }
}

fun Uri.getRealPathFromUriAPI19(context: Context): String? {
    if (DocumentsContract.isDocumentUri(context, this)) { // DownloadsProvider
        if (isDownloadsDocument(this)) { // DownloadsProvider
            val id = DocumentsContract.getDocumentId(this)
            if (!TextUtils.isEmpty(id)) {
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "")
                }
                return try {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )

                    getDataColumn(context, contentUri, null, null)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        } else if (isMediaDocument(this)) { // MediaProvider
            val docId = DocumentsContract.getDocumentId(this)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            val contentUri = when (split[0]) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(this.scheme, ignoreCase = true)) { // MediaStore (and general)
        return if (isGooglePhotosUri(this)) this.lastPathSegment
        else getDataColumn(context, this, null, null)
    } else if ("file".equals(this.scheme, ignoreCase = true)) { // File
        return this.path
    }

    return null
}

private fun getDataColumn(
    context: Context,
    uri: Uri?,
    selection: String?,
    selectionArgs: Array<String>?
): String? {

    var cursor: Cursor? = null
    val column = "_data"

    try {
        cursor = context.contentResolver.query(
            uri!!,
            arrayOf(column),
            selection,
            selectionArgs,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}

val Boolean.toVisibility: Int
    get() = if (this) View.VISIBLE else View.GONE

val Int.toVisibility: Boolean
    get() = this == View.VISIBLE

fun <T> Flow<T>.launchInWhenStarted(scope: LifecycleCoroutineScope): Job = scope.launchWhenStarted {
    collect()
}

val <T> SharedFlow<T>.value
    get() = if (replayCache.isNotEmpty()) replayCache[0] else null

// StateFlow imitation without an initial value, so we avoid null as an initial value
@Suppress("FunctionName", "UNCHECKED_CAST")
fun <T> SharedStateLikeFlow() = MutableSharedFlow<T>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

@Suppress("FunctionName", "UNCHECKED_CAST")
fun <T> SingleSharedFlow() = MutableSharedFlow<T>(
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
    extraBufferCapacity = 1
)

fun <T> Flow<T>.collect(scope: LifecycleCoroutineScope, action: suspend (T) -> Unit) {
    onEach { action(it) }.launchInWhenStarted(scope)
}

fun <T> Fragment.collect(flow: Flow<T>, action: suspend (T) -> Unit) {
    flow.collect(viewLifecycleOwner.lifecycleScope, action)
}

fun <T> AppCompatActivity.collect(flow: Flow<T>, action: suspend (T) -> Unit) {
    flow.collect(lifecycleScope, action)
}

fun showSnackbar(view: View, @StringRes text: Int) {
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show()
}

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}

class VerticalGridSpacingItemDecoration(
    private val spacing: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position

        if (position == 0) outRect.left = spacing
        outRect.top = spacing
        outRect.bottom = spacing
        outRect.right = spacing
    }
}

const val BUNDLE_ARGS = "BUNDLE_ARGS"