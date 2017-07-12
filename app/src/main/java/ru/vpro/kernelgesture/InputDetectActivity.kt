package ru.vpro.kernelgesture

import SuperSU.ShellSU
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.google.firebase.analytics.FirebaseAnalytics
import gestureDetect.drivers.SensorInput
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

        supportActionBar?.apply {
            subtitle = "Detect and sent to developer"
            setDisplayHomeAsUpEnabled(true)
        }

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
                thread {
                    reportError()
                    Handler(mainLooper).post {
                        with(android.app.AlertDialog.Builder(context))
                        {
                            setTitle("Report send")
                            setMessage("Thanks you for report!")
                            dlg = create()
                            dlg?.show()
                        }
                    }
                }
            }
        }

    }

    private fun reportError()
    {
/*
        log.forEach {
            val ix = it.indexOf(":")
            if (ix > 0)
            {
                val bundle = Bundle()
                val type = it.substring(0, ix)
                val value = it.substring(ix+1)
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, value)
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }
        }
*/
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/rfc822"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("vpro@vpro.ru"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "AnyKernelGesture log")
        intent.putExtra(Intent.EXTRA_TEXT, log.joinToString("\n"))

        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    var dlg: android.app.AlertDialog? = null
    private fun doStartDetect():List<String> {

        log = emptyList()
        val su = ShellSU()

        log += "Device name:" + android.os.Build.MODEL
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        log += "App version:${pInfo.versionName}"

        log += String()

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
        doSearch(su, "/sys", listOf("*gesture*", "*gesenable*"))

        log += "Run find cmd to search /proc/ functions"
        doSearch(su, "/proc", listOf("*goodix*"))

        return log
    }

    private fun doSearch(su:ShellSU, path:String,search: List<String>)
    {
        var rawCmd = emptyList<String>()
        search.forEach { rawCmd += "-name $it" }
        val cmd = rawCmd.joinToString(" -or ")

        var files = emptyList<String>()
        if (su.exec("find $path $cmd") && su.exec("echo --END--"))
        {
            while (true) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                files += line
            }
        }
        files.forEach {
            val value = su.getFileLine(it)
            log += "path:$it=>$value"
        }

        log += String()
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

    companion object {
        fun startActivity(context: Context){
            val intent = Intent(context, InputDetectActivity::class.java)
            context.startActivity(intent)
        }
    }
}
