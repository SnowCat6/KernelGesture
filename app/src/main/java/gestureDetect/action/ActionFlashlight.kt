package gestureDetect.action

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import gestureDetect.GestureAction
import gestureDetect.tools.GestureHW
import gestureDetect.tools.RxScreenOn
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

        composites += RxScreenOn(context)
                .filter{ it && enable }
                .subscribe {
                    enable = false
                    flashLight(context)
                }

        if (onDetectFlashlight(context)) return true

        composites += action.su.su.rxRootEnable
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it }
                .subscribe {
                    if (onDetectFlashlight(context)) {
                        action.notifyChanged(this)
                    }
                }

        return bHasFlash
    }
    private fun onDetectFlashlight(context: Context):Boolean
    {
        bHasFlash = false

        if (action.su.checkRootAccess())
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
        return "application.flashlight"
    }
    override fun isEnable(context: Context) : Boolean = bHasFlash

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_flashlight)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_flashlight)

    override fun run(context: Context): Boolean
    {
        enable = !enable
        if (flashLight(context) && enable)
            action.vibrate(context)

        return false
    }

    private fun flashLight(context: Context):Boolean
    {
        if (flashlightDirect != null) return flashlightDirect(context)
        return flashlightCamera(context)
    }

    override fun onStop() {
        enable = false
        context?.also { flashLight(it) }
        context = null
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

    private fun flashlightDirect(context: Context):Boolean {
        action.su.exec("echo ${if (enable) 255 else 0} > $flashlightDirect" )
        return true
    }
    private fun flashlightCamera(context: Context):Boolean
    {
        if (camera == null)
        {
            if (!enable) return true

            camera = try {
                Camera.open()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                closeCamera()
                return false
            }
        }

        try {
            val params = camera?.parameters ?: return false
            if (enable) {
                params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera?.parameters = params
                camera?.startPreview()
            } else {
                params.flashMode = Camera.Parameters.FLASH_MODE_OFF
                camera?.parameters = params
                camera?.stopPreview()
                closeCamera()
            }
            return true
        }catch (e:Exception){}
        return false
    }
    private fun closeCamera(){
        camera?.release()
        camera = null
    }
}