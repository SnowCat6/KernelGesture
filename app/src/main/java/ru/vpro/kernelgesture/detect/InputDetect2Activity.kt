package ru.vpro.kernelgesture.detect

import SuperSU.ShellSU
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import gestureDetect.GestureService
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import ru.vpro.kernelgesture.R
import kotlin.concurrent.thread


class InputDetect2Activity : AppCompatActivity()
{
    private var detectThread:Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_2)

        supportActionBar?.apply {
            subtitle = getString(R.string.ui_detect2_subtitle)
            setDisplayHomeAsUpEnabled(true)
        }

        GestureHW(this).screenON()

        //  Register screen activity event
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onEventIntent, intentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(onEventIntent)
        detectThread?.interrupt()
        detectThread = null
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
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

    /**
     * SCREEN Events
     */
    private val onEventIntent = object : BroadcastReceiver() {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
            //  Screen OFF
                Intent.ACTION_SCREEN_OFF -> {
                    bRunThread = true
                    detectThread = thread {
                        val settings = GestureSettings(context)
                        val bEnable = settings.getAllEnable()
                        val serviceIntent = Intent(context, GestureService::class.java)
                        if (bEnable) {
                            stopService(serviceIntent)
                            Thread.sleep(1000)
                        }

                        startThread()

                       GestureHW(context).screenON()

                        if (bEnable) {
                            startService(serviceIntent)
                        }
                        detectThread = null
                    }
                }
            //  Screen ON
                Intent.ACTION_SCREEN_ON -> {
                    if (bRunThread) {
                        bRunThread = false
                        stopThread()
                    }
                }
            }
        }
    }

    val su = ShellSU()
    var bRunThread = false

    private var dlg: AlertDialog? = null
    private fun closeDialog(){
        try{
            dlg?.dismiss()
        }
        catch (e:Exception){ }
        dlg = null
    }
    fun startThread(){

        val hw = GestureHW(this)
        hw.vibrate()

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!su.exec("while(true) do getevent -c 2 -l ; done>&2 &")) return
        if (!su.exec("while(true) do getevent -c 4 -l ; done>&2 &")) return

        var events = arrayOf<String>()
        var passEvents = listOf<String>()

        while(true){
            val line = su.readErrorLine() ?: break
            if (line == "--END_DETECT--") break

            if (!line.contains("EV_KEY")) continue

            if (passEvents.contains(line)) continue
            passEvents += line

            events += line
            hw.vibrate()
        }

        Handler(Looper.getMainLooper()).post {
            reportLog(events)
        }
    }

    fun stopThread()
    {
        thread{
            su.killJobs()
            Thread.sleep(2000)
            su.exec("echo --END_DETECT-->&2")
        }
    }

    private fun reportLog(events:Array<String>)
    {
        val intent = Intent()
        intent.putExtra("geteventResult", events)
        setResult(Activity.RESULT_OK, intent)

        val thisActivity = this

        closeDialog()
        with(AlertDialog.Builder(this))
        {
            val title = getString(R.string.ui_detect2_dlg_title)
            setTitle("$title ${events.size}")
            setMessage(getString(R.string.ui_detect2_dlg_content))

            setOnDismissListener {
                thisActivity.finish()
            }

            dlg = create()
            dlg?.show()
        }
    }
}