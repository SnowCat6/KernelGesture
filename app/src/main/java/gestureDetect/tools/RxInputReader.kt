package gestureDetect.tools

import SuperSU.ShellSU
import android.content.Context
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers

class RxInputReader(val su: ShellSU = ShellSU())
    : Observable<RxInputReader.EvData>()
{
    data class EvData(
            val device      : String,
            val evName      : String,
            val evButton    : String,
            val evPress     : String,
            val evTimeout   : Int
    )

    private var inputDevices  = emptyList<String>()
    private val observers = mutableListOf<Observer<in EvData>>()
    private val composites = CompositeDisposable()
    private var cmdName : String? = null

    fun create(context: Context){

        val reader = UnpackEventReader(context)
        cmdName = reader.copyResourceTo(
            "EventReader.so",
            "EventReader")
            ?.also { chmod(it) }
    }
    private fun chmod(it : String){
        su.exec("chmod 777 $it")
    }

    fun setDevices(devices : List<String>)
    {
        if (inputDevices == devices && composites.size() > 0)
            return

        inputDevices = devices
        composites.clear()
        if (devices.isEmpty()) return
        if (!hasObservers()) return

        composites += su.su.rxRootEnable
                .filter { it }
                .subscribe {
                    cmdName?.also { chmod(it) }
                    if (cmdName != null) doWorkNew()
                    else doWork()
                }

        if (!su.checkRootAccess()) return
    }
    private fun doWorkNew()
    {
        su.readErrorLine()?.also {

            val arg = inputDevices.joinToString(" ")
            su.exec("$cmdName $arg&")

            val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+):\\s+([^\\s]+)\\s+([^\\s]+)\\s*([^\\s]+)")

            composites += it
                    .subscribeOn(Schedulers.io())
                    .filter{ hasObservers() }
                    .onErrorReturn { null }
                    .subscribe {

                val ev = regSplit.find(it) ?: return@subscribe

                if (ev.groupValues[3] != "EV_KEY") return@subscribe
                val timeout = ev.groupValues[1].toDoubleOrNull() ?: 0.0

                EvData(
                        ev.groupValues[2],
                        ev.groupValues[3],
                        ev.groupValues[4],
                        ev.groupValues[5],
                        (timeout * 1000).toInt()
                ).also {
                    onNext(it)
                }
            }
        }
    }
    private fun doWork()
    {
        su.readErrorLine()?.also {

            var device = ""
            var bQueryFound = false
            val queryIx = System.currentTimeMillis()

            //  For each device
            var ix = 0
            inputDevices.forEach {

                //  Run input event detector
                evCmd(queryIx, ++ix, it, 2, 4)
                evCmd(queryIx, ++ix, it, 4, 2)
            }

            val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)")
            var lastEventTime = 0.0
            var lastKeyEventTime = 0.0
            var eqEvents = emptyList<String>()

            composites += it
                    .subscribeOn(Schedulers.io())
                    .filter{ hasObservers() }
                    .onErrorReturn { null }
                    .subscribe { rawLine ->

                        //  Check query number for skip old events output
                        if (!bQueryFound){
                            bQueryFound = rawLine == "query$queryIx"
                            return@subscribe
                        }
                        //  Check device is near screen
                        if (eqEvents.contains(rawLine))
                            return@subscribe

                        if (rawLine.contains("/dev/input/")) {
                            device = rawLine
                            return@subscribe
                        }

                        val ev = regSplit.find(rawLine) ?: return@subscribe
                        eqEvents += rawLine

                        val timeLine = ev.groupValues[1].toDoubleOrNull() ?: return@subscribe
                        var timeout = timeLine - lastEventTime

                        if (timeout < -1000.0) timeout = 1.0
                        if (timeout < 0) return@subscribe
                        if (timeout > 0) eqEvents = listOf(rawLine)
                        lastEventTime = timeLine

                        if (ev.groupValues[2] != "EV_KEY") return@subscribe

                        EvData(
                                device,
                                ev.groupValues[2],
                                ev.groupValues[3],
                                ev.groupValues[4],
                                ((timeLine - lastKeyEventTime) * 1000).toInt())
                                .also {
                                    lastKeyEventTime = timeLine
                                    onNext(it)
                                }

                    }
        }
    }

    override fun subscribeActual(observer: Observer<in EvData>?)
    {
        observer?.also {
            val size = synchronized(observers) {
                observers.add(observer)
                val dispose = EvDispose(observer)
                observer.onSubscribe(dispose)
                observers.size
            }
            if (size == 1) setDevices(inputDevices)
        }
    }
    inner class EvDispose(private val observer: Observer<in EvData>)
        : Disposable
    {
        override fun isDisposed(): Boolean {
            return synchronized(observers) {
                observers.contains(observer)
            }
        }

        override fun dispose() {
            val bDispose = synchronized(observers) {
                if (observers.size > 0) {
                    observers.remove(observer)
                    observers.size == 0
                }else false
            }
            if (bDispose) onDispose()
        }
    }

    private fun onDispose()
    {
        composites.clear()
        //  todo: sent any messages to flush buffer and self kill threads
        su.killJobs()
    }

    private fun onNext(event : EvData)
    {
        synchronized(observers){ observers.subList(0, observers.size) }
        .forEach { it.onNext(event) }
    }
    fun hasObservers()
        = observers.size > 0

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, device : String, nLimit : Int, nRepeat : Int)
    {
        if (nLimit > 0) {
            val CR = "\\\\n"
            val seq = (1..nRepeat).joinToString(" ")
            su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl $device | grep EV_KEY) ; [ \"\$v$ix\" ] && for i in $seq ; do echo query$queryIx$CR$device$CR\"\$v$ix\">&2 ; done ; done &")
        }else{
            su.exec("getevent -tl $device | grep EV_KEY>&2 &")
        }
    }

    companion object
    {
        private var rxInputReader : RxInputReader? = null
        fun getInstance(context: Context)
            = rxInputReader ?: RxInputReader().apply{

                rxInputReader = this
                create(context)
            }
    }
}