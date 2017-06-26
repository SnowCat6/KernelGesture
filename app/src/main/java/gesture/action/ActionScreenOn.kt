package gesture.action

import android.content.Context
import android.graphics.drawable.Drawable
import gesture.GestureService
import ru.vpro.kernelgesture.R

/**
 * Screen ON
 */
class ActionScreenOn : ActionItem {
    override fun action(): String = "screen.on"

    override fun name(context: Context): String
            = context.getString(R.string.ui_screen_on)

    override fun icon(context: Context): Drawable
            = context.getDrawable(R.drawable.icon_screen_on)

    override fun run(context: Context): Boolean {
        GestureService.UI.screenON(context)
        return true
    }
}