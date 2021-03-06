package ru.vpro.kernelgesture.detect

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import gestureDetect.tools.GestureHW
import gestureDetect.tools.RxInputReader
import gestureDetect.tools.RxScreenOn
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_detect_2.*
import org.inowave.planning.common.tools.bindAdapter
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.detect.detectors.DetectModelView
import ru.vpro.kernelgesture.tools.HeaderString
import ru.vpro.kernelgesture.tools.TwoString
import ru.vpro.kernelgesture.tools.adapter.ReAdapter


class InputDetect2Activity : AppCompatActivity()
{
    private val composites  = CompositeDisposable()
    private var events      = ArrayList<Any>()
    private var adapter     = ReAdapter(events)
    private var bNeedRun    = false

    private var hw : GestureHW? = null

    private val detectObserver = Observer<List<RxInputReader.EvData>> {
        Log.d("Event detect", it?.size.toString())
        it?.let {
            events.clear()
            events.add(HeaderString("Gesture events"))
            events.addAll(it.map { TwoString(it.device, it.evButton) })
            adapter.items = events
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_2)

        supportActionBar?.apply {
            subtitle = getString(R.string.ui_detect2_subtitle)
            setDisplayHomeAsUpEnabled(true)
        }

        hw = GestureHW(this)
        hw?.screenON()

        btnStart.setOnClickListener {
            bNeedRun = true
            DetectModelView(application).su.exec("input keyevent 26")
        }
        btnClose.isEnabled = false
        btnClose.setOnClickListener { finish() }

        val rxScreen = RxScreenOn(application)
        composites += rxScreen
            .subscribe {
                if (it) {
                    val v = DetectModelView.getModel(this)
                    if (v.events.hasObservers()) {
                        v.events.removeObserver(detectObserver)
                        bNeedRun = false
                        reportLog(events)
                    }
                }else{
                    if (bNeedRun) {
                        btnClose.isEnabled = true
                        DetectModelView.getModel(this)
                                .events
                                .start().observe(this, detectObserver)
                    }
                }
            }
        adapter.addHeaderView(listHeader)
        keyEventList?.bindAdapter(adapter)
    }

    override fun onResume() {
        super.onResume()
        val wind = window
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    override fun onDestroy()
    {
        composites.clear()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item?.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var dlg: AlertDialog? = null
    private fun closeDialog(){
        try{
            dlg?.dismiss()
        }
        catch (e:Exception){ }
        dlg = null
    }

    private fun reportLog(events:ArrayList<Any>)
    {
        val intent = Intent()
        intent.putExtra("result", events)
//        intent.putExtra("result", registerJson().toJson(events))
        setResult(Activity.RESULT_OK, intent)

        closeDialog()
        with(AlertDialog.Builder(this))
        {
            val title = getString(R.string.ui_detect2_dlg_title)
            setTitle("$title ${events.size-1}")
            setMessage(getString(R.string.ui_detect2_dlg_content))

            dlg = create()
            dlg?.setOnDismissListener {  dlg = null }
            dlg?.show()
        }
    }
    companion object
    {
        private val RESULT_ID = R.layout.activity_detect_2 and 0xFF
        fun startActivity(activity: Activity){
            val intent = Intent(activity, InputDetect2Activity::class.java)
            activity.startActivityForResult(intent, RESULT_ID)
        }
        fun getActivityResult(requestCode:Int, resultCode:Int, data:Intent?): List<Any>?
        {
            if (requestCode != RESULT_ID || resultCode != Activity.RESULT_OK || data == null)
                return null

            return data.getSerializableExtra("result") as ArrayList<Any>
        }
    }
}