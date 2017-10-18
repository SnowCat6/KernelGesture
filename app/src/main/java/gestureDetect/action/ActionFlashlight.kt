package gestureDetect.action

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import gestureDetect.GestureAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

class ActionFlashlight(action: GestureAction) : ActionItem(action)
{
    private val composites = CompositeDisposable()
    private val devices = arrayOf(
            "/sys/class/leds/flashlight/brightness"
    )

    private var context : Context? = null
    override fun onCreate(context: Context):Boolean
    {
        this.context = context
        if (onDetectFlashlight(context)) return true

        composites += action.su.su.rxRootEnable
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it }
                .subscribe {
                    onDetectFlashlight(context)
                    composites.clear()
                    action.notifyChanged(this)
                }

        return bHasFlash
    }
    private fun onDetectFlashlight(context: Context):Boolean
    {
        bHasFlash = false

        if (action.su.hasRootProcess())
        {
            for (it in devices) {
                if (!action.su.isFileExists(it)) continue
                flashlightDirect = it
                bHasFlash = true
                return bHasFlash
            }
        }

        if (context.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            try{
                camera = Camera.open()
                closeCamera()
                bHasFlash = true
                return bHasFlash
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        return false
    }

    override fun action(context: Context): String? {
        return if (bHasFlash) "application.flashlight" else ""
    }

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_flashlight)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_flashlight)

    override fun run(context: Context): Boolean
    {
        enable = !enable
        flashLight(context)

        return false
    }

    private fun flashLight(context: Context) {
        if (flashlightDirect != null) flashlightDirect(context)
        else flashlightCamera(context)
    }

    override fun onStop() {
        enable = false
        context?.also { flashLight(it) }
        closeCamera()
    }

    override fun close() {
        composites.clear()
        super.close()
    }

    private var bHasFlash = false
    private var flashlightDirect:String? = null
    private var camera:Camera? = null

    private var enable : Boolean = false

    private fun flashlightDirect(context: Context) {
        action.vibrate(context)
        action.su.exec("echo ${if (enable) 255 else 0} > $flashlightDirect" )
    }
    private fun flashlightCamera(context: Context)
    {
        if (camera == null)
        {
            if (!enable) return

            camera = try {
                Camera.open()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                closeCamera()
                return
            }
        }

        try {
            val params = camera?.parameters ?: return
            if (enable) {
                action.vibrate(context)
                params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera?.parameters = params
                camera?.startPreview()
            } else {
                action.vibrate(context)
                params.flashMode = Camera.Parameters.FLASH_MODE_OFF
                camera?.parameters = params
                camera?.stopPreview()
                closeCamera()
            }
        }catch (e:Exception){
        }
    }
    private fun closeCamera(){
        camera?.release()
        camera = null
    }
}