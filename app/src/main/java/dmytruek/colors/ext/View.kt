@file:Suppress("unused")

package dmytruek.colors.ext

import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.*

class DebouncingOnClickListener(
    private val intervalMillis: Long = 100,
    private val onClickListener: (View) -> Unit,
) : View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastClickTime >= intervalMillis) {
            lastClickTime = currentTime
            onClickListener(v)
        }
    }
}

var View.backgroundColor: Int   // 0xAARRGGBB
    get() = (this.background as? ColorDrawable)?.color ?: 0
    set(value) = this.setBackgroundColor(value)

var View.backgroundRes: Int     // R.drawable.your_drawable_file_name
    get() = throw Exception("impossible to get backgroundRes")
    set(value) = this.setBackgroundResource(value)

fun View.onClick(intervalMillis: Long = 100, onClick: ((View) -> Unit)) {
    setOnClickListener(DebouncingOnClickListener(intervalMillis, onClick))
}

fun View.onLongClick(onLongClick: ((View) -> Boolean)) {
    setOnLongClickListener(View.OnLongClickListener(onLongClick))
}

fun List<View>.onClick(intervalMillis: Long = 100, onClick: ((View) -> Unit)) {
    this.forEach { view -> view.onClick(intervalMillis, onClick) }
}

fun List<View>.onLongClick(onLongClick: ((View) -> Boolean)) {
    this.forEach { view -> view.onLongClick(onLongClick) }
}

var View.paddings: List<Int>   // (in px) RIGHT, TOP, LEFT, BOTTOM
    get() = listOf(paddingRight, paddingTop, paddingLeft, paddingBottom)
    set(value) {
        assert(value.size == 4)
        //                left      top      right     bottom
        this.setPadding(value[2], value[1], value[0], value[3])
    }

var View.padding: Int   // in px
    get() = throw Exception("to get paddings use paddings")
    set(value) { paddings = listOf(value, value, value, value) }

var View.paddingHorizontal: Int   // in px
    get() = throw Exception("to get paddings use paddings")
    set(value) { paddings = listOf(value, paddingTop, value, paddingBottom) }

var View.paddingVertical: Int   // in px
    get() = throw Exception("to get paddings use paddings")
    set(value) { paddings = listOf(paddingRight, value, paddingLeft, value) }

var View.paddingRight_: Int   // in px
    get() = this.paddingRight
    set(value) { paddings = listOf(value, paddingTop, paddingLeft, paddingBottom) }

var View.paddingTop_: Int   // in px
    get() = this.paddingTop
    set(value) { paddings = listOf(paddingRight, value, paddingLeft, paddingBottom) }

var View.paddingLeft_: Int   // in px
    get() = this.paddingLeft
    set(value) { paddings = listOf(paddingRight, paddingTop, value, paddingBottom) }

var View.paddingBottom_: Int   // in px
    get() = this.paddingBottom
    set(value) { paddings = listOf(paddingRight, paddingTop, paddingLeft, value) }

var View.margins: List<Int>   // (in px) RIGHT, TOP, LEFT, BOTTOM
    get() = listOf(marginRight, marginTop, marginLeft, marginBottom)
    set(value) {
        assert(value.size == 4)
        updateLayoutParams<MarginLayoutParams> {
            //           left      top      right     bottom
            setMargins(value[2], value[1], value[0], value[3])
        }
    }

var View.margin: Int   // in px
    get() = throw Exception("to get margins use margins")
    set(value) { margins = listOf(value, value, value, value) }

var View.marginHorizontal: Int   // in px
    get() = throw Exception("to get margins use margins")
    set(value) { margins = listOf(value, marginTop, value, marginBottom) }

var View.marginVertical: Int   // in px
    get() = throw Exception("to get margins use margins")
    set(value) { margins = listOf(marginRight, value, marginLeft, value) }

var View.marginRight_: Int   // in px
    get() = this.marginRight
    set(value) { margins = listOf(value, marginTop, marginLeft, marginBottom) }

var View.marginTop_: Int   // in px
    get() = this.marginTop
    set(value) { margins = listOf(marginRight, value, marginLeft, marginBottom) }

var View.marginLeft_: Int   // in px
    get() = this.marginLeft
    set(value) { margins = listOf(marginRight, marginTop, value, marginBottom) }

var View.marginBottom_: Int   // in px
    get() = this.marginBottom
    set(value) { margins = listOf(marginRight, marginTop, marginLeft, value) }
