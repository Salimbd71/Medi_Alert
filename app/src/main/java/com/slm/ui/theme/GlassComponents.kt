package com.slm.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slm.data.model.AppLanguage
import com.slm.domain.DailyAdherencePoint
import com.slm.util.TimeFormatter

@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val infinite = rememberInfiniteTransition(label = "ambient_mesh")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh_drift"
    )

    val bgBase = MaterialTheme.colorScheme.background
    val orb1 = if (isDark) Color(0xFF064E3B).copy(alpha = 0.50f) else Color(0xFF86EFAC).copy(alpha = 0.34f)
    val orb2 = if (isDark) Color(0xFF0D4A3E).copy(alpha = 0.44f) else Color(0xFF99F6E4).copy(alpha = 0.36f)
    val orb3 = if (isDark) Color(0xFF162A36).copy(alpha = 0.32f) else Color(0xFFFDE68A).copy(alpha = 0.20f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orb1, Color.Transparent),
                        center = Offset(size.width * (0.15f + 0.12f * drift), size.height * 0.12f),
                        radius = size.width * 0.78f
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * (0.15f + 0.12f * drift), size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orb2, Color.Transparent),
                        center = Offset(size.width * (0.88f - 0.10f * drift), size.height * 0.46f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * (0.88f - 0.10f * drift), size.height * 0.46f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orb3, Color.Transparent),
                        center = Offset(size.width * 0.35f, size.height * (0.88f - 0.06f * drift)),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.35f, size.height * (0.88f - 0.06f * drift))
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    highlightColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val glassBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
            )
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            (highlightColor ?: if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                .copy(alpha = if (isDark) 0.24f else 0.28f),
            MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.26f else 0.34f)
        )
    )

    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
        .clip(shape)
        .background(brush = glassBrush, shape = shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Surface(
        modifier = baseModifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AdherenceRingChart(
    percentage: Int,
    language: AppLanguage,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "adherence_ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val activeColor = when {
        percentage >= 80 -> EmeraldTaken
        percentage >= 50 -> AmberWarning
        else -> CoralAlert
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(activeColor, MintAccent, activeColor)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${TimeFormatter.localizeDigits(percentage.toString(), language)}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdherenceBarChart(
    points: List<DailyAdherencePoint>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val displayPoints = if (points.size > 14) points.takeLast(14) else points
    val barBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
    val primaryBar = MaterialTheme.colorScheme.primary
    val secondaryBar = MintAccent

    val revealProgress = remember { Animatable(0f) }
    val animationKey = remember(displayPoints) {
        displayPoints.map { "${it.epochDay}:${it.percentage}:${it.expectedCount}" }
    }
    LaunchedEffect(animationKey) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        ) {
            if (displayPoints.isEmpty()) return@Canvas
            val count = displayPoints.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.52f).coerceIn(8.dp.toPx(), 26.dp.toPx())
            val globalProgress = revealProgress.value

            displayPoints.forEachIndexed { index, point ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = barBg,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
                val staggerStart = (index.toFloat() / count.coerceAtLeast(1)) * 0.30f
                val localProgress = ((globalProgress - staggerStart) / (1f - staggerStart)).coerceIn(0f, 1f)
                val ratio = (point.percentage / 100f).coerceIn(0f, 1f) * localProgress
                val baseMinHeight = if (point.expectedCount > 0 && point.percentage > 0) 6.dp.toPx() else 0f
                val barHeight = (size.height * ratio).coerceAtLeast(if (localProgress > 0.05f) baseMinHeight else 0f)
                if (barHeight > 0f) {
                    val color = when {
                        point.percentage >= 80 -> EmeraldTaken
                        point.percentage >= 50 -> secondaryBar
                        else -> CoralAlert
                    }
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(color, primaryBar)),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (displayPoints.isNotEmpty()) {
                Text(
                    text = TimeFormatter.formatEpochDay(displayPoints.first().epochDay, language, "dd MMM"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeFormatter.formatEpochDay(displayPoints.last().epochDay, language, "dd MMM"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun mediAlertFilterChipColors(): SelectableChipColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val unselectedBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    return FilterChipDefaults.filterChipColors(
        containerColor = unselectedBg,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.primary,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
fun mediAlertFilterChipBorder(selected: Boolean): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val selectedBorder = if (isDark) Color(0xFF6EE7B7) else EmeraldDeep
    return FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.78f),
        selectedBorderColor = selectedBorder,
        borderWidth = 1.dp,
        selectedBorderWidth = 1.5.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mediAlertInputChipColors(): SelectableChipColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val unselectedBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    return InputChipDefaults.inputChipColors(
        containerColor = unselectedBg,
        labelColor = MaterialTheme.colorScheme.onSurface,
        leadingIconColor = MaterialTheme.colorScheme.primary,
        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mediAlertInputChipBorder(selected: Boolean): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val selectedBorder = if (isDark) Color(0xFF6EE7B7) else EmeraldDeep
    return InputChipDefaults.inputChipBorder(
        enabled = true,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.78f),
        selectedBorderColor = selectedBorder,
        borderWidth = 1.dp,
        selectedBorderWidth = 1.5.dp
    )
}

@Composable
fun mediAlertSegmentedButtonColors(): SegmentedButtonColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val selectedBorder = if (isDark) Color(0xFF6EE7B7) else EmeraldDeep
    val unselectedBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    return SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = selectedBorder,
        inactiveContainerColor = unselectedBg,
        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
        inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.82f)
    )
}

@Composable
fun mediAlertSwitchColors(): SwitchColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    return SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = if (isDark) Color(0xFF6EE7B7) else EmeraldDeep,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun mediAlertNavBarItemColors(): NavigationBarItemColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val selectedLabel = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
    return NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = selectedLabel,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun mediAlertNavRailItemColors(): NavigationRailItemColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val selectedLabel = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
    return NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = selectedLabel,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
