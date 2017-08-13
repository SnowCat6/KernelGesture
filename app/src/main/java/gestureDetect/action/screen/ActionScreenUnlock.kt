package gestureDetect.action.screen

import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawable

/**
 * Screen ON
 */
class ActionScreenUnlock(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = "screen.unlock"

    override fun name(): String
            = context.getString(R.string.ui_screen_unlock)

    override fun icon(): Drawable
            = getDrawable(context, R.drawable.icon_screen_on)

    override fun run(): Boolean {
        action.hw.powerON()
        action.hw.screenUnlock()
        action.screenON()
        return true
    }
}