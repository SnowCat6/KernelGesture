package gestureDetect.action

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import gestureDetect.GestureAction
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

    override fun onCreate(context: Context):Boolean
    {
        if (onDetectFlashlight(context)) return true

        composites += action.su.su.rxRootEnable
                .filter { it }
                .subscribe {
                    onDetectFlashlight(context)
                    composites.clear()
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
        return false
    }

    override fun onStop() {
        enable = false
        closeCamera()
    }

    override fun close() {
        composites.clear()
        super.close()
    }

    private var bHasFlash = false
    private var flashlightDirect:String? = null
    private var camera:Camera? = null

    var enable : Boolean = false
        set(value) {
            if (field == value) return
            field = value

            if (bHasFlash) {
                if (flashlightDirect != null) flashlightDirect()
                else flashlightCamera()
            }
        }

    private fun flashlightDirect(){
        action.vibrate()
        action.su.exec("echo ${if (enable) 255 else 0} > $flashlightDirect" )
    }
    private fun flashlightCamera()
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
                action.vibrate()
                params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera?.parameters = params
                camera?.startPreview()
            } else {
                action.vibrate()
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