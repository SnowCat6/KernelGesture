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
import gesture.action.ActionItem
import gesture.GestureAction



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

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd?.adUnitId = "ca-app-pub-5004205414285338/5364605548"
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
//        supportActionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_directions)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when(item?.itemId)
        {
            R.id.menu_adv ->
            {
                if (mInterstitialAd?.isLoaded != null && mInterstitialAd!!.isLoaded) {
                    mInterstitialAd?.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
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
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_gesture)
            setHasOptionsMenu(true)

            thisFragment = this
            commonGestureItems = arrayOf(
                    GestureItem("KEY_U",       "screen.on"),
                    GestureItem("KEY_UP",      "com.android.dialer"),
                    GestureItem("KEY_DOWN",    "com.android.contacts"),
                    GestureItem("KEY_LEFT",    ""),
                    GestureItem("KEY_RIGHT",   ""),
                    GestureItem("KEY_O",       ""),
                    GestureItem("KEY_E",       ""),
                    GestureItem("KEY_M",       "com.android.email"),
                    GestureItem("KEY_L",       ""),
                    GestureItem("KEY_W",       "application.browser"),
                    GestureItem("KEY_S",       "application.camera"),
                    GestureItem("KEY_V",       ""),
                    GestureItem("KEY_Z",       ""),
                    GestureItem("KEY_VOLUMEUP",            ""),
                    GestureItem("KEY_VOLUMEDOWN",          ""),
                    GestureItem("KEY_PROXIMITY",          "speech.time"),
                    GestureItem("GESTURE_DEFAULT_ACTION",  "")
            )

            commonGestureItems.forEach {

                with(findPreference(it.key))
                {
                    icon = it.icon
                    onPreferenceChangeListener = sBindGestureChangeListener
                    onPreferenceClickListener = sBindGestureActionListener
                    onPreferenceChangeListener.onPreferenceChange(this, it.enable)
                }
            }

            with(findPreference("GESTURE_ENABLE")){
                onPreferenceChangeListener = sBindAllEnableListener
                onPreferenceChangeListener.onPreferenceChange(this, GestureDetect.getAllEnable(activity))
            }

            with(findPreference("GESTURE_NOTIFY")){
                onPreferenceChangeListener = sBindNotifyListener
                onPreferenceChangeListener.onPreferenceChange(this, null)
            }
        }
        fun updateGesturesDetect(support:Array<String>, bShowAlert:Boolean)
        {
            var titles = emptyArray<String>()
            var alertMessage:String? = null
            val bAllEnable = GestureDetect.getAllEnable(activity)

            val bGesture = support.contains("GESTURE")
            findPreference("GESTURE_GROUP").isEnabled = /*bGesture*/ GestureDetect.SU.hasRootProcess() && bAllEnable

            if (bGesture){
                titles += getString(R.string.ui_title_gestures)
            }

            val bKeys = support.contains("KEYS")
            findPreference("KEY_GROUP").isEnabled = bKeys && bAllEnable

            if (bKeys){
                titles += getString(R.string.ui_title_keys)
            }

            val bProximity = support.contains("PROXIMITY")
            findPreference("SENSOR_GROUP").isEnabled = bProximity && bAllEnable

            if (bProximity){
                titles += getString(R.string.ui_title_gesture)
            }
            if (!titles.isEmpty()){
                val actionBar = (activity as AppCompatPreferenceActivity).supportActionBar
                actionBar.subtitle = titles.joinToString(", ")
            }

            if (titles.isEmpty())
                alertMessage = getString(R.string.ui_alert_gs_message_wo_keys)
            else
                if (!support.contains("GESTURE"))
                    alertMessage = getString(R.string.ui_alert_gs_message_keys) + " " + titles.joinToString(", ")

            if (alertMessage == null || !bShowAlert) return

            with(AlertDialog.Builder(activity))
            {
                setTitle(getString(R.string.ui_alert_gs_title))
                setMessage(alertMessage)
                create().show()
            }
        }

        fun updateRootAccess(bRootExists:Boolean)
        {
            val actionBar = (activity as AppCompatPreferenceActivity).supportActionBar
            if (bRootExists) {
                val p = findPreference("pref_ROOT")
                if (p != null) {
                    preferenceScreen?.removePreference(p)
                }
                actionBar.subtitle = ""
            }else{
                actionBar.subtitle = getString(R.string.ui_title_no_root)
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
                    if (defaultAction.isNotEmpty()) enable = true
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
                    _icon = null
                    GestureDetect.setEnable(activity, key, value)
                }

            var _actionName:String = ""
            val actionName:String
                get() {
                    if (_actionName.isEmpty())
                    {
                        if (applicationInfo != null) {
                            _actionName = UI.name(activity, applicationInfo)
                        }else{
                            if (action != null && action == "" && key == "GESTURE_DEFAULT_ACTION") {
                                _actionName = getString(R.string.ui_no_action)
                            }else {
                                if (action == null || (action == "" && !enable)){
                                    _actionName = getString(R.string.ui_no_action)
                                }else {
                                    _actionName = UI.name(activity, action)
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
                        _icon = UI.icon(activity, applicationInfo)
                    }else{
                        _icon = UI.icon(activity, action)
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
    }

    companion object
    {
        var thisFragment:GesturePreferenceFragment? = null

        private var commonGestureItems = emptyArray<GesturePreferenceFragment.GestureItem>()
        fun getItemInstance(key:String): GesturePreferenceFragment.GestureItem?
                = commonGestureItems.find { it.key == key }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindGestureChangeListener = Preference.OnPreferenceChangeListener { preference, value ->

            value as Boolean

            val gestureItem = getItemInstance(preference.key)
            if (gestureItem != null) {
                gestureItem.enable = value
                preference.summary = gestureItem.actionName
            }

            true
        }

        private val sBindAllEnableListener = Preference.OnPreferenceChangeListener { preference, value ->

            value as Boolean
            preference as TwoStatePreference
            val context = preference.context

            GestureDetect.setAllEnable(context, value)
/*
            with( preference.preferenceManager)
            {
                findPreference("GESTURE_GROUP").isEnabled = value
                findPreference("GESTURE_GROUP_ADD").isEnabled = value
            }
*/
            val mainHandler = Handler(context.mainLooper)
            val bHasRoot = GestureDetect.SU.hasRootProcess()

            if (value == true && !bHasRoot)
            {
                thisFragment?.updateRootAccess(bHasRoot)
                thread{
                    val bRoot = GestureDetect.SU.checkRootAccess()
                    val support = GestureDetect(context).getSupport()
                    mainHandler.post {
                        thisFragment?.updateRootAccess(bRoot)
                        thisFragment?.updateGesturesDetect(support, true)
                    }
                }
            }

            val support = GestureDetect(context).getSupport()
            thisFragment?.updateGesturesDetect(support, false)

            if (value) {
                context.startService(Intent(context, GestureService::class.java))
            }else{
                context.stopService(Intent(context, GestureService::class.java))
            }

            true
        }

        private val sBindNotifyListener = Preference.OnPreferenceChangeListener { preference, value ->

            var notify:String? = value as String?

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

        private val sBindGestureActionListener = Preference.OnPreferenceClickListener { preference ->

            val adapter = BoxAdapter(preference)

            with(AlertDialog.Builder(preference.context))
            {
                setTitle(preference.context.getString(R.string.iu_choose_action))
                setAdapter(adapter, onClickListener)
                create().show()
            }

            true
        }

        val onClickListener = DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->

            val adapter = (dialogInterface as AlertDialog).listView.adapter as BoxAdapter
            val item = adapter.getItem(i) as? AppListItem ?: return@OnClickListener

            val preference = adapter.preference as TwoStatePreference
            val itemAction =  UI.action(preference.context, item)

            if (BuildConfig.DEBUG) {
                Log.d("Set gesture action", itemAction)
            }

            val gestureItem = getItemInstance(preference.key) ?: return@OnClickListener
            with(gestureItem)
            {
                action = itemAction
                enable = itemAction.isNotEmpty()

                preference.isChecked = enable
                preference.icon = icon
                preference.onPreferenceChangeListener.onPreferenceChange(preference, enable)
            }
        }

        class AppListItem(val action:String, val name:String, val icon:Drawable)
        {
            constructor(context:Context, action:Any) :
                    this(UI.action(context, action),
                            UI.name(context, action),
                            UI.icon(context, action))
            constructor(context:Context, applicationInfo: ApplicationInfo) :
                    this(UI.action(context, applicationInfo.packageName),
                            UI.name(context, applicationInfo),
                            UI.icon(context, applicationInfo))
        }

        class BoxAdapter internal constructor(
                internal val preference: Preference) : BaseAdapter()
        {
            internal var objects = listOf<Any>("wait")
            internal val lInflater = preference.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            internal val context = preference.context

            init{

                thread{
                    val pm =  preference.context.packageManager

                    val mainIntent = Intent(Intent.ACTION_MAIN, null)
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val pkgAppsList = pm.queryIntentActivities(mainIntent, 0)

                    var items:List<Any> = emptyList()
                    items += AppListItem(preference.context, "none")

                    GestureAction(context).getActions()
                            .filter { it.action()!="" }
                            .forEach { items += AppListItem(context, it) }

                    items += "-"

                    pkgAppsList.forEach {
                        items += AppListItem(context, it.activityInfo.applicationInfo)
                    }

                    Handler(context.mainLooper).post {
                        objects = items
                        notifyDataSetChanged()
                    }
                }
            }

            //  Количество объектов
            override fun getCount(): Int = objects.size
            //  Объект
            override fun getItem(position: Int): Any = objects[position]
            // id по позиции
            override fun getItemId(position: Int): Long = position.toLong()
            // пункт списка
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
            {
                val thisItem = getItem(position)
                if (thisItem is String && thisItem == "-") {
                    if (convertView != null && convertView.findViewById(R.id.splitter) != null)
                        return convertView
                    return lInflater.inflate(R.layout.adapter_splitter, parent, false)
                }
                // используем созданные, но не используемые view
                val view: View
                if (convertView == null || convertView.findViewById(R.id.splitter) != null) {
                   view = lInflater.inflate(R.layout.adapter_choose_item, parent, false)
                }else view = convertView

                (view.findViewById(R.id.title) as TextView).text = UI.name(preference.context, thisItem)

                val icon = UI.icon(context, thisItem)
                (view.findViewById(R.id.icon) as ImageView).setImageDrawable(icon)

                return view
            }
        }
        object UI
        {
            fun action(context:Context, item:Any?):String
            {
                when(item)
                {
                    "none" -> return ""
                    is AppListItem -> return item.action
                    is ApplicationInfo -> return item.packageName
                    is ActionItem -> return item.action()
                    is String -> return item
                }
                return ""
            }
            fun name(context:Context, item:Any?):String
            {
                when(item){
                    is AppListItem -> return item.name
                    is ApplicationInfo -> return context.packageManager
                            .getApplicationLabel(item).toString()

                    "" ->  return context.getString(R.string.ui_default_action)
                    "none"  -> return context.getString(R.string.ui_no_action)
                    "wait" -> return context.getString(R.string.ui_wait_app)
                    is ActionItem -> return item.name()
                    is String -> return GestureAction(context)
                            .getAction(item)?.name() ?: ""
                }
                return ""
            }
            fun icon(context:Context, item:Any?): Drawable
            {
                when(item){
                    is AppListItem -> return item.icon
                    is ApplicationInfo -> return context.packageManager.getApplicationIcon(item)
                    is ActionItem -> return item.icon()
                    is String -> return GestureAction(context)
                            .getAction(item)?.icon() ?: context.getDrawable(android.R.color.transparent)
                }
                return context.getDrawable(android.R.color.transparent)
            }

        }
    }
}
