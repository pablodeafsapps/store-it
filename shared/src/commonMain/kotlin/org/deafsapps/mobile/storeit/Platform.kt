package org.deafsapps.mobile.storeit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform