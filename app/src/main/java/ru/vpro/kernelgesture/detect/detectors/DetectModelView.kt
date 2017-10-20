package ru.vpro.kernelgesture.detect.detectors

import SuperSU.ShellSU
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.FragmentActivity

class DetectModelView(application: Application) :
        AndroidViewModel(application)
{
    val su      = ShellSU(ShellSU.ProcessSU())
    val events  = EventDetector(application, su)
    val inputs  = InputDetector(application, su)

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        fun getModel(activity : FragmentActivity)
                = ViewModelProviders.of(activity).get(DetectModelView::class.java)
    }
}