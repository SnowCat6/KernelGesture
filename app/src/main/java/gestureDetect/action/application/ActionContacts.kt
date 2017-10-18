package gestureDetect.action.application

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionContacts(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(context: Context): Boolean
            = super.onDetect(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))

    override fun action(context: Context): String?
            = super.action(context, "application.contacts")

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_contacts)
}