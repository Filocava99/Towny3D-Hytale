package it.cava99.towny3d.util

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Simple logging utility for Towny3D.
 * Uses Hytale's console for output with consistent formatting.
 */
object TownyLog {
    private val timestampFormat = DateTimeFormatter.ISO_INSTANT

    private fun timestamp(): String = timestampFormat.format(Instant.now())

    fun info(message: String) {
        println("[Towny3D INFO] ${timestamp()} - $message")
    }

    fun warn(message: String) {
        println("[Towny3D WARN] ${timestamp()} - $message")
    }

    fun error(message: String, throwable: Throwable? = null) {
        println("[Towny3D ERROR] ${timestamp()} - $message")
        throwable?.printStackTrace()
    }

    fun debug(message: String) {
        // Debug logging disabled in production
    }
}
