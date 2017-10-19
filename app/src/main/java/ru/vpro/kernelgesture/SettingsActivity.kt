package ru.vpro.kernelgesture


import SuperSU.ShellSU
import android.app.AlertDialog
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.crash.FirebaseCrash
import gestureDetect.GestureAction
import gestureDetect.GestureDetect
import gestureDetect.GestureService
import gestureDetect.action.ActionItem
import gestureDetect.tools.GestureSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import ru.vpro.kernelgesture.detect.InputDetectActivity
import ru.vpro.kernelgesture.tools.AppCompatPreferenceActivity
import ru.vpro.kernelgesture.tools.getDrawableEx
import kotlin.concurrent.thread

class SettingsActivity :
        AppCompatPreferenceActivity(),
        FragmentManager.OnBackStackChangedListener

{
    private var mInterstitialAd: InterstitialAd? = null
    private val composites = CompositeDisposable()

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

        if (rxConfigUpdate.value == null)
        {
            GestureDetect(this).also {
                composites += it.rxSupportUpdate
                    .subscribe { list->
                        rxConfigUpdate.onNext(it)
                    }

                thread{
                    it.onCreate(this)
                }

            }
        }
    }

    override fun onDestroy()
    {
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

    class TouchscreenPreferenceFragment : GesturePreferenceFragment()
    { init {  xmlResourceId = R.xml.pref_gesture_touch; iconResource = R.drawable.icon_gesture_touch } }

    class KeyPreferenceFragment : GesturePreferenceFragment()
    { init { xmlResourceId = R.xml.pref_gesture_keys; iconResource = R.drawable.icon_gesture_key }}

    companion object
    {
        var bShowAlertDlg   = true
        val rxSubTitle      = BehaviorSubject.createDefault(String())
        val rxConfigUpdate  = BehaviorSubject.create<GestureDetect>()

        fun uiAction(context:Context, item:Any?):String?
        {
            return when(item)
            {
                "none" -> null
                is GesturePreferenceFragment.ActionListItem -> item.action
                is ApplicationInfo -> item.packageName
                is ActionItem -> item.action(context) ?: ""
                is String -> {
                    GestureAction.getInstance(context)
                            .getAction(context, item)
                            ?.apply { return item  }
                    uiAppInfo(context, item)?.apply { return item }
                    ""
                }
                else -> ""
            }
        }
        fun uiName(context:Context, item:Any?):String
        {
            return when(item){
                is GesturePreferenceFragment.ActionListItem -> item.name
                is ApplicationInfo -> context.packageManager
                        .getApplicationLabel(item)?.toString() ?: ""

                "" ->  context.getString(R.string.ui_default_action)
                "none"  -> context.getString(R.string.ui_no_action)
                is ActionItem -> item.name(context) ?: ""
                is String -> GestureAction.getInstance(context)
                        .getAction(context, item)?.name(context) ?: ""
                else -> ""
            }
        }
        fun uiIcon(context:Context, item:Any?): Drawable
        {
            return when(item){
                is GesturePreferenceFragment.ActionListItem -> item.icon
                is ApplicationInfo -> context.packageManager.getApplicationIcon(item)
                        ?: context.getDrawableEx(android.R.color.transparent)
                is ActionItem -> item.icon(context)
                        ?: context.getDrawableEx(android.R.color.transparent)
                is String -> GestureAction.getInstance(context)
                        .getAction(context, item)?.icon(context)
                        ?: context.getDrawableEx(android.R.color.transparent)
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

}
