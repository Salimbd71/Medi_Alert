package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.model.AppLanguage
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GlassBackdrop
import com.example.ui.theme.GlassCard
import com.example.util.TimeFormatter

@Composable
fun AppLockScreen(
    language: AppLanguage,
    hasPinLock: Boolean,
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(false) }

    val canUseBiometric = remember(context, biometricEnabled) {
        if (!biometricEnabled) false
        else {
            val manager = BiometricManager.from(context)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    fun launchBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strings.appName)
            .setSubtitle(strings.unlockWithBiometric)
            .setNegativeButtonText(strings.cancel)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            launchBiometricPrompt()
        }
    }

    GlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!hasPinLock && canUseBiometric) {
                                Icons.Rounded.Fingerprint
                            } else {
                                Icons.Rounded.Lock
                            },
                            contentDescription = strings.securitySection,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPinLock) strings.enterPinPrompt else strings.unlockWithBiometric,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (hasPinLock) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            repeat(4) { index ->
                                val filled = index < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (filled) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        AnimatedVisibility(visible = errorMessage) {
                            Text(
                                text = strings.wrongPinError,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralAlert
                            )
                        }

                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("BIO", "0", "DEL")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { key ->
                                        when (key) {
                                            "BIO" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .clickable(enabled = canUseBiometric) {
                                                            launchBiometricPrompt()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (canUseBiometric) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Fingerprint,
                                                            contentDescription = strings.unlockWithBiometric,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(30.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "DEL" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            if (enteredPin.isNotEmpty()) {
                                                                enteredPin = enteredPin.dropLast(1)
                                                                errorMessage = false
                                                            }
                                                        }
                                                        .testTag("pin_backspace"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                                        contentDescription = strings.delete,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            else -> {
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                        .clickable {
                                                            if (enteredPin.length < 4) {
                                                                val next = enteredPin + key
                                                                enteredPin = next
                                                                errorMessage = false
                                                                if (next.length == 4) {
                                                                    if (!onVerifyPin(next)) {
                                                                        errorMessage = true
                                                                        enteredPin = ""
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .testTag("pin_key_$key"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = TimeFormatter.localizeDigits(key, language),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (canUseBiometric || !hasPinLock) {
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = { launchBiometricPrompt() },
                            modifier = Modifier.testTag("biometric_unlock_button")
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(strings.unlockWithBiometric)
                        }
                    }
                }
            }
        }
    }
}
