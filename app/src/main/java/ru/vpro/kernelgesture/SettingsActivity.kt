package ru.vpro.kernelgesture


import android.annotation.TargetApi
import android.content.*
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import android.content.Intent
import gesture.GestureDetect
import gesture.GestureService
import android.app.AlertDialog
import android.content.pm.ResolveInfo
import android.content.DialogInterface
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager





/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
   * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
   * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()

        startService(Intent(this, GestureService::class.java))
    }


    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
//        loadHeadersFromResource(R.xml.pref_headers, target)
        fragmentManager.beginTransaction().replace(android.R.id.content, GesturePreferenceFragment()).commit()
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GesturePreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GesturePreferenceFragment : PreferenceFragment()
    {
        val gestureKeys:Array<Pair<String,String>> = arrayOf(
                Pair("KEY_U",       "screen.on"),
                Pair("KEY_UP",      "phone"),
                Pair("KEY_DOWN",    "phone.contacts"),
                Pair("KEY_LEFT",    "music.prev|photo"),
                Pair("KEY_RIGHT",   "music.next|google"),
                Pair("KEY_O",       ""),
                Pair("KEY_E",       ""),
                Pair("KEY_M",       "music"),
                Pair("KEY_L",       ""),
                Pair("KEY_W",       "browser"),
                Pair("KEY_S",       "sound.off"),
                Pair("KEY_V",       ""),
                Pair("KEY_Z",       ""),
                Pair("KEY_VOLUMEUP",            ""),
                Pair("KEY_VOLUMEDOWN",          ""),
                Pair("GESTURE_DEFAULT_ACTION",  "")
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_gesture)
            setHasOptionsMenu(true)

            gestureKeys.forEach { (first, second) ->
                val preference = findPreference(first) ?: return@forEach

                val action:String? = GestureDetect.getAction(activity, first)
                if (action == null) GestureDetect.setAction(activity, first, second)

                preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, false)

                preference.onPreferenceClickListener = sBindPreferenceClickListener
            }

            val preferenceEnable = findPreference("GESTURE_ENABLE")
            preferenceEnable.onPreferenceChangeListener = sBindPreferenceEnableListener
            sBindPreferenceEnableListener.onPreferenceChange(preferenceEnable, GestureDetect.getAllEnable(activity))

            val preferenceNotify = findPreference("GESTURE_NOTIFY")
            preferenceNotify.onPreferenceChangeListener = sBindPreferenceNotifyListenerListener
            sBindPreferenceNotifyListenerListener.onPreferenceChange(preferenceNotify, false)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->

            val action:String? = GestureDetect.getAction(preference.context, preference.key)

            if (action == null){
                preference.summary = preference.context.getString(R.string.ui_no_action)
            }else{
                val pm = preference.context.getPackageManager()
                var ai: ApplicationInfo?
                try {
                    ai = pm.getApplicationInfo(action, 0)
                } catch (e: Exception) {
                    ai = null
                }

                val applicationName = if (ai != null) pm.getApplicationLabel(ai) else action
                preference.summary = applicationName
            }

            true
        }

        private val sBindPreferenceEnableListener = Preference.OnPreferenceChangeListener { preference, value ->

            val preferenceEnable = preference.getPreferenceManager().findPreference("GESTURE_GROUP")
            GestureDetect.setAllEnable(preference.context, value as Boolean)
            preferenceEnable.isEnabled = value

            true
        }

        private val sBindPreferenceNotifyListenerListener = Preference.OnPreferenceChangeListener { preference, value ->

            var notify:String? = null
            if (value is String) notify = value

            try {
                if (notify == null) {
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.context)
                    notify = sharedPreferences.getString("GESTURE_NOTIFY", null)
                }
            }catch (e:Exception){}

            if (notify == null || notify.isEmpty()) {
                preference.summary = preference.context.getString(R.string.ui_no_notify)
            }else{

                val ringtone = RingtoneManager.getRingtone(preference.context, Uri.parse(notify))
                when (ringtone) {
                    null -> preference.summary = preference.context.getString(R.string.ui_sound_default)
                    else -> preference.summary = ringtone.getTitle(preference.context)
                }
            }

            true
        }
        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        private val sBindPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->

            val pm =  preference.context.getPackageManager()

            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pkgAppsList = pm.queryIntentActivities(mainIntent, 0)
/*
            var items = arrayOf("none", "screen.on", "phone", "phone.contacts")
            var items_action = arrayOf("none", "screen.on", "phone", "phone.contacts")
            pkgAppsList.forEach {
                items_action += it.activityInfo.packageName
                items += pm.getApplicationLabel(it.activityInfo.applicationInfo).toString()
            }
*/
            val builder = AlertDialog.Builder(preference.context)
            builder.setTitle(preference.context.getString(R.string.iu_choose_action))
 /*
            builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->

                var value =  items_action[item]
                if (value == "none") value = ""

                GestureDetect.setAction(preference.context, preference.key, value)
                sBindPreferenceNotifyListenerListener.onPreferenceChange(preference, value)
            })
*/
            val adapter = BoxAdapter(preference, pkgAppsList)
            builder.setAdapter(adapter, onClickListener)

            val alert = builder.create()
            alert.show()

            true
        }

        val onClickListener = DialogInterface.OnClickListener() { dialogInterface: DialogInterface, i: Int ->

            val lw = (dialogInterface as AlertDialog).listView
            val adapter = lw.adapter as BoxAdapter
            val item = adapter.getThisItem(i)

            var value =  item.activityInfo.packageName
            if (value == "none") value = ""

            val preference = adapter.getPreference()
            GestureDetect.setAction(lw.context, preference.key, value)
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value)
        }


        class BoxAdapter internal constructor(
                internal val preference: Preference,
                internal val objects: List<ResolveInfo>) : BaseAdapter()
        {
            val lInflater = preference.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val pm = preference.context.packageManager

            fun getPreference():Preference = preference

            override fun getCount(): Int {
                return objects.size
            }
            override fun getItem(position: Int): Any {
                return objects[position]
            }
            // id по позиции
            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            // пункт списка
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
            {
                // используем созданные, но не используемые view
                val view: View
                if (convertView == null) {
                    view = lInflater.inflate(R.layout.adapter_choose_item, parent, false)
                }else view = convertView

                val thisItem = getThisItem(position)
                (view.findViewById(R.id.title) as TextView)
                        .setText(pm?.getApplicationLabel(thisItem.activityInfo.applicationInfo))

                return view
            }

            // товар по позиции
            internal fun getThisItem(position: Int): ResolveInfo {
                return getItem(position) as ResolveInfo
            }
        }
    }
}
