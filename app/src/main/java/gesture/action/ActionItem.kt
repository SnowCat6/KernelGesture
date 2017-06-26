package gesture

import android.content.Context
import android.graphics.drawable.Drawable

interface ActionItem {
    fun onStart(context: Context) {}
    fun action(): String
    fun isAction(context: Context, action: String): Boolean = action == action()
    fun name(context: Context): String = action()
    fun icon(context: Context): Drawable = context.getDrawable(android.R.color.transparent)
    fun run(context: Context): Boolean
}