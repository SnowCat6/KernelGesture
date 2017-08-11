package ru.vpro.kernelgesture.detect

import SuperSU.ShellSU
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import gestureDetect.drivers.SensorInput
import ru.vpro.kernelgesture.R
import kotlin.concurrent.thread


class InputDetectActivity : AppCompatActivity() {

    private var logList:ListView? = null
    private var logListAdapter:ArrayAdapter<String>? = null

    private var startButton:Button? = null
    private var sendButton:Button? = null

    private var log = emptyList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_1)

        supportActionBar?.apply {
            subtitle = getString(R.string.ui_detect_subtitle)
            setDisplayHomeAsUpEnabled(true)
        }

        var view: View? = findViewById(R.id.logList)
        logList = view as ListView?

        view = findViewById(R.id.startLog)
        startButton = view as Button?

        view = findViewById(R.id.seendLog)
        sendButton = view as Button?

        startButton?.apply {
            setOnClickListener {

                isEnabled = false
                sendButton?.isEnabled = false

                dlg?.dismiss()
                with(AlertDialog.Builder(context)){
                    setTitle(getString(R.string.ui_detect_dlg_title))
                    setMessage(getString(R.string.ui_detect_dlg_content))
                    setCancelable(false)
                    dlg = create()
                    dlg?.show()
                }

                log = emptyList()
                updateProgress()

                thread{
                    doStartDetect()
                    updateProgress()
                    Handler(Looper.getMainLooper()).post {
                        dlg?.dismiss()
                        isEnabled = true
                        sendButton?.isEnabled = true
                        doStartDetect2()
                    }
                }
            }
        }

        sendButton?.apply {
            isEnabled = false
            setOnClickListener {
                isEnabled = false
                dlg?.dismiss()
                thread {
                    Handler(mainLooper).post {

                        with(AlertDialog.Builder(context))
                        {
                            if (reportError()) {
                                setTitle(getString(R.string.ui_detect_send_title))
                                setMessage(getString(R.string.ui_collect_send_content))
                            } else {
                                setTitle(getString(R.string.ui_detect_send_no_title))
                                setMessage(getString(R.string.ui_detect_send_no_content))
                            }
                            dlg = create()
                            dlg?.show()
                        }

                        isEnabled = true
                    }
                }
            }
        }

    }
    private fun doStartDetect2(){
        InputDetect2Activity.startActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        InputDetect2Activity.getActivityResult(requestCode, resultCode, data)?.apply {
            log += "Gesture events"
            log += this
            updateProgress()
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun reportError(): Boolean {

        val mailAddress = "snowcat6@gmail.com"
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/rfc822"
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailAddress))
        intent.putExtra(Intent.EXTRA_SUBJECT, "AnyKernelGesture log")
        intent.putExtra(Intent.EXTRA_TEXT, log.joinToString("\n"))

        if (intent.resolveActivity(packageManager) == null) return false

        startActivity(Intent.createChooser(intent, "Send Email"))
        return true
    }

    var dlg: android.app.AlertDialog? = null
    private fun doStartDetect() {

        log = emptyList()
        val su = ShellSU()

        log += "Device name:" + android.os.Build.MODEL
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        log += "App version:${pInfo.versionName}"

        log += String()

        log += "Add input devices list"
        SensorInput.getInputEvents().forEach {
            log += "device:${it.second}=>${it.first}"
        }
        log += String()
        updateProgress()

        if (!su.checkRootAccess(this))
        {
            log += "No ROOT access to more search"
            return
        }

        updateProgress()

        log += "Run find cmd to search /sys/ devices"
        doSearch(su, "/sys", listOf("*gesture*", "*gesenable*", "*wakeup_mode*"))

        log += "Run find cmd to search /proc/ functions"
        doSearch(su, "/proc", listOf("*goodix*"))

        /**
        //  Xiaomi gesture mode???
        // ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-0038/wakeup_mode /data/tp/wakeup_mode
        //  ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-004a/wakeup_mode /data/tp/wakeup_mode
         */
        log += "Run find cmd to search /data/tp/ devices"
        doSearch(su, "/data/tp", listOf("*wakeup*"))
    }

    private fun doSearch(su:ShellSU, path:String,search: List<String>):Boolean
    {
        var files = emptyList<String>()

        if (!su.execExists("find")){
            log += "No \"find\" command found, try setup \"BusyBox\" and repeat!"
/*
            var rawCmd = emptyList<String>()
            search.forEach { rawCmd += "-e $it" }
            val cmd = rawCmd.joinToString(" ")

            if (!su.exec("ls -lR $path | grep $cmd")) return false
            if (!su.exec("echo --END--")) return false

            while (true) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                val split = line.split(Regex("\\s\\d{2}:\\d{2}\\s"), 2)
                if (split.size == 2) files += split[1]
            }
            files.forEach {
                log += "path:$it"
            }
 */
        }else {
            var rawCmd = emptyList<String>()
            search.forEach { rawCmd += "-name $it" }
            val cmd = rawCmd.joinToString(" -or ")

            if (!su.exec("find $path $cmd")) return false
            if (!su.exec("echo --END--")) return false

            while (true) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                files += line
            }
            files.forEach {
                val value = su.getFileLine(it)
                log += "path:$it=>$value"
            }
        }

        updateProgress()
        return true
    }
    private fun updateProgress(){
        Handler(Looper.getMainLooper()).post {
            logListAdapter = ArrayAdapter<String>(logList!!.context, android.R.layout.simple_list_item_1, log)
            logList?.adapter = logListAdapter!!
        }
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
