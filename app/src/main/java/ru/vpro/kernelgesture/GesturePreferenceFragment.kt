package ru.vpro.kernelgesture

import SuperSU.ShellSU
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.*
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import ru.vpro.kernelgesture.tools.BoxAdapter
import ru.vpro.kernelgesture.tools.getDrawableEx
import kotlin.concurrent.thread

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
       updateControls()

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

       arrayOf(
               Pair("TOUCH_PREFERENCE", SettingsActivity.TouchscreenPreferenceFragment::class.java),
               Pair("KEY_PREFERENCE", SettingsActivity.KeyPreferenceFragment::class.java)
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

       composites += GestureSettings.rxUpdateValue
               .filter { it.key == GestureSettings.GESTURE_ENABLE }
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe {
                   updateControls()
               }

       composites += SettingsActivity.rxConfigUpdate
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe {
                   updateControls()
                   updateActions()
               }
       updateActions()
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

   private val onClickListener = DialogInterface.OnClickListener {
       dialogInterface: DialogInterface, i: Int ->

       val adapter = (dialogInterface as AlertDialog).listView.adapter as BoxAdapter
       val item = adapter.getItem(i) as? BoxAdapter.ActionListItem ?: return@OnClickListener
       val preference = adapter.preference as TwoStatePreference
       val itemAction = SettingsActivity.uiAction(activity, item)

       getItemInstance(preference.key)?.apply {

           action = itemAction ?: ""
           enable = itemAction?.isNotEmpty() == true

           preference.isChecked = enable
           preference.icon = icon
           preference.onPreferenceChangeListener.onPreferenceChange(preference, enable)

       }
   }

   private fun getItemInstance(key:String):GestureItem?
           = preferenceItems.find { it.key == key }

   inner class GestureItem(val key:String, private var defaultAction:String)
   {
       private var applicationInfo: ApplicationInfo? = null

       var action:String
           get() {
               val a = settings?.getAction(key)
               if (SettingsActivity.uiAction(activity, a)?.isNotEmpty() == true) return a!!
               if (a != null) return ""
               if (SettingsActivity.uiAction(activity, defaultAction)?.isEmpty() == true) return ""

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
                   return SettingsActivity.uiName(activity, this)
               }

               if (action.isEmpty()){
                   if (key == "GESTURE_DEFAULT_ACTION")
                       return getString(R.string.ui_no_action)
                   if (!enable) return getString(R.string.ui_no_action)
               }

               return SettingsActivity.uiName(activity, action)
           }

       var icon: Drawable? = null
           get(){
               field?.apply { return field }

               getAppInfo()?.apply {
                   field = SettingsActivity.uiIcon(activity, this)
                   return field
               }

               field = SettingsActivity.uiIcon(activity, action)
               return field
           }

       private fun getAppInfo(): ApplicationInfo?
               = applicationInfo ?: SettingsActivity.uiAppInfo(activity, action)
   }

   private fun updateActions()
   {
       preferenceItems.forEach {

           findPreference(it.key)?.apply {

               icon = it.icon
               onPreferenceChangeListener = changeListener()
               onPreferenceClickListener = actionListener
               onPreferenceChangeListener.onPreferenceChange(this, it.enable)
           }
       }
   }

   private var dlg: AlertDialog? = null
   private fun updateControls()
   {
       val su = ShellSU()
       if (su.hasRootProcess()) preferenceScreen.findPreference("pref_ROOT")?.apply {
           preferenceScreen.removePreference(this)
       }

       val fragment = fragmentManager
               .findFragmentById(android.R.id.content) as? PreferenceFragment? ?: return

       val context = fragment.activity
       val support = SettingsActivity.rxConfigUpdate.value?.getSupport() ?: emptyList()
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
       SettingsActivity.rxSubTitle.onNext(subtitle)

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
       if (alertMessage == null || !SettingsActivity.bShowAlertDlg) return
       SettingsActivity.bShowAlertDlg = false

       with(AlertDialog.Builder(context))
       {
           setTitle(context.getString(R.string.ui_alert_gs_title))
           setMessage(alertMessage)
           dlg = create()
           dlg?.setOnDismissListener {  dlg = null }
           dlg?.show()
       }
   }

}