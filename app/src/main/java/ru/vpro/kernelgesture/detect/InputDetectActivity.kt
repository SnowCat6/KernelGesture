package ru.vpro.kernelgesture.detect

import SuperSU.ShellSU
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.ArrayAdapter
import gestureDetect.drivers.SensorInput
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_detect_1.*
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R

class InputDetectActivity : AppCompatActivity() {

    private var logListAdapter : ArrayAdapter<String>? = null
    private var logx        = ArrayList<String>()
    private val composites  = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_1)

        supportActionBar?.apply {
            subtitle = getString(R.string.ui_detect_subtitle)
            setDisplayHomeAsUpEnabled(true)
        }

        logListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, logx)
//        logList?.addHeaderView(listHeader)
        logList?.adapter = logListAdapter

        startLog?.apply {
            setOnClickListener {

                isEnabled = false
                seendLog?.isEnabled = false

                closeDialog()
                with(AlertDialog.Builder(context)) {
                    setTitle(getString(R.string.ui_detect_dlg_title))
                    setMessage(getString(R.string.ui_detect_dlg_content))
                    setCancelable(false)

                    dlg = create()
                    dlg?.setOnDismissListener { dlg = null }
                    dlg?.show()
                }

                logx.clear()
                updateProgress()

                composites += doStartDetect()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe ({
                            logx.add(it)
                            updateProgress()
                        },{},{
                            closeDialog()
                            isEnabled = true
                            seendLog?.isEnabled = true
                            doStartDetect2()
                        })
            }
        }

        seendLog?.apply {
            isEnabled = false
            setOnClickListener {
                isEnabled = false

                closeDialog()
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
                    dlg?.setOnDismissListener {  dlg = null }
                    dlg?.show()
                }
                isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        composites.clear()
        super.onDestroy()
    }

    private fun doStartDetect2(){
        InputDetect2Activity.startActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        InputDetect2Activity.getActivityResult(requestCode, resultCode, data)?.apply {
            logx.add(String())
            logx.add("Gesture events")
            logx.addAll(this)
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
        intent.putExtra(Intent.EXTRA_TEXT, logx.joinToString("\n"))

        if (intent.resolveActivity(packageManager) == null) return false

        startActivity(Intent.createChooser(intent, "Send Email"))
        return true
    }

    private var dlg: android.app.AlertDialog? = null
    private fun closeDialog(){
        try{
            dlg?.dismiss()
        }
        catch (e:Exception){ }
        dlg = null
    }
    private fun doStartDetect()
            = Observable.create<String> { emitter ->

        emitter.apply {

            su.close()
            su.open()

            onNext("Android SDK:" + android.os.Build.VERSION.SDK_INT)
            onNext("Device name:" + android.os.Build.MODEL)

            val pInfo = packageManager.getPackageInfo(packageName, 0)
            onNext("App version:${pInfo.versionName}")

            onNext(String())

            onNext("Add input devices list")
            SensorInput.getInputEvents().forEach {
                onNext("device:${it.second}=>${it.first}")
            }
            onNext(String())

            if (!su.checkRootAccess())
            {
                onNext("No ROOT access to more search, please install SuperSU")
                return@apply
            }

            if (BuildConfig.DEBUG) {
                onComplete()
                return@apply
            }

            onNext("Run find cmd to search /sys/ devices")
            doSearch(this, su, "/sys", listOf("*gesture*", "*gesenable*", "*wakeup_mode*"))

            onNext("Run find cmd to search /proc/ functions")
            doSearch(this, su, "/proc", listOf("*goodix*"))

            /**
            //  Xiaomi gesture mode???
            // ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-0038/wakeup_mode /data/tp/wakeup_mode
            //  ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-004a/wakeup_mode /data/tp/wakeup_mode
             */
            onNext("Run find cmd to search /data/tp/ devices")
            doSearch(this, su, "/data/tp", listOf("*wakeup*"))
        }
        emitter.onComplete()
    }

    private fun doSearch(emitter : ObservableEmitter<String>, su:ShellSU, path:String, search: List<String>):Boolean
    {
        var files = emptyList<String>()

        if (!su.execExists("find")){
            emitter.onNext("No \"find\" command found, try setup \"BusyBox\" and repeat!")
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
                emitter.onNext("path:$it=>$value")
            }
        }

        return true
    }
    private fun updateProgress()
    {
        runOnUiThread {
            logListAdapter?.notifyDataSetChanged()
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

    companion object
    {
        val su  = ShellSU(ShellSU.ProcessSU())

        fun startActivity(context: Context){
            val intent = Intent(context, InputDetectActivity::class.java)
            context.startActivity(intent)
        }
    }
}
