package ru.vpro.kernelgesture.detect

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.ArrayAdapter
import gestureDetect.GestureService
import gestureDetect.tools.GestureHW
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_detect_2.*
import ru.vpro.kernelgesture.R

class InputDetect2Activity : AppCompatActivity()
{
    private val composites = CompositeDisposable()
    private var events = ArrayList<String>()
    private var logListAdapter: ArrayAdapter<String>? = null
    private var bNeedRun = false

    private val su = InputDetectActivity.su
    private var hw : GestureHW? = null

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
            su.exec("input keyevent 26")
        }
        btnClose.isEnabled = false
        btnClose.setOnClickListener {
            finish()
        }

        composites += GestureHW.rxScreenOn
            .subscribe {
                if (it){
                   stopThread()
                }else{
                   runThread()
                }
            }
    }

    override fun onDestroy()
    {
        composites.clear()
        GestureService.bDisableService = false
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

    private fun runThread(){

        if (!bNeedRun) return

        btnClose.isEnabled = true

        composites += startDetect()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hw?.vibrate()
                },{},{
                    reportLog(events)
                })
    }
    private fun evCmd(ix : Int, nLimit:Int, nRepeat:Int)
    {
        val CR = "\\\\n"
        val seq = (1..nRepeat).joinToString(" ")
        su.exec("while true ; do v$ix=\$(getevent -c $nLimit -l) ; [ \"\$v$ix\" ] && for i in $seq ; do echo \"\$v$ix\">&2 ; done ; done &")
    }

    private fun startDetect()
            = Observable.create<String>{ emitter ->

        GestureService.bDisableService = true
        hw?.vibrate()

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        evCmd(0, 1, 10)
        evCmd(1, 2, 8)
        evCmd(2, 4, 4)
        evCmd(3, 8, 2)

        while(!emitter.isDisposed){

            val line = su.readErrorLine() ?: break
            if (line == "--END_DETECT--") break

            if (!line.contains("EV_KEY")) continue

            if (events.contains(line)) continue
            events.add(line)
            emitter.onNext(line)
        }
        GestureService.bDisableService = false
        emitter.onComplete()
    }

    private fun stopThread()
    {
        if (GestureService.bDisableService) {
            bNeedRun = false
            su.killJobs()
            su.exec("echo --END_DETECT-->&2")
        }
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