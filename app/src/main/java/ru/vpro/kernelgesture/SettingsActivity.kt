package ru.vpro.kernelgesture


import android.annotation.TargetApi
import android.content.*
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.text.TextUtils
import android.view.MenuItem
import android.content.Intent
import android.util.Log
import gesture.GestureService


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
        val gestureKeys:Array<Pair<String,Boolean>> = arrayOf(
                Pair("KEY_U", false),
                Pair("KEY_UP", false),
                Pair("KEY_DOWN", false),
                Pair("KEY_LEFT", false),
                Pair("KEY_RIGHT", false),
                Pair("KEY_O", false),
                Pair("KEY_E", false),
                Pair("KEY_M", false),
                Pair("KEY_L", false),
                Pair("KEY_W", false),
                Pair("KEY_S", false),
                Pair("KEY_V", false),
                Pair("KEY_Z", false)
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_gesture)
            setHasOptionsMenu(true)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            for ((first) in gestureKeys)
            {
                val preference = findPreference(first) ?: continue
                preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, false)
            }
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

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.context)
            val gestureEnable = sharedPreferences.getBoolean(preference.key, false)

            var action:String? = null
            try {
                action = sharedPreferences.getString("${preference.key}_ACTION", null)
            }catch (e:Exception){ }

            if (action == null || action.isEmpty()){
                preference.summary = preference.context.getString(R.string.ui_no_action)
            }else{
                preference.summary = action
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
  }
}
