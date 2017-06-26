package gesture.action

import android.content.Context
import android.graphics.drawable.Drawable

interface ActionItem
{
    val context:Context

    fun onStart() {}
    fun onStop() {}

    fun action(): String
    fun isAction(action: String): Boolean = action == action()
    fun name(): String = action()
    fun icon(): Drawable = context.getDrawable(android.R.color.transparent)
    fun run(): Boolean
}