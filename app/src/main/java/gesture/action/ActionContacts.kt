package gesture.action

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionContacts(action: GestureAction) : ActionApp(action)
{
    init{
        intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
    }

    override fun action(): String
            = super.action("application.contacts")

    override fun name(): String
            = context.getString(R.string.ui_action_contacts)
}