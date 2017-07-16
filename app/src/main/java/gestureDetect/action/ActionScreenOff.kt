package gestureDetect.action

import SuperSU.ShellSU
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Screen OFF
 */
class ActionScreenOff(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = if (action.su.hasRootProcess()) "screen.off" else ""

    override fun name(): String
            = context.getString(R.string.ui_action_screen_off)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_screen_off)

    override fun run(): Boolean {
        action.hw.vibrate()
        action.su.exec("input keyevent 26")
        return true
    }
}