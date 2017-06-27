package gesture.action

import android.content.Context
import android.graphics.drawable.Drawable

abstract class ActionItem(val context:Context)
{
    open fun onStart() {}
    open fun onStop() {}

    abstract fun action(): String
    open fun isAction(action: String): Boolean = action == action()
    open fun name(): String = action()
    open fun icon(): Drawable = context.getDrawable(android.R.color.transparent)
    abstract fun run(): Boolean
}