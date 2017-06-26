package gesture

import android.content.Context
import gesture.action.*


class GestureAction(val context:Context)
{
    private val allActions = arrayOf(
            ActionScreenOn(context),
            ActionGoogleNow(context),
            ActionSpeechTime(context),
            ActionSpeechBattery(context),
            ActionWebBrowser(context),
            ActionCamera(context)
    )

    fun onStart() {
       allActions.forEach { it.onStart() }
    }
    fun onStop(){
        allActions.forEach { it.onStop() }
    }

    fun getAction(action: String): ActionItem?
            = allActions.firstOrNull { it.isAction(action) }

    fun getActions(): Array<ActionItem>
            = allActions

}
