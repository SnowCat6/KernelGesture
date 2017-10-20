package ru.vpro.kernelgesture.detect

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Button
import kotlinx.android.synthetic.main.activity_detect_1.*
import org.inowave.planning.ui.common.adapter.HeaderString
import ru.vpro.kernelgesture.tools.adapter.bindAdapter
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.detect.detectors.DetectModelView
import ru.vpro.kernelgesture.tools.adapter.ReAdapter

class InputDetectActivity : AppCompatActivity() {

    private var logx    = ArrayList<Any>()
    private var adapter = ReAdapter(logx)

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_1)

        supportActionBar?.apply {
            subtitle = getString(R.string.ui_detect_subtitle)
            setDisplayHomeAsUpEnabled(true)
        }

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
                onButtonDetect(this)
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

        DetectModelView.getModel(this).inputs.let {
            it.onComplete {

                closeDialog()
                startLog?.isEnabled = true
                seendLog?.isEnabled = true

                InputDetect2Activity.startActivity(this)
            }

            it.observe(this, Observer {
                it?.let {
                    logx.clear()
                    logx.addAll(it)
                    updateProgress()
                }
            })
        }
        adapter.addHeaderView(buttonLayout)
        adapter.addHeaderView(listHeader)
        logList?.bindAdapter(adapter)
    }
    private fun onButtonDetect(button : Button)
    {
        DetectModelView.getModel(this).inputs.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        logx.add(HeaderString("Gesture events"))
        logx.addAll(InputDetect2Activity.resilt)
        updateProgress()

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

    private fun updateProgress()
    {
        adapter.items = logx
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
        fun startActivity(context: Context){
            val intent = Intent(context, InputDetectActivity::class.java)
            context.startActivity(intent)
        }
    }
}
