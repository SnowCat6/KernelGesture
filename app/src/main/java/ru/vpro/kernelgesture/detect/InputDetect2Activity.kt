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
import gestureDetect.tools.GestureSettings
import ru.vpro.kernelgesture.R
import kotlin.concurrent.thread


class InputDetect2Activity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_2)

        supportActionBar?.apply {
            subtitle = "Detect gesture events"
            setDisplayHomeAsUpEnabled(true)
        }

        //  Register screen activity event
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onEventIntent, intentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(onEventIntent)
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
        val RESULT_ID = R.layout.activity_detect_2 and 0xFF
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
    val onEventIntent = object : BroadcastReceiver() {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
            //  Screen OFF
                Intent.ACTION_SCREEN_OFF -> {
                    thread {
                        val settings = GestureSettings(context)
                        val bEnable = settings.getAllEnable()
                        settings.setAllEnable(false)
                        Thread.sleep(1000)

                        startThread()

                        if (bEnable) {
                            settings.setAllEnable(bEnable)
                            startService(Intent(context, GestureService::class.java))
                        }
                    }
                }
            //  Screen ON
                Intent.ACTION_SCREEN_ON -> {
                    stopThread()
                }
            }
        }
    }

    val su = ShellSU()
    var bRunThread = false
    var dlg: android.app.AlertDialog? = null

    fun startThread(){

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!su.exec("getevent -l &")) return

        bRunThread = true
        var events = arrayOf<String>()

        while(bRunThread){
            val line = su.readExecLine() ?: break
            if (line == "--END--") break
            if (line.contains("EV_KEY")) {
                events += line
            }
        }

        Handler(Looper.getMainLooper()).post {
            reportLog(events)
        }
    }
    fun stopThread()
    {
        thread{
            su.killJobs()
            Thread.sleep(1000)
            bRunThread = false
            su.exec("echo --END--")
        }
    }
    fun reportLog(events:Array<String>)
    {
        val intent = Intent()
        intent.putExtra("geteventResult", events)
        setResult(Activity.RESULT_OK, intent)

        val thisActivity = this
        dlg?.dismiss()
        with(AlertDialog.Builder(this))
        {
            setTitle("Events captured: ${events.size}")
            setMessage("Tap on screen for close window!")

            setOnDismissListener {
                thisActivity.finish()
            }

            dlg = create()
            dlg?.show()
        }
    }
}