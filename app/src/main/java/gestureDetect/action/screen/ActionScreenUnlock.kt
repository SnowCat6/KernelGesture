package gestureDetect.action.screen

import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.GestureService
import gestureDetect.action.ActionItem
import ru.vpro.kernelgesture.R

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
            = context.getDrawable(R.drawable.icon_screen_on)

    override fun run(): Boolean {
        action.screenON()
        action.hw.screenUnlock()
        return true
    }
}