package org.deafsapps.storeit

import android.os.Build
import org.deafsapps.storeit.Platform

internal class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

internal actual fun getPlatform(): Platform = AndroidPlatform()