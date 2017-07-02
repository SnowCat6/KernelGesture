package gesture.action

import android.graphics.drawable.Drawable
import gesture.GestureAction
import ru.vpro.kernelgesture.R

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
            = context.getDrawable(R.drawable.icon_screen_on)

    override fun run(): Boolean {
        action.screenON()
        return true
    }
}