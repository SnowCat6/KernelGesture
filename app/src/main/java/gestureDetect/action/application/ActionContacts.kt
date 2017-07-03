package gestureDetect.action.application

import android.content.Intent
import android.provider.ContactsContract
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionContacts(action: GestureAction) : ActionApp(action)
{
    override fun onDetect(): Boolean
            = super.onDetect(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))

    override fun action(): String
            = super.action("application.contacts")

    override fun name(): String
            = context.getString(R.string.ui_action_contacts)
}