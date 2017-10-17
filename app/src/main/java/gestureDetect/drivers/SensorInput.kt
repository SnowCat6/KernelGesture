package gestureDetect.drivers

import gestureDetect.GestureDetect
import gestureDetect.drivers.input.*
import gestureDetect.tools.GestureHW
import gestureDetect.tools.InputReader
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.FileReader

/**
 * Класс для получения событий мз устройств ввода, /dev/input/
 */

class SensorInput(gesture: GestureDetect): SensorHandler(gesture)
{
    private val rxInputReader = InputReader.getInstance(context)

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputTouchscreen(gesture),
            InputQCOMM_KPD(gesture),
            InputSunXi_KPD(gesture),
            InputCypressTPD(gesture)
    )

    private var inputDevices = emptyList<Pair<String, InputHandler>>()
    private val composites = CompositeDisposable()
    private val compositesEv = CompositeDisposable()

    override fun enable(bEnable: Boolean)
            = inputDevices.forEach { it.second.setEnable(bEnable) }

    override fun close()
    {
        composites.clear()
        compositesEv.clear()
    }

    override fun onCreate()
    {
        if (composites.size() != 0) return

        composites += gesture.su.su.rxRootEnable
            .filter { it }
            .subscribe {

                inputDevices = emptyList()

                getInputEvents().forEach { (input, name) ->
                    inputHandlers
                        .filter { it.onDetect(name) }
                        .forEach { inputDevices += Pair(input, it) }
                }
                rxInputReader.setDevices(inputDevices.map { it.first })
            }

        composites += GestureHW.rxScreenOn
            .filter { it }
            .observeOn(Schedulers.io())
            .subscribe { enable(true) }
    }

    override fun onResume()
    {
        if (compositesEv.size() != 0) return

        compositesEv += rxInputReader
            .filter { !gesture.isNearProximity }
            .subscribe { evInput ->

                inputDevices
                    .find { it.first == evInput.device }
                    ?.second?.onEvent(evInput)
                    ?.also { sensorEvent(it) }
            }
    }

    companion object
    {
        fun getInputEvents():List<Pair<String, String>>
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
                            } ?:
                            regHandlers.find(line)?.apply {
                                groupValues[1]
                                        .split(" ")
                                        .filter { it.startsWith("event") }
                                        .forEach { inputs += Pair("/dev/input/$it", name) }
                            }
                        }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return inputs
        }
    }
}
