package gestureDetect.action

import SuperSU.ShellSU
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.GestureHW
import ru.vpro.kernelgesture.R

/**
 * Screen OFF
 */
class ActionScreenOff(action: GestureAction) : ActionItem(action)
{
    val su = ShellSU()

    override fun action(): String
            = if (su.hasRootProcess()) "screen.off" else ""

    override fun name(): String
            = context.getString(R.string.ui_action_screen_off)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_screen_off)

    override fun run(): Boolean {
        action.hw.vibrate()
        su.exec("input keyevent 26")
        return true
    }
}