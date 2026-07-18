package com.managehouse.ingest.service

import java.security.MessageDigest

object Hashing {
    /**
     * externalId estável para dedupe: packageName + texto + minuto. Reposts do Android
     * (mesma notificação repetida em segundos) geram o mesmo hash e não duplicam.
     */
    fun externalId(packageName: String, text: String, timestampMillis: Long): String {
        val minute = timestampMillis / 60000
        val raw = "$packageName|${text.trim()}|$minute"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
