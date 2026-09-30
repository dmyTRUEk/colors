@file:Suppress("unused")

package dmytruek.colors.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

fun Context.getAttrDrawable(@AttrRes attributeId: Int): Drawable? {
    val drawableId = TypedValue().apply {
        this@getAttrDrawable.theme.resolveAttribute(attributeId, this, true)
    }.resourceId
    return ContextCompat.getDrawable(this, drawableId)
}

fun Context.getClipboardText(): String {
    val clipboardManager: ClipboardManager = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clipData: ClipData = clipboardManager.primaryClip ?: return ""
    return if (clipData.itemCount > 0) clipData.getItemAt(0).text?.toString().orEmpty() else ""
}

fun Context.setClipboardText(text: String) {
    val clipboardManager: ClipboardManager = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("colorsClipboard", text)
    clipboardManager.setPrimaryClip(clipData)
}
