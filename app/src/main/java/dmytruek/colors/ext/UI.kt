@file:Suppress("FunctionName", "unused")

package dmytruek.colors.ext

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dmytruek.colors.R

const val fill = -1
const val wrap = -2

const val weightOccupyMin: Float = 0f
const val weightOccupyMax: Float = 1f

const val visibilityGone = GONE
const val visibilityVisible = VISIBLE
const val visibilityInvisible = INVISIBLE

const val gravityNone = Gravity.NO_GRAVITY
const val gravityCenter = Gravity.CENTER
const val gravityCenterHorizontal = Gravity.CENTER_HORIZONTAL
const val gravityCenterVertical   = Gravity.CENTER_VERTICAL
const val gravityBottom = Gravity.BOTTOM
const val gravityTop    = Gravity.TOP
const val gravityLeft   = Gravity.LEFT
const val gravityRight  = Gravity.RIGHT
const val gravityStart  = Gravity.START
const val gravityEnd    = Gravity.END

const val gravityBottomLeft  = gravityBottom or gravityLeft
const val gravityBottomRight = gravityBottom or gravityRight
const val gravityTopLeft     = gravityTop or gravityLeft
const val gravityTopRight    = gravityTop or gravityRight

inline fun Context.Row(init: LinearLayout.() -> Unit): LinearLayout =
    LinearLayout(this).apply {
        init()
        orientation = LinearLayout.HORIZONTAL
    }

inline fun Context.Column(init: LinearLayout.() -> Unit): LinearLayout =
    LinearLayout(this).apply {
        init()
        orientation = LinearLayout.VERTICAL
    }

inline fun Context.Frame(init: FrameLayout.() -> Unit): FrameLayout =
    FrameLayout(this).apply(init)

inline fun Context.Text(init: AppCompatTextView.() -> Unit): AppCompatTextView =
    AppCompatTextView(this).apply(init)

inline fun Context.EditText(init: AppCompatEditText.() -> Unit): AppCompatEditText =
    AppCompatEditText(this).apply(init)

inline fun Context.Image(init: AppCompatImageView.() -> Unit): AppCompatImageView =
    AppCompatImageView(this).apply(init)

inline fun Context.ImageButton(init: AppCompatImageButton.() -> Unit): AppCompatImageButton =
    AppCompatImageButton(this).apply(init)

inline fun Context.Button(init: AppCompatButton.() -> Unit): AppCompatButton =
    AppCompatButton(this).apply(init)

inline fun Context.RecyclerView(init: RecyclerView.() -> Unit): RecyclerView =
    RecyclerView(this).apply(init)

inline fun Context.Slider(init: AppCompatSeekBar.() -> Unit): AppCompatSeekBar =
    AppCompatSeekBar(this).apply(init)

inline fun Context.Switch(init: SwitchCompat.() -> Unit): SwitchCompat =
    SwitchCompat(this).apply(init)

inline fun Context.AlertDialogBuilder(init: AlertDialog.Builder.() -> Unit): AlertDialog.Builder =
    AlertDialog.Builder(this).apply(init)

inline fun Context.Scroll(init: ScrollView.() -> Unit): ScrollView =
    ScrollView(this).apply(init)

inline fun Context.Divider(init: View.() -> Unit): View =
    View(this).apply {
        layoutParams = ViewGroup.LayoutParams(fill, 1.dpToPx(context))
        //backgroundColor = ContextCompat.getColor(context, R.color.colorBgRootLayout)
        init()
    }

inline fun frameParams(
    width: Int,
    height: Int,
    gravity: Int = gravityNone,
    margins: FrameLayout.LayoutParams.() -> Unit = {},
): FrameLayout.LayoutParams = FrameLayout.LayoutParams(width, height, gravity).apply(margins)

inline fun linearParams(
    width: Int,
    height: Int,
    weight: Float = 0f,
    margins: LinearLayout.LayoutParams.() -> Unit = {},
): LinearLayout.LayoutParams = LinearLayout.LayoutParams(width, height, weight).apply(margins)

fun Context.showAlertDialogForInputText(
    textTitle: String,
    textHint: String = "",
    textDefault: String = "",
    onClickOK: (String) -> Unit,
) {
    val editText = this.EditText {
        text_ = textDefault
        hint = textHint
    }
    val layout = this.Frame {
        padding = 10.dpToPx(this@showAlertDialogForInputText)
        addView(editText, fill, wrap)
    }
    this.AlertDialogBuilder {
        setTitle(textTitle)
        setView(layout)
        setPositiveButton("OK") { _, _ -> onClickOK(editText.text_) }
        setNegativeButton("Cancel") { _, _ -> /*  nothing ;)  */ }
    }.create().show()
}
