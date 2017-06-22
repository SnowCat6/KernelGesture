package ru.vpro.kernelgesture


import android.annotation.TargetApi
import android.content.*
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.content.Intent
import gesture.GestureDetect
import gesture.GestureService
import android.app.AlertDialog
import android.content.pm.ResolveInfo
import android.content.DialogInterface
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import com.google.android.gms.ads.AdListener
import kotlin.concurrent.thread

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import android.os.Looper.getMainLooper
import ru.vpro.kernelgesture.SettingsActivity.GesturePreferenceFragment.Companion.getItemInstance


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
class SettingsActivity : AppCompatPreferenceActivity()
{
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()

        startService(Intent(this, GestureService::class.java))

        mInterstitialAd = InterstitialAd(this);
        mInterstitialAd?.adUnitId = "ca-app-pub-5004205414285338/5364605548";
        mInterstitialAd?.loadAd(AdRequest.Builder().build())

        mInterstitialAd?.adListener = object : AdListener() {
            override fun onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd?.loadAd(AdRequest.Builder().build())
            }
        }
    }


    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
//        supportActionBar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when(item?.itemId)
        {
            R.id.menu_adv ->
            {
                if (mInterstitialAd?.isLoaded != null && mInterstitialAd!!.isLoaded) {
                    mInterstitialAd?.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }
        }
        return super.onOptionsItemSelected(item)
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
    override fun onBuildHeaders(target: List<Header>) {
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
        override fun onResume() {
            super.onResume()
            thisFragment = this
        }

        override fun onCreate(savedInstanceState: Bundle?)
        {
            val gestureItems:Array<GestureItem> = arrayOf(
                    GestureItem("KEY_U",       "screen.on"),
                    GestureItem("KEY_UP",      ""),
                    GestureItem("KEY_DOWN",    ""),
                    GestureItem("KEY_LEFT",    ""),
                    GestureItem("KEY_RIGHT",   ""),
                    GestureItem("KEY_O",       ""),
                    GestureItem("KEY_E",       ""),
                    GestureItem("KEY_M",       ""),
                    GestureItem("KEY_L",       ""),
                    GestureItem("KEY_W",       ""),
                    GestureItem("KEY_S",       ""),
                    GestureItem("KEY_V",       ""),
                    GestureItem("KEY_Z",       ""),
                    GestureItem("KEY_VOLUMEUP",            ""),
                    GestureItem("KEY_VOLUMEDOWN",          ""),
                    GestureItem("GESTURE_DEFAULT_ACTION",  "")
            )

            commonGestureItems = gestureItems
            thisFragment = this

            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_gesture)
            setHasOptionsMenu(true)

            gestureItems.forEach {
                val preference = findPreference(it.key) ?: return@forEach

                preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
                preference.onPreferenceClickListener = sBindPreferenceClickListener

                preference.onPreferenceChangeListener.onPreferenceChange(preference, it.enable)
            }

            val preferenceEnable = findPreference("GESTURE_ENABLE")
            preferenceEnable.onPreferenceChangeListener = sBindPreferenceEnableListener
            preferenceEnable.onPreferenceChangeListener.onPreferenceChange(preferenceEnable, GestureDetect.getAllEnable(activity))

            val preferenceNotify = findPreference("GESTURE_NOTIFY")
            preferenceNotify.onPreferenceChangeListener = sBindPreferenceNotifyListenerListener
            preferenceNotify.onPreferenceChangeListener.onPreferenceChange(preferenceNotify, false)

            thread{
                val mainHandler = Handler(getMainLooper())
                val bRoot = GestureDetect.canAppWork()
                mainHandler.post {
                    updateRootAccess(bRoot)
                }

                if (bRoot) {
                    val support = GestureDetect.getInstance().getSupport()
                    mainHandler.post {
                        updateGesturesDetect(support)
                    }
                }
            }
        }
        private fun updateGesturesDetect(support:Array<String>)
        {
            if (support.contains("GESTURE")) return

            val dlgAlert = AlertDialog.Builder(activity)
            dlgAlert.setTitle(getString(R.string.ui_alert_gs_title))

            if (support.contains("KEYS")) {
                dlgAlert.setMessage(getString(R.string.ui_alert_gs_message_keys))
            } else {
                dlgAlert.setMessage(getString(R.string.ui_alert_gs_message_wo_keys))
            }

            dlgAlert.create().show()
        }

        private fun updateRootAccess(bRootExists:Boolean)
        {
            if (bRootExists) {
                val p = findPreference("pref_ROOT")
                if (p != null) {
                    preferenceScreen?.removePreference(p)
                }
            }else{
                val p = findPreference("GESTURE_ENABLE") as SwitchPreference
                p.isChecked = false
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
        inner class GestureItem(
                val key:String,
                var defaultAction:String
        )
        {
            var applicationInfo:ApplicationInfo? = null

            init{
                if (action != null && applicationInfo == null) {
                    applicationInfo = getAppInfo(action!!)
                }
            }

            var action:String?
                get() {
                   val a = GestureDetect.getAction(activity, key)
                    if (a != null) return a

                    action = defaultAction
                    return defaultAction
                }
                set(value) {
                    _actionName = ""
                    _icon = null
                    applicationInfo = if (value != null) getAppInfo(value) else null
                    GestureDetect.setAction(activity, key, value)
                }
            var enable:Boolean
                get() = GestureDetect.getEnable(activity, key)
                set(value){
                    _actionName = ""
                    GestureDetect.setEnable(activity, key, value)
                }

            var _actionName:String = ""
            val actionName:String
                get() {
                    if (_actionName.isEmpty())
                    {
                        if (applicationInfo != null) {
                            _actionName = UI.actionName(activity, applicationInfo)
                        }else{
                            if (action != null && action == "" && key == "GESTURE_DEFAULT_ACTION") {
                                _actionName = getString(R.string.ui_no_action)
                            }else {
                                if (action == null || (action == "" && !enable)){
                                    _actionName = getString(R.string.ui_no_action)
                                }else {
                                    _actionName = UI.actionName(activity, action)
                                }
                            }
                        }
                    }
                    return _actionName
                }

            var _icon:Drawable? = null
            val icon:Drawable?
                get(){
                    if (_icon != null) return _icon
                    if (applicationInfo != null){
                        _icon = UI.actionIcon(activity, applicationInfo)
                    }else{
                        _icon = UI.actionIcon(activity, action)
                    }
                    return _icon
                }

            private fun getAppInfo(action:String):ApplicationInfo?
            {
                try {
                    return activity.packageManager.getApplicationInfo(action, 0)
                } catch (e: Exception) {}

                return null
            }
        }
        companion object
        {
            private var commonGestureItems = emptyArray<GestureItem>()
            fun getItemInstance(key:String):GestureItem?
                    = commonGestureItems.find { it.key == key }
        }
    }

    companion object {
        var thisFragment:GesturePreferenceFragment? = null
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->

            value as Boolean

            val gestureItem = getItemInstance(preference.key)
            if (gestureItem != null) {
                gestureItem.enable = value
                preference.summary = gestureItem.actionName
                preference.icon = gestureItem.icon
            }

            true
        }

        private val sBindPreferenceEnableListener = Preference.OnPreferenceChangeListener { preference, value ->

            value as Boolean

            preference.getPreferenceManager().findPreference("GESTURE_GROUP").isEnabled = value
            preference.getPreferenceManager().findPreference("GESTURE_GROUP_ADD").isEnabled = value

            GestureDetect.setAllEnable(preference.context, value)

            if (value == true)
            {
                val mainHandler = Handler(preference.context.getMainLooper())
                thread{
                    val bRoot = GestureDetect.canAppWork()
                    mainHandler.post {
                        if (bRoot){
                            val p = preference.preferenceManager.findPreference("pref_ROOT")
                            if (p != null) {
                                thisFragment?.preferenceScreen?.removePreference(p)
                            }
                        }
                        val p = preference as SwitchPreference
                        p.isChecked = bRoot
                    }
                }
            }

            true
        }

        private val sBindPreferenceNotifyListenerListener = Preference.OnPreferenceChangeListener { preference, value ->

            var notify:String? = if (value is String) value else null

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

            var items:List<Any> = emptyList()
            items += "none"
            items += "screen.on"
            pkgAppsList.forEach { items += it.activityInfo.applicationInfo }

            val builder = AlertDialog.Builder(preference.context)
            builder.setTitle(preference.context.getString(R.string.iu_choose_action))

            val adapter = BoxAdapter(preference, items)
            builder.setAdapter(adapter, onClickListener)

            val alert = builder.create()
            alert.show()

            true
        }

        val onClickListener = DialogInterface.OnClickListener() { dialogInterface: DialogInterface, i: Int ->

            val lw = (dialogInterface as AlertDialog).listView
            val adapter = lw.adapter as BoxAdapter
            val item = adapter.getItem(i)
            val preference = adapter.getPreference()

            var value =  UI.actionAction(preference.context, item)
            if (value == "none") value = ""

            val gestureItem = getItemInstance(preference.key)
            gestureItem?.action = value

            val p = preference as TwoStatePreference?
            p?.isChecked = value.isNotEmpty()
            preference.onPreferenceChangeListener.onPreferenceChange(preference, p?.isChecked)
        }

        class BoxAdapter internal constructor(
                internal val preference: Preference,
                internal val objects: List<Any>) : BaseAdapter()
        {
            val lInflater = preference.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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

                val thisItem = getItem(position)
                (view.findViewById(R.id.title) as TextView).text = UI.actionName(preference.context, thisItem)

                val icon = UI.actionIcon(preference.context, thisItem)
                (view.findViewById(R.id.icon) as ImageView).setImageDrawable(icon)

                return view
            }

            // товар по позиции
            internal fun getThisItem(position: Int): ResolveInfo {
                return getItem(position) as ResolveInfo
            }
        }
        object UI
        {
            fun actionName(context:Context, item:Any?):String
            {
                when(item){
                    is ApplicationInfo -> return context.packageManager
                            .getApplicationLabel(item)
                            .toString()

                    "" ->  return context.getString(R.string.ui_default_action)
                    "none"  -> return context.getString(R.string.ui_no_action)
                    "screen.on" -> return context.getString(R.string.ui_screen_on)
                }
                return ""
            }
            fun actionAction(context:Context, item:Any?):String
            {
                when(item){
                    is ApplicationInfo -> return item.packageName
                    is String -> return item
                }
                return ""
            }
            fun actionIcon(context:Context, item:Any?): Drawable
            {
                when(item){
                    is ApplicationInfo -> {
                        val pm = context.packageManager
                        return pm.getApplicationIcon(item)
                    }
                    "screen.on" -> {
                        return context.getDrawable(R.drawable.icon_screen_on)
                    }
                }
                return context.getDrawable(android.R.color.transparent)
            }

        }
    }
}
