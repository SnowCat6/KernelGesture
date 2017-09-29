package ru.vpro.kernelgesture


import SuperSU.ShellSU
import android.app.AlertDialog
import android.app.FragmentManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.*
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.crash.FirebaseCrash
import gestureDetect.GestureAction
import gestureDetect.GestureDetect
import gestureDetect.GestureService
import gestureDetect.action.ActionItem
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.adapter_choose_item.view.*
import ru.vpro.kernelgesture.detect.InputDetectActivity
import ru.vpro.kernelgesture.tools.AppCompatPreferenceActivity
import ru.vpro.kernelgesture.tools.getDrawableEx
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
class SettingsActivity :
        AppCompatPreferenceActivity(),
        FragmentManager.OnBackStackChangedListener

{
    private var mInterstitialAd: InterstitialAd? = null
    private val composites = CompositeDisposable()
    private var bIsDetectProgess = false

    data class GestureConfig(
        var gestureAction:GestureAction?=null,
        var gestureDetect:GestureDetect?=null
    ) {
        fun isEmpty(): Boolean = gestureAction == null || gestureDetect == null
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG){
            FirebaseCrash.setCrashCollectionEnabled(false)
        }
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
        composites += GestureSettings.rxUpdateValue
                .filter { it.key == GestureSettings.GESTURE_ENABLE && it.value == true }
                .observeOn(Schedulers.computation())
                .subscribe {
                    val su = ShellSU()
                    su.enable(true)
                    startService(Intent(this, GestureService::class.java))
                }
        composites += rxSubTitle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    supportActionBar.subtitle = it
                }
        composites += ShellSU.commonSU.rxRootEnable
                .filter { it }
                .subscribe {
                    updateGestureConfig(true)
                }

        updateGestureConfig()
    }
    private fun updateGestureConfig(bShowDialog : Boolean = false)
    {
        synchronized(bIsDetectProgess) {
            if (bIsDetectProgess) return
            bIsDetectProgess = true
        }

        thread{
            if (gestureConfig.isEmpty()){
                gestureConfig = GestureConfig(
                        GestureAction(this),
                        GestureDetect(this))
            }else {
                gestureConfig.apply {
                    gestureAction?.onDetect()
                    gestureDetect?.enable(true)
                }
            }
            bShowAlertDlg = bShowDialog && ShellSU.commonSU.bEnableSU
            rxGestureConfig.onNext(gestureConfig)

            synchronized(bIsDetectProgess) {
                bIsDetectProgess = false
            }
        }
    }

    override fun onDestroy() {
        composites.clear()
        super.onDestroy()
    }

    override fun onStart()
    {
        super.onStart()
        //  Если окно только запустилось, создать фрагмент по умолчанию
         onBackStackChanged()
    }

    //  Количество фрагментов изменилось, надо поменять индикатор навигатора
    override fun onBackStackChanged()
    {
        //  Если есть фрагменты, то добавить кнопку назад
        supportActionBar.setDisplayHomeAsUpEnabled(fragmentManager.backStackEntryCount > 0)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (!isInMultiWindowMode) supportActionBar.subtitle = rxSubTitle.value
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        fragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private var dlg:AlertDialog? = null
    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when(item?.itemId)
        {
            android.R.id.home ->{
                if (fragmentManager.backStackEntryCount > 0)
                    super.onBackPressed()
            }
            R.id.menu_adv -> {
                val ctx = this
                mInterstitialAd?.apply {
                    if (isLoaded) show()
                    else with(AlertDialog.Builder(ctx)){
                        setTitle(getString(R.string.ui_adv_title))
                        setMessage(getString(R.string.ui_adv_content))
                        dlg = create()
                        dlg?.show()
                    }
                }
                Log.d("TAG", "The interstitial wasn't loaded yet.")
            }
            R.id.menu_settings -> InputDetectActivity.startActivity(this)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    /**
     * {@inheritDoc}
     */
    override fun onBuildHeaders(target: List<Header>) {

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

        var settings: GestureSettings? = null

        private val composites = CompositeDisposable()

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(xmlResourceId)
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            settings = GestureSettings(activity)
            val hw = GestureHW(activity)

            findPreference(GestureSettings.GESTURE_ENABLE)?.apply{
                this as SwitchPreference
                onPreferenceChangeListener = enableAllListener
                onPreferenceChangeListener.onPreferenceChange(this, settings!!.getAllEnable())

                composites += GestureSettings.rxUpdateValue
                        .filter{ it.key == GestureSettings.GESTURE_ENABLE && it.value !=  isChecked }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            isChecked = it.value as Boolean
                        }
            }

            findPreference("GESTURE_NOTIFY")?.apply{
                onPreferenceChangeListener = notifyListener
                onPreferenceChangeListener.onPreferenceChange(this, null)
            }

            findPreference("pref_ROOT")?.apply{
                setOnPreferenceClickListener {
                    thread {
                        val su = ShellSU()
                        su.enable(true)
                        if (su.checkRootAccess())
                            settings?.setAllEnable(true)
                    }
                    true
                }
            }

            findPreference("GESTURE_PROXIMITY")?.apply {
                isEnabled = hw.hasProximity()
            }
            findPreference("GESTURE_VIBRATION")?.apply {
                isEnabled = hw.hasVibrate()
            }

            preferenceItems.forEach {

                findPreference(it.key)?.apply {

                    icon = it.icon
                    onPreferenceChangeListener = changeListener()
                    onPreferenceClickListener = actionListener
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
                        icon = context.getDrawableEx(item.iconResource)

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

            composites += ShellSU.commonSU.rxRootEnable
                    .filter { it }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        updateControls()
                    }

            composites += GestureSettings.rxUpdateValue
                    .filter { it.key ==  GestureSettings.GESTURE_ENABLE }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        updateControls()
                    }

            composites += rxGestureConfig
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        updateControls()
                    }

            updateControls()
        }

        override fun onDestroy(){
            settings = null
            composites.clear()
            super.onDestroy()
        }

        private val enableAllListener = Preference.OnPreferenceChangeListener { preference, value ->
            settings?.setAllEnable(value as Boolean)
            true
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
        private val actionListener= Preference.OnPreferenceClickListener { preference ->

                val adapter = BoxAdapter(preference)

                with(AlertDialog.Builder(activity))
                {
                    setTitle(getString(R.string.iu_choose_action))
                    setAdapter(adapter, onClickListener)
                    dlg = create()
                    dlg?.setOnDismissListener {  dlg = null }
                    dlg?.show()
                }

                true
            }

        private val notifyListener = Preference.OnPreferenceChangeListener { preference, value ->

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
                    preference.summary = ringtone?.getTitle(activity) ?: getString(R.string.ui_sound_default)
                }

                true
            }

        private val onClickListener = DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->

            val adapter = (dialogInterface as AlertDialog).listView.adapter as BoxAdapter
            val item = adapter.getItem(i) as? BoxAdapter.ActionListItem ?: return@OnClickListener
            val preference = adapter.preference as TwoStatePreference
            val itemAction =  uiAction(activity, item)

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

        inner class GestureItem(val key:String, private var defaultAction:String)
        {
            private var applicationInfo:ApplicationInfo? = null

            var action:String
                get() {
                    val a = settings?.getAction(key)
                    if (uiAction(activity, a).isNotEmpty()) return a!!
                    if (a != null) return ""
                    if (uiAction(activity, defaultAction).isEmpty()) return ""

                    action = defaultAction
                    enable = true
                    return defaultAction
                }
                set(value) {
                    icon = null
                    applicationInfo = null
                    settings?.setAction(key, value)
                }

            var enable:Boolean
                get() = settings?.getEnable(key) == true
                set(value){
                    icon = null
                    settings?.setEnable(key, value)
                }

            val actionName:String
                get() {
                    getAppInfo()?.apply {
                        return uiName(activity, this)
                    }

                    if (action.isEmpty()){
                        if (key == "GESTURE_DEFAULT_ACTION")
                            return getString(R.string.ui_no_action)
                        if (!enable) return getString(R.string.ui_no_action)
                    }

                    return uiName(activity, action)
                }

            var icon:Drawable? = null
                get(){
                    field?.apply { return field }

                    getAppInfo()?.apply {
                        field = uiIcon(activity, this)
                        return field
                    }

                    field = uiIcon(activity, action)
                    return field
                }

            private fun getAppInfo():ApplicationInfo?
                    = applicationInfo ?: uiAppInfo(activity, action)
        }

        private var dlg:AlertDialog? = null
        private fun updateControls()
        {
            val su = ShellSU()
            if (su.hasRootProcess()) preferenceScreen.findPreference("pref_ROOT")?.apply {
                preferenceScreen.removePreference(this)
            }

            val fragment = fragmentManager
                    .findFragmentById(android.R.id.content) as? PreferenceFragment? ?: return

            val context = fragment.activity
            val support = gestureConfig.gestureDetect?.getSupport() ?: emptyList()
            val settings = GestureSettings(context)

            var titles = emptyList<String>()
            var alertMessage:String? = null
            val bAllEnable = settings.getAllEnable()

            val bGesture = support.contains("GESTURE")
            val bGestureON = support.contains("GESTURE_ON")
            val bGestureHW = support.contains("GESTURE_HW")
            if (bGestureHW) titles += context.getString(R.string.ui_title_gestures)

            val bKeys = support.contains("KEYS")
            if (bKeys) titles += context.getString(R.string.ui_title_keys)

            val bProximity = support.contains("PROXIMITY")
            if (bProximity) titles += context.getString(R.string.ui_title_gesture)

            val subtitle = if (!titles.isEmpty()) titles.joinToString(", ")
            else context.getString(R.string.ui_title_no_any_support)
            rxSubTitle.onNext(subtitle)

            if (titles.isEmpty())
                alertMessage = context.getString(R.string.ui_alert_gs_message_wo_keys)
            else
                if (!bGestureHW)
                    alertMessage = context.getString(R.string.ui_alert_gs_message_keys) + " " + titles.joinToString(", ")

            with(preferenceScreen){
                val bEnable = su.hasRootProcess() && bAllEnable && titles.isNotEmpty()
                findPreference("GESTURE_GROUP")?.isEnabled = bGesture && bEnable
                findPreference("GESTURE_GROUP_ON")?.isEnabled = bGestureON && bEnable
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
                dlg?.setOnDismissListener {  dlg = null }
                dlg?.show()
            }
        }

        inner class BoxAdapter internal constructor(
                internal val preference: Preference) : BaseAdapter()
        {
            private var objects = emptyList<Any>()
            private val context = preference.context
            private val lInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            private val currentAction = settings?.getAction(preference.key)

            inner class ActionListItem(val action:String, val name:String, val icon:Drawable)
            {
                constructor(action:Any) :
                        this(uiAction(context, action), uiName(context, action), uiIcon(context, action))
            }

            init{
                objects += ActionListItem("none")

                gestureConfig.gestureAction?.getActions()
                        ?.forEach { objects += ActionListItem(it) }

                thread{
                    val pm =  context.packageManager
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

                    Handler(Looper.getMainLooper()).post {
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
                val bSplitView:View? = convertView?.findViewById(R.id.splitter)

                if (thisItem == "-")
                {
                    if (bSplitView != null) return convertView
                    return lInflater.inflate(R.layout.adapter_splitter, parent, false)
                }
                // используем созданные, но не используемые view
                val view:View = if (bSplitView != null || convertView == null)
                    lInflater.inflate(R.layout.adapter_choose_item, parent, false)
                else convertView

                view.title?.text = uiName(context, thisItem)
                view.icon?.setImageDrawable(uiIcon(context, thisItem))

                view.background = if (currentAction == uiAction(context, thisItem)) context.getDrawableEx(android.R.color.holo_orange_light)
                else null

                return view
            }
        }

        fun uiAction(context:Context, item:Any?):String
        {
            return when(item)
            {
                "none" -> ""
                is BoxAdapter.ActionListItem -> item.action
                is ApplicationInfo -> item.packageName
                is ActionItem -> item.action()
                is String -> {
                    gestureConfig.gestureAction?.getAction(item)?.apply { return item  }
                    uiAppInfo(context, item)?.apply { return item }
                    ""
                }
                else -> ""
            }
        }
        fun uiName(context:Context, item:Any?):String
        {
            return when(item){
                is BoxAdapter.ActionListItem -> item.name
                is ApplicationInfo -> context.packageManager
                        .getApplicationLabel(item)?.toString() ?: ""

                "" ->  context.getString(R.string.ui_default_action)
                "none"  -> context.getString(R.string.ui_no_action)
                is ActionItem -> item.name()
                is String -> gestureConfig.gestureAction?.getAction(item)?.name() ?: ""
                else -> ""
            }
        }
        fun uiIcon(context:Context, item:Any?): Drawable
        {
            return when(item){
                is BoxAdapter.ActionListItem -> item.icon
                is ApplicationInfo -> context.packageManager.getApplicationIcon(item)?: context.getDrawableEx(android.R.color.transparent)
                is ActionItem -> item.icon()
                is String -> gestureConfig.gestureAction?.getAction(item)?.icon() ?: context.getDrawableEx(android.R.color.transparent)
                else -> context.getDrawableEx(android.R.color.transparent)
            }
        }
        fun uiAppInfo(context:Context, action:String):ApplicationInfo?
        {
            try {
                return context.packageManager.getApplicationInfo(action, 0)
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
        var bShowAlertDlg = false
        var gestureConfig = GestureConfig()

        val rxSubTitle      = BehaviorSubject.createDefault(String())
        var rxGestureConfig = BehaviorSubject.createDefault(gestureConfig)
    }
}
