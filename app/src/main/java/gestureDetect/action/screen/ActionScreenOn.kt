package gestureDetect.action.screen

import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

/**
 * Screen ON
 */
class ActionScreenOn(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = "screen.on"

    override fun name(): String
            = context.getString(R.string.ui_screen_on)

    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_screen_on)

    override fun run(): Boolean {
        action.screenON()
        return true
    }
}