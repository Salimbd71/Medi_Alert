package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldTaken
import com.example.ui.theme.GlassBackdrop
import com.example.ui.theme.GlassCard
import com.example.ui.theme.MintAccent
import com.example.ui.theme.StatusPill
import com.example.util.PermissionHealthState

@Composable
fun AnimatedSplashScreen() {
    val strings = LocalAppStrings.current
    val infinite = rememberInfiniteTransition(label = "splash_loader")
    val spinAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_spin"
    )
    val pulseScale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_pulse"
    )

    val logoScale = remember { Animatable(0.72f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    GlassBackdrop(
        modifier = Modifier.testTag("splash_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(144.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                    ) {
                        val outerStroke = 5.dp.toPx()
                        val outerInset = outerStroke / 2f
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(outerInset, outerInset),
                            size = Size(size.width - outerStroke, size.height - outerStroke),
                            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MintAccent.copy(alpha = 0.65f),
                                    primaryColor
                                )
                            ),
                            startAngle = spinAngle,
                            sweepAngle = 250f,
                            useCenter = false,
                            topLeft = Offset(outerInset, outerInset),
                            size = Size(size.width - outerStroke, size.height - outerStroke),
                            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                        )

                        val innerStroke = 2.5.dp.toPx()
                        val innerMargin = 14.dp.toPx()
                        drawArc(
                            color = MintAccent.copy(alpha = 0.55f),
                            startAngle = -spinAngle * 1.4f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(innerMargin, innerMargin),
                            size = Size(size.width - innerMargin * 2, size.height - innerMargin * 2),
                            style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_medi_alert_fg),
                        contentDescription = strings.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .scale(logoScale.value)
                            .clip(CircleShape)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.appSlogan,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomePermissionDialog(
    permissionHealth: PermissionHealthState,
    showFollowUpState: Boolean,
    onContinueClick: () -> Unit,
    onRequestExactAlarmClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSkipOrFinishClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .testTag("welcome_permission_dialog"),
                cornerRadius = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_medi_alert_fg),
                            contentDescription = strings.appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showFollowUpState) strings.onboardingDeniedTitle else strings.welcomeOnboardingTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (showFollowUpState) strings.onboardingDeniedDesc else strings.welcomeOnboardingSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OnboardingPermissionItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = strings.onboardingNotifTitle,
                        description = strings.onboardingNotifDesc,
                        isGranted = permissionHealth.hasNotificationPermission,
                        grantedLabel = strings.granted
                    )

                    OnboardingPermissionItem(
                        icon = Icons.Rounded.Alarm,
                        title = strings.onboardingExactAlarmTitle,
                        description = strings.onboardingExactAlarmDesc,
                        isGranted = permissionHealth.canScheduleExactAlarms,
                        grantedLabel = strings.granted
                    )

                    // Clear notice of what will be limited if permission is not granted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmberWarning.copy(alpha = 0.14f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.onboardingLimitedTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.onboardingLimitedDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!showFollowUpState) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onContinueClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_continue_button")
                            ) {
                                Text(strings.continueButtonLabel, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = onSkipOrFinishClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_skip_button")
                            ) {
                                Text(strings.skipForNowLabel)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!permissionHealth.hasNotificationPermission) {
                                FilledTonalButton(
                                    onClick = onOpenSettingsClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("onboarding_open_settings_button")
                                ) {
                                    Text(strings.openAppSettingsLabel)
                                }
                            } else if (!permissionHealth.canScheduleExactAlarms) {
                                FilledTonalButton(
                                    onClick = onRequestExactAlarmClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("onboarding_exact_alarm_button")
                                ) {
                                    Text(strings.allowExactAlarmLabel)
                                }
                            }

                            Button(
                                onClick = onSkipOrFinishClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_finish_button")
                            ) {
                                Text(strings.continueToDashboardLabel, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    grantedLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) EmeraldTaken.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                contentDescription = title,
                tint = if (isGranted) EmeraldTaken else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isGranted) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill(
                        text = grantedLabel,
                        containerColor = EmeraldTaken.copy(alpha = 0.16f),
                        contentColor = EmeraldTaken
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
