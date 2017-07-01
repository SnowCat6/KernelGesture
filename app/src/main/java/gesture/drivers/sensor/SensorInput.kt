package gesture.drivers.sensor

import android.util.Log
import gesture.GestureDetect
import gesture.drivers.input.*
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.FileReader
import kotlin.concurrent.thread

/**
 * Класс для получения событий мз устройств ввода, /dev/input/
 */

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
        if (!GestureDetect.SU.hasRootProcess()) return false

        getInputEvents().forEach { (input, name) ->
            inputHandlers.forEach {
                if (it.onDetect(name)) inputDevices += Pair(input, it)
            }
        }
        gesture.registerDelayEvents("KEY_VOLUMEUP", "KEY_VOLUMEUP_DELAY")
        gesture.registerDelayEvents("KEY_VOLUMEDOWN", "KEY_VOLUMEDOWN_DELAY")

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
                GestureDetect.SU.exec("while v$ix=$(getevent -c 2 -tl $inputName)  ; do for i in 1 2 3 4 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
                ++ix
                GestureDetect.SU.exec("while v$ix=$(getevent -c 4 -tl $inputName)  ; do for i in 1 2 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
            }

            var lastEventTime:Double = 0.0
            GestureDetect.SU.exec("echo query$queryIx>&2")
            while(bRunning)
            {
                //  Read line from input
                val rawLine = GestureDetect.SU.readErrorLine() ?: break

                //  Stop if gesture need stop run
                if (!bRunning) break

                //  Check query number for prevent old events output
                if (!bQueryFound){
                    bQueryFound = rawLine == "query$queryIx"
                    continue
                }

                if (rawLine == "CLOSE_EVENTS")
                    break

                if (gesture.lock) continue
                //  Check device is near screen
                if (gesture.isNear) continue

                val line:String
                val splitLine = rawLine.split(']')
                if (splitLine.size == 2) {

                    line = splitLine[1].trim()
                    //  Check only key events
                    if (!line.contains("EV_KEY")) continue

                    val timeLine = splitLine[0].trim('[').trim().toDoubleOrNull() ?: continue
                    val timeout = timeLine - lastEventTime
                    if (timeout > 0) lastEventTime = timeLine
                    if (timeout <= 0.0) continue
                }else{
                    line  = rawLine
                    //  Detect current input device
                    if (line.contains("/dev/input/")) device = line
                    continue
                }

                val splitEvent = line.split(' ').filter { it.isNotBlank() }
                if (splitEvent.size != 3) continue
                if (splitEvent[2] != "DOWN") continue

                //  Find device for event accept
                for ((first, second) in inputDevices)
                {
                    if (first != device) continue

                    //  Get gesture
                    val gestureEvent = second.onEvent(splitEvent) ?: break

                    if (BuildConfig.DEBUG) {
                        Log.d("SensorInput", line)
                    }

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
