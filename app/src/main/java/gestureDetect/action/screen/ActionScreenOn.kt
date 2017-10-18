package gestureDetect.action.screen

import android.content.Context
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
    override fun action(context: Context): String?
            = "screen.on"

    override fun name(context: Context): String?
            = context.getString(R.string.ui_screen_on)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_screen_on)

    override fun run(context: Context): Boolean {
        action.screenON(context)
        return true
    }
}