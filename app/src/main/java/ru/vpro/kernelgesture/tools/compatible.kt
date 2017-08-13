package ru.vpro.kernelgesture.tools

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build

fun getDrawable(context:Context, nResID:Int):Drawable
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return context.getDrawable(nResID)
    return context.resources.getDrawable(nResID)
}
