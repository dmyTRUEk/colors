@file:Suppress("unused")

package dmytruek.colors.ext

import android.widget.TextView
import androidx.annotation.ColorInt

var TextView.textColor: Int     // 0xAARRGGBB
    get() = this.currentTextColor
    set(@ColorInt value) = this.setTextColor(value)

var TextView.text_: String
    get() = this.text?.toString().orEmpty()
    set(value) {
        this.text = value
    }
