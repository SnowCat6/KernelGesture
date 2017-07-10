package ru.vpro.kernelgesture


import SuperSU.ShellSU
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import gestureDetect.GestureAction
import gestureDetect.GestureDetect
import gestureDetect.GestureService
import gestureDetect.GestureSettings
import gestureDetect.action.ActionItem
import kotlin.concurrent.thread


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

    override fun onCreate(savedInstanceState: Bundle?)
    {
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

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        if (!isInMultiWindowMode) updateControls(this)
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
//        supportActionBar.setDisplayHomeAsUpEnabled(true)
//        supportActionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_directions)
        fragmentManager.addOnBackStackChangedListener {
            supportActionBar.setDisplayHomeAsUpEnabled(fragmentManager.backStackEntryCount > 0)
        }
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
            android.R.id.home ->{
                super.onBackPressed()
                return true
            }
            R.id.menu_adv ->
            {
                if (mInterstitialAd?.isLoaded != null && mInterstitialAd!!.isLoaded) {
                    mInterstitialAd?.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }
            }
            R.id.menu_settings ->
            {
                InputDetectActivity.startActivity(this)
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
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
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, GesturePreferenceFragment())
                .commit()
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean =
        arrayOf(
                PreferenceFragment::class.java.name,
                GesturePreferenceFragment::class.java.name
        ).contains(fragmentName)

     /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    open class GesturePreferenceFragment : PreferenceFragment()
    {
        private val preferenceItems = arrayOf(
                GestureItem("KEY_U",       "screen.on"),
                GestureItem("KEY_U_ON",    ""),
                GestureItem("KEY_UP",      "application.dialer"),
                GestureItem("KEY_DOWN",    "application.contacts"),
                GestureItem("KEY_LEFT",    ""),
                GestureItem("KEY_RIGHT",   ""),
                GestureItem("KEY_O",       ""),
                GestureItem("KEY_E",       ""),
                GestureItem("KEY_M",       "application.email"),
                GestureItem("KEY_L",       ""),
                GestureItem("KEY_W",       "application.browser"),
                GestureItem("KEY_S",       "application.camera"),
                GestureItem("KEY_V",       ""),
                GestureItem("KEY_Z",       "speech.battery"),
                GestureItem("KEY_VOLUMEUP",             ""),
                GestureItem("KEY_VOLUMEUP_DELAY",       ""),
                GestureItem("KEY_VOLUMEUP_DELAY_ON",    ""),
                GestureItem("KEY_VOLUMEDOWN",           ""),
                GestureItem("KEY_VOLUMEDOWN_DELAY",     ""),
                GestureItem("KEY_VOLUMEDOWN_DELAY_ON",  ""),
                GestureItem("KEY_PROXIMITY",            "speech.time"),
                GestureItem("GESTURE_DEFAULT_ACTION",   "")
        )

        var xmlResourceId = R.xml.pref_gesture
        var iconResource = 0

        var settings:GestureSettings? = null
        var gestureAction:GestureAction? = null

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(xmlResourceId)
            LocalBroadcastManager.getInstance(activity).registerReceiver(mReceiver, IntentFilter(ShellSU.EVENT_UPDATE_ROOT_STATE))

            settings = GestureSettings(activity)
            gestureAction = GestureAction(activity)

            findPreference("GESTURE_ENABLE")?.apply{
                onPreferenceChangeListener = enableAllListener()
                onPreferenceChangeListener.onPreferenceChange(this, settings!!.getAllEnable())
            }

            findPreference("GESTURE_NOTIFY")?.apply{
                onPreferenceChangeListener = notifyListener()
                onPreferenceChangeListener.onPreferenceChange(this, null)
            }

            findPreference("pref_ROOT")?.apply{
                setOnPreferenceClickListener {
                    thread {
                        su.enable(true)
                        if (su.checkRootAccess(context))
                            settings?.setAllEnable(true)
                    }
                    true
                }
            }

            preferenceItems.forEach {

                it.action
                findPreference(it.key)?.apply {
                    icon = it.icon
                    onPreferenceChangeListener = changeListener()
                    onPreferenceClickListener = actionListener()
                    onPreferenceChangeListener.onPreferenceChange(this, it.enable)
                }
            }

            arrayOf(
                    Pair("TOUCH_PREFERENCE", TouchscreenPreferenceFragment::class.java),
                    Pair("KEY_PREFERENCE", KeyPreferenceFragment::class.java)
            ).forEach { (preferenceName, preferenceClass) ->

                findPreference(preferenceName)?.apply {

                    val item = preferenceClass.newInstance()
                    if (item.iconResource > 0)
                        icon = context.getDrawable(item.iconResource)

                    setOnPreferenceClickListener {
                        fragmentManager
                                .beginTransaction()
                                .replace(android.R.id.content, item)
                                .addToBackStack(null)
                                .commit()
                        true
                    }
                }
            }

        }

        override fun onResume() {
            super.onResume()
            updateControls()
        }

        override fun onDestroy(){
            settings = null
            gestureAction = null
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver)
            super.onDestroy()
        }

        private var mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                activity?.apply {
                    updateControls(intent)
                }
            }
        }

        private fun enableAllListener(): Preference.OnPreferenceChangeListener {
            return Preference.OnPreferenceChangeListener { preference, value ->

                value as Boolean
                preference as TwoStatePreference
                val context = preference.context

                if (value) su.enable(true)
                settings?.setAllEnable(value)
                updateControls(context)
                if (value) context.startService(Intent(context, GestureService::class.java))

                if (value == true && !su.hasRootProcess())
                {
                    thread{
                        if (su.checkRootAccess(context)) {
                            settings?.setAllEnable(value)
                            updateControls(context)
                        }
                    }
                }

/*
            if (value){
                val fileName = "/dev/input/event5"
                Log.d("Read IO", fileName)
                try{
                    val f = File(fileName)
                    f.setReadOnly()
                    f.readBytes()
                    Log.d("Read IO", "$fileName read OK")
                }catch (e:Exception){
                    e.printStackTrace()
                }
                Log.d("Read IO", "exec shell")
                try{
                    val sh = Runtime.getRuntime().exec("getevent -p")
                    sh.waitFor()
                    val line = sh.inputStream.bufferedReader().readLine()
                    Log.d("Read IO", "exec shell result: $line")
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
*/
                true
            }
        }
        private fun changeListener(): Preference.OnPreferenceChangeListener {
            return Preference.OnPreferenceChangeListener { preference, value ->

                value as Boolean

                getItemInstance(preference.key)?.apply {
                    enable = value
                    preference.summary = actionName
                }

                true
            }
        }
        private fun actionListener(): Preference.OnPreferenceClickListener {
            return Preference.OnPreferenceClickListener { preference ->

                val adapter = BoxAdapter(preference)

                with(AlertDialog.Builder(activity))
                {
                    setTitle(getString(R.string.iu_choose_action))
                    setAdapter(adapter, onClickListener)
                    create().show()
                }

                true
            }
        }
        private fun notifyListener() : Preference.OnPreferenceChangeListener {
            return Preference.OnPreferenceChangeListener { preference, value ->

                var notify:String? = value as String?

                try {
                    if (notify == null) {
                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
                        notify = sharedPreferences.getString("GESTURE_NOTIFY", null)
                    }
                }catch (e:Exception){}

                if (notify == null || notify.isEmpty()) {
                    preference.summary = getString(R.string.ui_no_notify)
                }else{
                    val ringtone = RingtoneManager.getRingtone(activity, Uri.parse(notify))
                    when (ringtone) {
                        null -> preference.summary = getString(R.string.ui_sound_default)
                        else -> preference.summary = ringtone.getTitle(activity)
                    }
                }

                true
            }
        }
        private val onClickListener = DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->

            val adapter = (dialogInterface as AlertDialog).listView.adapter as BoxAdapter
            val item = adapter.getItem(i) as? ActionListItem ?: return@OnClickListener

            val preference = adapter.preference as TwoStatePreference

            val itemAction =  uiAction(item)

            if (BuildConfig.DEBUG) {
                Log.d("Set gesture action", itemAction)
            }

            getItemInstance(preference.key)?.apply {

                action = itemAction
                enable = itemAction.isNotEmpty()

                preference.isChecked = enable
                preference.icon = icon
                preference.onPreferenceChangeListener.onPreferenceChange(preference, enable)
            }
        }

        private fun getItemInstance(key:String):GestureItem?
                = preferenceItems.find { it.key == key }

        inner class GestureItem(val key:String, var defaultAction:String)
        {
            var applicationInfo:ApplicationInfo? = null

            var action:String
                get() {
                    val a = settings?.getAction(key)
                    if (uiAction(a).isNotEmpty()) return a!!
                    if (uiAction(defaultAction).isEmpty()) return ""

                    action = defaultAction
                    enable = true
                    return defaultAction
                }
                set(value) {
                    _actionName = ""
                    _icon = null
                    applicationInfo = null
                    settings?.setAction(key, value)
                }
            var enable:Boolean
                get() = settings?.getEnable(key) ?: false
                set(value){
                    _actionName = ""
                    _icon = null
                    settings?.setEnable(key, value)
                }

            var _actionName:String = ""
            val actionName:String
                get() {
                    if (_actionName.isNotEmpty()) return _actionName

                    if (getAppInfo() != null) {
                        _actionName = uiName(getAppInfo())
                    }else{
                        if (action == "" && key == "GESTURE_DEFAULT_ACTION") {
                            _actionName = getString(R.string.ui_no_action)
                        }else {
                            if (action == "" && !enable) _actionName = getString(R.string.ui_no_action)
                            else _actionName = uiName(action)
                        }
                    }
                    return _actionName
                }

            var _icon:Drawable? = null
            val icon:Drawable?
                get(){
                    if (_icon == null) {
                        if (getAppInfo() != null) _icon = uiIcon(getAppInfo())
                        else _icon = uiIcon(action)
                    }
                    return _icon
                }

            private fun getAppInfo():ApplicationInfo?
                    = if (applicationInfo != null)
                        applicationInfo else uiAppInfo(action)
        }

        var dlg:AlertDialog? = null
        private fun updateControls(intent:Intent? = null)
        {
            if (su.hasRootProcess()) preferenceScreen.findPreference("pref_ROOT")?.apply {
                preferenceScreen.removePreference(this)
            }

            gestureAction?.onDetect()

            val fragment = fragmentManager
                    .findFragmentById(android.R.id.content) as PreferenceFragment? ?: return

            val context = fragment.activity
            val support = GestureDetect(context).getSupport()
            val settings = GestureSettings(context)

            var titles = emptyList<String>()
            var alertMessage:String? = null
            val bAllEnable = settings.getAllEnable()

            val bGesture = support.contains("GESTURE")
            if (bGesture) titles += context.getString(R.string.ui_title_gestures)

            val bKeys = support.contains("KEYS")
            if (bKeys) titles += context.getString(R.string.ui_title_keys)

            val bProximity = support.contains("PROXIMITY")
            if (bProximity) titles += context.getString(R.string.ui_title_gesture)

            (activity as AppCompatPreferenceActivity?)?.supportActionBar?.apply {
                if (!titles.isEmpty()) subtitle = titles.joinToString(", ")
                else subtitle = context.getString(R.string.ui_title_no_any_support)
            }

            if (titles.isEmpty())
                alertMessage = context.getString(R.string.ui_alert_gs_message_wo_keys)
            else
                if (!support.contains("GESTURE"))
                    alertMessage = context.getString(R.string.ui_alert_gs_message_keys) + " " + titles.joinToString(", ")

            with(preferenceScreen){
                val bEnable = su.hasRootProcess() && bAllEnable && titles.isNotEmpty()
                findPreference("GESTURE_GROUP")?.isEnabled = /*bGesture*/ bEnable
                findPreference("GESTURE_GROUP_ON")?.isEnabled = /*bGesture*/ bEnable
                findPreference("KEY_GROUP")?.isEnabled = bKeys && bEnable
                findPreference("KEY_GROUP_ON")?.isEnabled = bKeys && bEnable
                findPreference("SENSOR_GROUP")?.isEnabled = bProximity && bAllEnable
            }

            if (!su.hasRootProcess()) return
            if (alertMessage == null || !bShowAlertDlg) return
            bShowAlertDlg = false

            with(AlertDialog.Builder(context))
            {
                setTitle(context.getString(R.string.ui_alert_gs_title))
                setMessage(alertMessage)
                dlg = create()
                dlg?.show()
            }
        }

        inner class ActionListItem(val action:String, val name:String, val icon:Drawable)
        {
            constructor(action:Any) :
                    this(uiAction(action), uiName(action), uiIcon(action))
        }

        inner class BoxAdapter internal constructor(
                internal val preference: Preference) : BaseAdapter()
        {
            private var objects = emptyList<Any>()
            private val lInflater = preference.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            private val context = preference.context
            private val currentAction = settings?.getAction(preference.key)

            init{
                objects += ActionListItem("none")

                gestureAction?.getActions()
                        ?.forEach { objects += ActionListItem(it) }

                thread{
                    val pm =  preference.context.packageManager

                    val mainIntent = Intent(Intent.ACTION_MAIN, null)
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val pkgAppsList = pm.queryIntentActivities(mainIntent, 0)

                    var items = listOf<Any>("-")
                    var filterMap = listOf<String>()
                    pkgAppsList.forEach {
                        if (!filterMap.contains(it.activityInfo.applicationInfo.packageName)) {
                            items += ActionListItem(it.activityInfo.applicationInfo)
                            filterMap += it.activityInfo.applicationInfo.packageName
                        }
                    }

                    Handler(context.mainLooper).post {
                        objects += items
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

                (view.findViewById(R.id.title) as TextView).text = uiName(thisItem)

                val icon = uiIcon(thisItem)
                (view.findViewById(R.id.icon) as ImageView).setImageDrawable(icon)

                if (currentAction != null && currentAction.isNotEmpty() &&
                        currentAction == uiAction(thisItem))
                {
                    view.background = context.getDrawable(android.R.color.holo_orange_light)
                }else{
                    view.background = null
                }

                return view
            }
        }

        fun uiAction(item:Any?):String
        {
            when(item)
            {
                "none" -> return ""
                is ActionListItem -> return item.action
                is ApplicationInfo -> return item.packageName
                is ActionItem -> return item.action()
                is String -> {
                    if (gestureAction?.getAction(item) != null) return item
                    val appInfo = uiAppInfo(item)
                    if (appInfo != null) return item
                }
            }
            return ""
        }

        fun uiName(item:Any?):String
        {
            when(item){
                is ActionListItem -> return item.name
                is ApplicationInfo -> return activity.packageManager
                        .getApplicationLabel(item).toString()

                "" ->  return activity.getString(R.string.ui_default_action)
                "none"  -> return activity.getString(R.string.ui_no_action)
                is ActionItem -> return item.name()
                is String -> return gestureAction?.getAction(item)?.name() ?: ""
            }
            return ""
        }
        fun uiIcon(item:Any?): Drawable
        {
            when(item){
                is ActionListItem -> return item.icon
                is ApplicationInfo -> return activity.packageManager.getApplicationIcon(item)
                is ActionItem -> return item.icon()
                is String -> return gestureAction?.getAction(item)?.icon() ?: activity.getDrawable(android.R.color.transparent)
            }
            return activity.getDrawable(android.R.color.transparent)
        }
        fun uiAppInfo(action:String):ApplicationInfo?
        {
            try {
                return activity.packageManager.getApplicationInfo(action, 0)
            } catch (e: Exception) {}
            return null
        }
    }

    class TouchscreenPreferenceFragment : GesturePreferenceFragment()
    { init {  xmlResourceId = R.xml.pref_gesture_touch; iconResource = R.drawable.icon_gesture_touch } }

    class KeyPreferenceFragment : GesturePreferenceFragment()
    { init { xmlResourceId = R.xml.pref_gesture_keys; iconResource = R.drawable.icon_gesture_key }}


    companion object
    {
        val su = ShellSU()
        var bShowAlertDlg = true

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        fun updateControls(context:Context)
        {
            val intent = Intent(ShellSU.EVENT_UPDATE_ROOT_STATE)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
