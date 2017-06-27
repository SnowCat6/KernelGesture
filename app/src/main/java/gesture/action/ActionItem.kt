package gesture.action

import android.R
import android.graphics.drawable.Drawable
import gesture.GestureAction

abstract class ActionItem(val action: GestureAction)
{
    open fun onStart() {}
    open fun onStop() {}

    abstract fun action(): String
    open fun isAction(action: String): Boolean = action == action()
    open fun name(): String = action()
    open fun icon(): Drawable = action.context.getDrawable(R.color.transparent)
    abstract fun run(): Boolean
}