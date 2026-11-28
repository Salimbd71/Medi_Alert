package com.slm.domain

import java.security.MessageDigest

object PinHasher {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("medi_alert_salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank() || inputPin.isBlank()) return false
        return hashPin(inputPin) == storedHash
    }
}
