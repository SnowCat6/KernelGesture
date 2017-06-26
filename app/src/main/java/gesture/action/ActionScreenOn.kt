package gesture.action

import android.content.Context
import android.graphics.drawable.Drawable
import gesture.GestureService
import ru.vpro.kernelgesture.R

/**
 * Screen ON
 */
class ActionScreenOn(override val context: Context) : ActionItem {
    override fun action(): String = "screen.on"

    override fun name(): String
            = context.getString(R.string.ui_screen_on)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_screen_on)

    override fun run(): Boolean {
        GestureService.UI.screenON(context)
        return true
    }
}