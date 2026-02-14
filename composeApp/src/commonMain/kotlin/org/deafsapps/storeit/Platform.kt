package org.deafsapps.storeit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform