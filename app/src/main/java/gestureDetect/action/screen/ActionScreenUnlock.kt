package gestureDetect.action.screen

import android.content.Context
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import gestureDetect.tools.GestureHW
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

/**
 * Screen ON
 */
class ActionScreenUnlock(action: GestureAction) : ActionItem(action)
{
    override fun action(context: Context): String?
            = "screen.unlock"

    override fun name(context: Context): String?
            = context.getString(R.string.ui_screen_unlock)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_screen_on)

    override fun run(context: Context): Boolean {
        val hw = GestureHW(context)
        hw.powerON()
        hw.screenUnlock()
        action.screenON(context)
        return true
    }
}