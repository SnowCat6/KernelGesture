package gestureDetect.action.screen

import android.content.Context
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import gestureDetect.tools.GestureHW
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

/**
 * Screen OFF
 */
class ActionScreenOff(action: GestureAction) : ActionItem(action)
{
    override fun action(context: Context): String?
            = "screen.off"

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_screen_off)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_screen_off)

    override fun run(context: Context): Boolean {
        GestureHW(context).vibrate()
        action.su.exec("input keyevent 26")
        return true
    }
}