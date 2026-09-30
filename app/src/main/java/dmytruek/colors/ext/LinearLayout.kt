@file:Suppress("unused")

package dmytruek.colors.ext

import android.view.ViewGroup
import android.widget.LinearLayout

var LinearLayout.width_: Int    // in ? (px?)
    get() = this.layoutParams?.width ?: 0
    set(value) {
        val params: ViewGroup.LayoutParams? = this.layoutParams
        params?.width = value
        this.layoutParams = params
    }

var LinearLayout.height_: Int    // in ? (px?)
    get() = this.layoutParams?.height ?: 0
    set(value) {
        val params: ViewGroup.LayoutParams? = this.layoutParams
        params?.height = value
        this.layoutParams = params
    }
