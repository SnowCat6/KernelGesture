package gesture.drivers.sensor

import android.util.Log
import gesture.GestureDetect
import ru.vpro.kernelgesture.BuildConfig
import kotlin.concurrent.thread
import gesture.drivers.input.*
import java.io.BufferedReader
import java.io.FileReader

class SensorInput(gesture: GestureDetect):SensorHandler(gesture)
{
    companion object{
        var queryIx = 0
    }

    var bRunning = false

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputMTK_TPD(gesture), InputMTK_KPD(gesture),
            InputQCOMM_KPD(gesture), InputFT5x06_ts(gesture),
            InputSunXi_KPD(gesture)
    )
    private var inputDevices = emptyArray<Pair<String, InputHandler>>()

    override fun enable(bEnable: Boolean)
    {
        inputDevices.forEach { it.second.setEnable(bEnable) }
    }

    override fun close()
    {
        if (!bRunning) return
        bRunning = false

        GestureDetect.SU.exec("kill -s SIGINT %%")
        //  Many execute for flush process buffer
        for(ix in 0..15) GestureDetect.SU.exec("echo CLOSE_EVENTS>&2")
    }

    override fun onDetect(): Boolean
    {
        if (!GestureDetect.SU.checkRootAccess()) return false

        getInputEvents().forEach { (input, name) ->
            inputHandlers.forEach {
                if (it.onDetect(name)) inputDevices += Pair(input, it)
            }
        }

        return inputDevices.isNotEmpty()
    }

    override fun onStart()
    {
        if (bRunning) return
        bRunning = true

        thread{

            //  Power on gesture if available, many drivers not set this value if screen off
            enable(true)

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Start")
            }

            var device = ""
            var bQueryFound = false
            ++queryIx

            //  For each device
            var ix = 0
            inputDevices.forEach { (inputName, device) ->

                //  Run input event detector
                ++ix
                GestureDetect.SU.exec("while v$ix=$(getevent -c 2 -l $inputName)  ; do for i in 1 2 3 4 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
                ++ix
                GestureDetect.SU.exec("while v$ix=$(getevent -c 4 -l $inputName)  ; do for i in 1 2 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
            }

            GestureDetect.SU.exec("echo query${queryIx}>&2")
            while(bRunning)
            {
                //  Read line from input
                val line = GestureDetect.SU.readErrorLine() ?: break

                //  Stop if gesture need stop run
                if (!bRunning) break

                //  Check query number for prevent old events output
                if (!bQueryFound){
                    bQueryFound = line == "query${queryIx}"
                    continue
                }

                if (line == "CLOSE_EVENTS")
                    break

                if (gesture.lock) continue

                //  Detect current input device
                if (line.contains("/dev/input/")) {
                    device = line
                    continue
                }

                //  Check device is near screen
                if (gesture.isNear) continue

                //  Check only key events
                if (!line.contains("EV_")) continue

                //  Find device for event accept
                for ((first, second) in inputDevices)
                {
                    if (first != device) continue

                    if (BuildConfig.DEBUG) {
                        Log.d("SensorInput", line)
                    }
                    //  Get gesture
                    val gestureEvent = second.onEvent(line) ?: break
                    //  Close cmd events
                    sensorEvent(gestureEvent)
                    break
                }
            }
            bRunning = false

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Exit")
            }
        }
    }

    override fun onStop()
    {
    }

    private fun getInputEvents():Array<Pair<String, String>>
    {
        var inputs = emptyArray<Pair<String, String>>()
        try {
            var name = ""
            BufferedReader(FileReader("/proc/bus/input/devices"))
                    .readLines()
                    .forEach { line ->

                        val iName = line.indexOf("Name=")
                        if (iName >= 0) name = line.substring(iName + 5).trim('"')

                        val iHandlers = line.indexOf("Handlers=")
                        if (iHandlers >= 0)
                        {
                            line.substring(iHandlers + 9)
                                    .split(" ")
                                    .filter { it.contains("event") }
                                    .forEach { inputs += Pair("/dev/input/$it", name) }
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputs
    }
}
