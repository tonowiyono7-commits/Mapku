package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SpeedMode
import com.example.location.GeoMath
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.JoystickInnerGlow
import com.example.ui.theme.JoystickOuterRing
import com.example.ui.theme.MapBlue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun JoystickOverlay(
    speedMode: SpeedMode,
    activeSpeedKmh: Float,
    liveSpeedKmh: Float,
    bearing: Float,
    isCruiseLocked: Boolean,
    joystickDx: Float,
    joystickDy: Float,
    isJoystickOnLeft: Boolean,
    isSpoofingActive: Boolean,
    onVectorChanged: (Float, Float) -> Unit,
    onSelectSpeedMode: (SpeedMode) -> Unit,
    onToggleCruiseLock: () -> Unit,
    onSwapSide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnVectorChanged by rememberUpdatedState(onVectorChanged)
    val density = LocalDensity.current
    val padSize = 144.dp
    val knobSize = 52.dp
    val maxRadiusPx = with(density) { ((padSize - knobSize) / 2f - 4.dp).toPx() }

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val targetOffset = when {
        isDragging -> dragOffset
        isCruiseLocked -> Offset(joystickDx * maxRadiusPx, joystickDy * maxRadiusPx)
        else -> Offset.Zero
    }

    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "joystick_knob_offset"
    )

    val activeAccent by animateColorAsState(
        targetValue = if (isSpoofingActive) ActiveEmerald else MapBlue,
        label = "joystick_accent"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = if (isJoystickOnLeft) Alignment.Start else Alignment.End
    ) {
        // Live telemetry & speed gear pill
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xE6181E27),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displaySpeed = if (liveSpeedKmh > 0.2f) liveSpeedKmh else activeSpeedKmh
                    val compass = GeoMath.compassLabelIndonesian(bearing)
                    Text(
                        text = String.format(
                            java.util.Locale.US,
                            "%.1f km/j • %03d° %s",
                            displaySpeed,
                            bearing.toInt(),
                            compass
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedGearChip(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = stringResource(R.string.speed_walk),
                        selected = speedMode == SpeedMode.WALK,
                        activeColor = activeAccent,
                        testTag = "speed_walk_chip",
                        onClick = { onSelectSpeedMode(SpeedMode.WALK) }
                    )
                    SpeedGearChip(
                        icon = Icons.AutoMirrored.Filled.DirectionsBike,
                        label = stringResource(R.string.speed_cycle),
                        selected = speedMode == SpeedMode.CYCLE,
                        activeColor = activeAccent,
                        testTag = "speed_cycle_chip",
                        onClick = { onSelectSpeedMode(SpeedMode.CYCLE) }
                    )
                    SpeedGearChip(
                        icon = Icons.Filled.DirectionsCar,
                        label = stringResource(R.string.speed_drive),
                        selected = speedMode == SpeedMode.DRIVE,
                        activeColor = activeAccent,
                        testTag = "speed_drive_chip",
                        onClick = { onSelectSpeedMode(SpeedMode.DRIVE) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Joystick disc + side utility buttons (Cruise Lock & Swap Side)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isJoystickOnLeft) {
                JoystickSideControls(
                    isCruiseLocked = isCruiseLocked,
                    activeAccent = activeAccent,
                    onToggleCruiseLock = onToggleCruiseLock,
                    onSwapSide = onSwapSide
                )
            }

            Box(
                modifier = Modifier
                    .size(padSize)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(JoystickOuterRing)
                    .testTag("joystick_pad")
                    .semantics { contentDescription = "Kontrol joystick arah lokasi" }
                    .pointerInput(maxRadiusPx) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffset = Offset.Zero
                                if (!isCruiseLocked) {
                                    currentOnVectorChanged(0f, 0f)
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = Offset.Zero
                                if (!isCruiseLocked) {
                                    currentOnVectorChanged(0f, 0f)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val raw = dragOffset + dragAmount
                                val dist = hypot(raw.x, raw.y)
                                val clamped = if (dist > maxRadiusPx && dist > 0f) {
                                    Offset(raw.x / dist * maxRadiusPx, raw.y / dist * maxRadiusPx)
                                } else {
                                    raw
                                }
                                dragOffset = clamped
                                val nx = (clamped.x / maxRadiusPx).coerceIn(-1f, 1f)
                                val ny = (clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                                currentOnVectorChanged(nx, ny)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerRadius = size.minDimension / 2f - 4.dp.toPx()

                    // Outer luminous rim
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f),
                        radius = outerRadius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Inner guidance ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = outerRadius * 0.56f,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Cardinal crosshair ticks
                    val tickOuter = outerRadius - 3.dp.toPx()
                    val tickInner = outerRadius - 12.dp.toPx()
                    for (deg in listOf(0f, 90f, 180f, 270f)) {
                        val rad = Math.toRadians(deg.toDouble())
                        val x1 = cx + (cos(rad) * tickInner).toFloat()
                        val y1 = cy + (sin(rad) * tickInner).toFloat()
                        val x2 = cx + (cos(rad) * tickOuter).toFloat()
                        val y2 = cy + (sin(rad) * tickOuter).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Active directional sector glow
                    val knobMag = hypot(animatedOffset.x, animatedOffset.y) / maxRadiusPx
                    if (knobMag > 0.08f) {
                        val angleDeg = Math.toDegrees(
                            atan2(animatedOffset.y.toDouble(), animatedOffset.x.toDouble())
                        ).toFloat()
                        drawArc(
                            color = activeAccent.copy(alpha = (0.35f * knobMag).coerceIn(0.1f, 0.45f)),
                            startAngle = angleDeg - 32f,
                            sweepAngle = 64f,
                            useCenter = true,
                            topLeft = Offset(cx - outerRadius, cy - outerRadius),
                            size = Size(outerRadius * 2f, outerRadius * 2f)
                        )
                    }

                    // Tether line from center to thumb knob
                    val knobCenter = Offset(cx + animatedOffset.x, cy + animatedOffset.y)
                    if (knobMag > 0.05f) {
                        drawLine(
                            color = JoystickInnerGlow.copy(alpha = 0.7f),
                            start = Offset(cx, cy),
                            end = knobCenter,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Thumb knob with radial highlight
                    val knobRadius = knobSize.toPx() / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFD8E4F8)),
                            center = knobCenter - Offset(knobRadius * 0.2f, knobRadius * 0.25f),
                            radius = knobRadius * 1.3f
                        ),
                        radius = knobRadius,
                        center = knobCenter
                    )
                    drawCircle(
                        color = activeAccent,
                        radius = knobRadius * 0.52f,
                        center = knobCenter
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = knobRadius * 0.18f,
                        center = knobCenter
                    )
                }

                // Cardinal labels ("U" at top)
                Text(
                    text = "U",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }

            if (isJoystickOnLeft) {
                JoystickSideControls(
                    isCruiseLocked = isCruiseLocked,
                    activeAccent = activeAccent,
                    onToggleCruiseLock = onToggleCruiseLock,
                    onSwapSide = onSwapSide
                )
            }
        }
    }
}

@Composable
private fun JoystickSideControls(
    isCruiseLocked: Boolean,
    activeAccent: Color,
    onToggleCruiseLock: () -> Unit,
    onSwapSide: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (isCruiseLocked) activeAccent else Color(0xE6181E27),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(44.dp)
                .minimumInteractiveComponentSize()
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
        ) {
            IconButton(
                onClick = onToggleCruiseLock,
                modifier = Modifier.testTag("cruise_lock_button")
            ) {
                Icon(
                    imageVector = if (isCruiseLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = stringResource(R.string.action_cruise_lock),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = Color(0xE6181E27),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(44.dp)
                .minimumInteractiveComponentSize()
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
        ) {
            IconButton(
                onClick = onSwapSide,
                modifier = Modifier.testTag("swap_joystick_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = stringResource(R.string.action_swap_joystick),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedGearChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeColor else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
