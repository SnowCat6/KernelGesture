package ru.vpro.kernelgesture

import SuperSU.ShellSU
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.google.firebase.analytics.FirebaseAnalytics
import gestureDetect.drivers.sensor.SensorInput
import kotlin.concurrent.thread



class InputDetectActivity : AppCompatActivity() {

    private var logList:ListView? = null

    private var startButton:Button? = null
    private var sendButton:Button? = null

    private var log = emptyList<String>()
    private var firebaseAnalytics: FirebaseAnalytics? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_detect)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        logList = findViewById(R.id.logList) as ListView?
        startButton = findViewById(R.id.startLog) as Button?
        sendButton = findViewById(R.id.seendLog) as Button?

        startButton?.apply {
            setOnClickListener {
                isEnabled = false
                log = emptyList()
                thread{
                    val log = doStartDetect()
                    Handler(Looper.getMainLooper()).post {
                        isEnabled = true
                        sendButton?.isEnabled = true

                        val listAdapter = ArrayAdapter<String>(logList!!.context, android.R.layout.simple_list_item_1, log)
                        logList?.adapter = listAdapter
                    }
                }
            }
        }

        sendButton?.apply {
            isEnabled = false
            setOnClickListener {
                isEnabled = false
                val bundle = Bundle()
                log.forEach {
                    val ix = it.indexOf(":")
                    if (ix > 0)
                    {
                        val id = it.substring(0, ix)
                        val value = it.substring(ix)
                        bundle.putString(FirebaseAnalytics.Param.GROUP_ID, id)
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, value)
                        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                    }
                }
            }
        }

    }

    var dlg: android.app.AlertDialog? = null
    private fun doStartDetect():List<String> {

        log = emptyList()
        val su = ShellSU()

        log += "Add input devices list"
        SensorInput.getInputEvents().forEach {
            log += "device:${it.second}"
        }
        log += String()

        if (!su.checkRootAccess(this))
        {
            log += "No ROOT access to more search"
            return log
        }

        log += "Run find cmd to search /sys/ devices"
        doSearch(su, "/sys", listOf<String>("*gesture*", "*gesenable*"))

        log += "Run find cmd to search /proc/ functions"
        doSearch(su, "/proc", listOf<String>("*goodix*", "*ts*", "*data*"))

        return log
    }

    private fun doSearch(su:ShellSU, path:String,search: List<String>)
    {
        var rawCmd = emptyList<String>()
        search.forEach { rawCmd += "-name $it" }
        val cmd = rawCmd.joinToString(" -or ")

        if (su.exec("find $path $cmd") && su.exec("echo --END--"))
        {
            while (true) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                log += "path:" + line
            }
        }
        log += String()
    }

    companion object {
        fun startActivity(context: Context){
            val intent = Intent(context, InputDetectActivity::class.java)
            context.startActivity(intent)
        }
    }
}
