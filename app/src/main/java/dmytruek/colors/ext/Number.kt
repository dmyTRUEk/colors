@file:Suppress("unused")

package dmytruek.colors.ext

import android.content.Context
import kotlin.math.abs
import kotlin.math.roundToInt

fun Number.dpToPx(context: Context): Int = (this.toDouble() * context.resources.displayMetrics.density).roundToInt()
fun Number.dpToPxFloat(context: Context): Float = (this.toDouble() * context.resources.displayMetrics.density).toFloat()

val Number.abs: Double get() = abs(this.toDouble())
