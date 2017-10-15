package ru.vpro.kernelgesture.detect

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import gestureDetect.tools.GestureHW
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_detect_2.*
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.detect.detectors.DetectModelView

class InputDetect2Activity : AppCompatActivity()
{
    private val composites  = CompositeDisposable()
    private var events      = mutableListOf<String>()
    private var logListAdapter: ArrayAdapter<String>? = null
    private var bNeedRun    = false

    private var hw : GestureHW? = null

    private val detectObserver = Observer<List<String>> {
        Log.d("Event detect", it?.size.toString())
        it?.let {
            events.clear()
            events.addAll(it)
            logListAdapter?.notifyDataSetChanged()
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

        logListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, events)
//        keyEventList?.addHeaderView(listHeader)
        keyEventList?.adapter = logListAdapter

        hw = GestureHW(this)
        hw?.registerEvents()
        hw?.screenON()

        btnStart.setOnClickListener {
            bNeedRun = true
            DetectModelView(application).su.exec("input keyevent 26")
        }
        btnClose.isEnabled = false
        btnClose.setOnClickListener { finish() }

        composites += GestureHW.rxScreenOn
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
    }

    override fun onDestroy()
    {
        composites.clear()
        hw?.unregisterEvents()

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

    companion object
    {
        private val RESULT_ID = R.layout.activity_detect_2 and 0xFF
        fun startActivity(activity: Activity){
            val intent = Intent(activity, InputDetect2Activity::class.java)
            activity.startActivityForResult(intent, RESULT_ID)
        }
        fun getActivityResult(requestCode:Int, resultCode:Int, data:Intent?): Array<String>?
        {
            if (requestCode != RESULT_ID || resultCode != Activity.RESULT_OK || data == null)
                return null
            return data.getStringArrayExtra("geteventResult")
        }
    }

    private var dlg: AlertDialog? = null
    private fun closeDialog(){
        try{
            dlg?.dismiss()
        }
        catch (e:Exception){ }
        dlg = null
    }

    private fun reportLog(events:List<String>)
    {
        logListAdapter?.notifyDataSetChanged()

        val intent = Intent()
        intent.putExtra("geteventResult", events.toTypedArray())
        setResult(Activity.RESULT_OK, intent)

        closeDialog()
        with(AlertDialog.Builder(this))
        {
            val title = getString(R.string.ui_detect2_dlg_title)
            setTitle("$title ${events.size}")
            setMessage(getString(R.string.ui_detect2_dlg_content))

            dlg = create()
            dlg?.setOnDismissListener {  dlg = null }
            dlg?.show()
        }
    }
}