package gestureDetect.drivers.sensor

import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.input.*
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.FileReader
import kotlin.concurrent.thread

/**
 * Класс для получения событий мз устройств ввода, /dev/input/
 */

class SensorInput(gesture: GestureDetect):SensorHandler(gesture)
{
    var bRunning = false
    var bRunThread = false

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputMTK_TPD(gesture), InputMTK_KPD(gesture),
            InputQCOMM_KPD(gesture), InputFT5x06_ts(gesture),
            InputSunXi_KPD(gesture)
    )
    private var inputDevices = emptyList<Pair<String, InputHandler>>()

    override fun enable(bEnable: Boolean)
            = inputDevices.forEach { it.second.setEnable(bEnable) }

    override fun close()
    {
        bRunning = false

        if (bRunThread) {
            bRunThread = false

            gesture.su.exec("kill -s SIGINT %%")
            //  Many execute for flush process buffer
            for (ix in 0..15) gesture.su.exec("echo CLOSE_EVENTS>&2")
        }
    }

    override fun onDetect(): Boolean
    {
        inputDevices = emptyList()

        if (!gesture.su.hasRootProcess()){
            close()
            return false
        }

        getInputEvents().forEach { (input, name) ->
            inputHandlers.forEach {
                if (it.onDetect(name)) inputDevices += Pair(input, it)
            }
        }

        gesture.registerDelayEvents("KEY_VOLUMEUP", "KEY_VOLUMEUP_DELAY")
        gesture.registerDelayEvents("KEY_VOLUMEDOWN", "KEY_VOLUMEDOWN_DELAY")

        gesture.registerScreenEvents("KEY_VOLUMEUP_DELAY", "KEY_VOLUMEUP_DELAY_ON")
        gesture.registerScreenEvents("KEY_VOLUMEDOWN_DELAY", "KEY_VOLUMEDOWN_DELAY_ON")

        startThread()

        return inputDevices.isNotEmpty()
    }

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, inputName:String, nLimit:Int, nRepeat:Int){
        val CR = "\\\\n"
        gesture.su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl $inputName | grep EV_KEY) ; [ \"\$v$ix\" ] && for i in `seq 1 $nRepeat` ; do echo query$queryIx$CR$inputName$CR\"\$v$ix\">&2 ; done ; done &")
    }

    override fun onStart()
    {
        bRunning = true
        if (gesture.su.checkRootAccess(context) && inputDevices.isNotEmpty())
            startThread()
    }

    override fun onStop() {
        bRunning = false
    }

    private fun startThread()
    {
        if (bRunThread || !bRunning) return
        bRunThread = true

        thread{

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Start")
            }

            var device = ""
            var bQueryFound = false
            val queryIx = System.currentTimeMillis()

            //  For each device
            var ix = 0
            inputDevices.forEach { (inputName, device) ->

                //  Run input event detector
                evCmd(queryIx, ++ix, inputName, 2, 5)
                evCmd(queryIx, ++ix, inputName, 4, 2)
            }

            var lastEventTime:Double = 0.0
            var prevLine = String()
            val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)")

            while(bRunThread)
            {
                //  Read line from input
                val rawLine = gesture.su.readErrorLine() ?: break
                //  Stop if gesture need stop run
                if (!bRunThread) break

                if (rawLine == prevLine) continue
                prevLine = rawLine

                //  Check query number for skip old events output
                if (!bQueryFound){
                    bQueryFound = rawLine == "query$queryIx"
                    continue
                }

                if (rawLine == "CLOSE_EVENTS")
                    break

                if (gesture.disabled) continue
                //  Check device is near screen
                if (gesture.isNearProximity) continue

                val ev = regSplit.find(rawLine)
                if (ev == null){
                    //  Detect current input device
                    if (rawLine.contains("/dev/input/")) device = rawLine
                    continue
                }
/*
                Log.d("Sensor", device)
                inputDevices.forEach {
                    Log.d("Sensors", it.first)
                }
*/
                //  Check only key events
                if (ev.groupValues[2] != "EV_KEY") continue
                if (ev.groupValues[4] != "DOWN") continue

                val timeLine = ev.groupValues[1].toDoubleOrNull() ?: continue
                if (timeLine <= lastEventTime) continue
                lastEventTime = timeLine

                inputDevices
                        .find { it.first == device }
                        ?.second?.onEvent(ev.groupValues.subList(2, 4))
                        ?.apply { sensorEvent(this) }
            }
            bRunThread = false

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Exit")
            }
        }
    }

    private fun getInputEvents():List<Pair<String, String>>
    {
        var inputs = emptyList<Pair<String, String>>()
        val regName = Regex("Name=\"(.*)\"")
        val regHandlers = Regex("Handlers=(.*)")

        try {
            var name = ""
            BufferedReader(FileReader("/proc/bus/input/devices"))
                    .forEachLine { line ->

                        regName.find(line)?.apply {
                            name = groupValues[1]
                        }
                        regHandlers.find(line)?.apply {
                            groupValues[1].split(" ")
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
