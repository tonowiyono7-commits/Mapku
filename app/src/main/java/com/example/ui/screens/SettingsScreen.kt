package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GeoJoySettings
import com.example.ui.MainViewModel
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MapBlue
import com.example.ui.theme.WarningAmber
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settings: GeoJoySettings,
    isDeveloperMockAuthorized: Boolean,
    onNavigateBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackToMap()
    }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Developer Options Mock Status Card
            SectionHeader(
                icon = Icons.Filled.DeveloperMode,
                title = stringResource(R.string.settings_dev_section)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isDeveloperMockAuthorized) ActiveEmerald.copy(alpha = 0.5f)
                    else WarningAmber.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDeveloperMockAuthorized) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = if (isDeveloperMockAuthorized) ActiveEmerald else WarningAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDeveloperMockAuthorized) {
                                    "Opsi Pengembang Terhubung"
                                } else {
                                    "GeoJoy Belum Dipilih sebagai Mock App"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isDeveloperMockAuthorized) {
                                    "Perangkat siap menerima injeksi lokasi GPS palsu."
                                } else {
                                    "Pilih GeoJoy pada menu Pilih aplikasi lokasi palsu di Opsi Pengembang."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshAuthorization() },
                            modifier = Modifier.testTag("refresh_dev_auth_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Periksa ulang status"
                            )
                        }
                    }

                    if (!isDeveloperMockAuthorized) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { openDeveloperSettings(context) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MapBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_dev_settings_button")
                        ) {
                            Text(stringResource(R.string.action_open_dev_settings))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Speed Configuration
            SectionHeader(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.settings_speed_section)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SpeedSliderRow(
                        label = stringResource(R.string.speed_walk),
                        value = settings.walkSpeedKmh,
                        range = 1f..15f,
                        onValueChange = viewModel::setWalkSpeed
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SpeedSliderRow(
                        label = stringResource(R.string.speed_cycle),
                        value = settings.cycleSpeedKmh,
                        range = 10f..45f,
                        onValueChange = viewModel::setCycleSpeed
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SpeedSliderRow(
                        label = stringResource(R.string.speed_drive),
                        value = settings.driveSpeedKmh,
                        range = 25f..140f,
                        onValueChange = viewModel::setDriveSpeed
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Joystick & GPS Behavior
            SectionHeader(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.settings_gps_section)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_joystick_side),
                        checked = settings.joystickOnLeft,
                        onCheckedChange = viewModel::setJoystickOnLeft
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_gps_jitter),
                        subtitle = stringResource(R.string.settings_gps_jitter_desc),
                        checked = settings.enableGpsJitter,
                        onCheckedChange = viewModel::setEnableGpsJitter
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_follow_pin),
                        checked = settings.keepMapCentered,
                        onCheckedChange = viewModel::setKeepMapCentered
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_show_trail),
                        checked = settings.showTrail,
                        onCheckedChange = viewModel::setShowTrail
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = stringResource(R.string.settings_update_interval),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(150L, 250L, 500L, 1000L).forEach { interval ->
                            FilterChip(
                                selected = settings.updateIntervalMs == interval,
                                onClick = { viewModel.setUpdateInterval(interval) },
                                label = { Text("${interval}ms") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MapBlue.copy(alpha = 0.16f),
                                    selectedLabelColor = MapBlue
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MapBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MapBlue
        )
    }
}

@Composable
private fun SpeedSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${value.roundToInt()} km/j",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = MapBlue
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MapBlue,
                activeTrackColor = MapBlue
            )
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
