package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.GeoJoySettings
import com.example.data.MapLayerStyle
import com.example.data.SavedLocationEntity
import com.example.location.GeoMath
import com.example.location.GeoPointData
import com.example.location.MockLocationState
import com.example.location.MockRunStatus
import com.example.ui.MainViewModel
import com.example.ui.SearchUiState
import com.example.ui.components.JoystickOverlay
import com.example.ui.components.MapSearchHeader
import com.example.ui.components.OsmMapView
import com.example.ui.components.SaveFavoriteNameDialog
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MapBlue
import com.example.ui.theme.StopCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.theme.WarningAmberText
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    mockState: MockLocationState,
    settings: GeoJoySettings,
    placeTitle: String,
    placeAddress: String,
    isFavorite: Boolean,
    favorites: List<SavedLocationEntity>,
    history: List<SavedLocationEntity>,
    searchState: SearchUiState,
    tappedPreview: GeoPointData?,
    recenterCounter: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showJoystick by remember { mutableStateOf(true) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var showQuickHistorySheet by remember { mutableStateOf(false) }
    var showSaveFavoriteDialog by remember { mutableStateOf(false) }
    var showDevDialog by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var zoomTrigger by remember { mutableStateOf(0 to 0.0) }
    var zoomCounter by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.toggleSpoofing()
    }

    fun handleStartOrStop() {
        if (mockState.status != MockRunStatus.IDLE) {
            viewModel.toggleSpoofing()
            return
        }
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (fineGranted && notifGranted) {
            viewModel.toggleSpoofing()
        } else {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    BackHandler(enabled = searchState.isSearchActive || tappedPreview != null) {
        when {
            searchState.isSearchActive -> viewModel.closeSearchAndClear()
            tappedPreview != null -> viewModel.dismissMapTapPreview()
        }
    }

    val isMoving = hypot(mockState.joystickDx.toDouble(), mockState.joystickDy.toDouble()) > 0.04

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full-bleed Google Maps-like Interactive Map
        OsmMapView(
            latitude = mockState.latitude,
            longitude = mockState.longitude,
            bearing = mockState.bearing,
            isMoving = isMoving,
            status = mockState.status,
            trail = mockState.trail,
            showTrail = settings.showTrail,
            keepCentered = settings.keepMapCentered,
            layerStyle = settings.mapLayerStyle,
            tappedTarget = tappedPreview,
            recenterTrigger = recenterCounter,
            zoomDeltaTrigger = zoomTrigger,
            onSingleTap = { lat, lng -> viewModel.onMapSingleTap(lat, lng) },
            onLongPress = { lat, lng ->
                viewModel.teleportTo(lat, lng)
                Toast.makeText(context, "Dipindahkan ke titik peta", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Search Header & Quick-Jump Presets + Optional Dev Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp)
        ) {
            MapSearchHeader(
                searchState = searchState,
                presets = viewModel.quickPresets,
                onQueryChange = viewModel::updateSearchQuery,
                onSearchFocus = viewModel::setSearchActive,
                onSubmitSearch = viewModel::submitSearch,
                onClearOrCloseSearch = viewModel::closeSearchAndClear,
                onSelectResult = viewModel::selectSearchResult,
                onSelectPreset = { preset ->
                    viewModel.teleportTo(
                        lat = preset.latitude,
                        lng = preset.longitude,
                        title = "${preset.title}, ${preset.subtitle}",
                        address = preset.subtitle
                    )
                }
            )

            AnimatedVisibility(
                visible = !searchState.isSearchActive &&
                    !mockState.isDeveloperMockAuthorized &&
                    !bannerDismissed
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WarningAmberContainer,
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showDevDialog = true }
                        .testTag("dev_setup_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = WarningAmberText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dev_banner_title),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                                color = WarningAmberText
                            )
                            Text(
                                text = stringResource(R.string.dev_banner_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = WarningAmberText.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { bannerDismissed = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Tutup", color = WarningAmberText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Right-Edge Google Maps Floating Controls (Layers, Joystick Toggle, Recenter, Zoom)
        AnimatedVisibility(
            visible = !searchState.isSearchActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 126.dp, end = 14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                MapCircleControlButton(
                    icon = Icons.Filled.Layers,
                    contentDescription = stringResource(R.string.action_map_layers),
                    tint = if (settings.mapLayerStyle != MapLayerStyle.DEFAULT) MapBlue else MaterialTheme.colorScheme.onSurface,
                    testTag = "map_layers_button",
                    onClick = { showLayersSheet = true }
                )

                MapCircleControlButton(
                    icon = Icons.Filled.Gamepad,
                    contentDescription = stringResource(R.string.action_toggle_joystick),
                    tint = if (showJoystick) MapBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "toggle_joystick_button",
                    onClick = { showJoystick = !showJoystick }
                )

                MapCircleControlButton(
                    icon = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.action_recenter),
                    tint = MapBlue,
                    testTag = "recenter_button",
                    onClick = { viewModel.recenterCamera() }
                )

                // Compact Zoom +/- pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                zoomCounter += 1
                                zoomTrigger = zoomCounter to 1.0
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Perbesar peta",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.width(28.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        IconButton(
                            onClick = {
                                zoomCounter += 1
                                zoomTrigger = zoomCounter to -1.0
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Perkecil peta",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Dock: Joystick Overlay + Tapped Pin Action + Google Maps Location Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tapped-on-map teleport prompt pill
            AnimatedVisibility(
                visible = tappedPreview != null && !searchState.isSearchActive,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                tappedPreview?.let { target ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Titik Dipilih di Peta",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = GeoMath.formatCoords(target.latitude, target.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.dismissMapTapPreview() }) {
                                Text("Batal")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.confirmTeleportToTapped() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MapBlue),
                                modifier = Modifier.testTag("confirm_teleport_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pindah")
                            }
                        }
                    }
                }
            }

            // Analog Joystick Pod (Left or Right aligned cleanly above Bottom Card)
            AnimatedVisibility(
                visible = showJoystick && !searchState.isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    JoystickOverlay(
                        speedMode = settings.selectedSpeedMode,
                        activeSpeedKmh = settings.activeSpeedKmh,
                        liveSpeedKmh = mockState.currentSpeedKmh,
                        bearing = mockState.bearing,
                        isCruiseLocked = mockState.isCruiseLocked,
                        joystickDx = mockState.joystickDx,
                        joystickDy = mockState.joystickDy,
                        isJoystickOnLeft = settings.joystickOnLeft,
                        isSpoofingActive = mockState.status != MockRunStatus.IDLE,
                        onVectorChanged = viewModel::updateJoystick,
                        onSelectSpeedMode = viewModel::setSpeedMode,
                        onToggleCruiseLock = viewModel::toggleCruiseLock,
                        onSwapSide = { viewModel.setJoystickOnLeft(!settings.joystickOnLeft) },
                        modifier = Modifier.align(
                            if (settings.joystickOnLeft) Alignment.BottomStart else Alignment.BottomEnd
                        )
                    )
                }
            }

            // Google Maps-style Location Control Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Live Status Chip + Active Map Layer Pill
                            val (statusText, statusColor) = when (mockState.status) {
                                MockRunStatus.ACTIVE_SYSTEM ->
                                    stringResource(R.string.status_active_system) to ActiveEmerald
                                MockRunStatus.ACTIVE_PREVIEW ->
                                    stringResource(R.string.status_active_preview) to WarningAmber
                                MockRunStatus.IDLE ->
                                    stringResource(R.string.status_ready) to MapBlue
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = statusColor
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = placeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = GeoMath.formatCoords(mockState.latitude, mockState.longitude),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick History & Favorites sheet button
                        IconButton(
                            onClick = { showQuickHistorySheet = true },
                            modifier = Modifier.testTag("quick_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = stringResource(R.string.action_quick_history),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Copy coordinates button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = GeoMath.formatCoords(mockState.latitude, mockState.longitude)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Koordinat GeoJoy", text))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.coords_copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("copy_coords_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy_coords),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Favorite toggle / naming button
                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    viewModel.removeCurrentFavorite()
                                    Toast.makeText(context, "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSaveFavoriteDialog = true
                                }
                            },
                            modifier = Modifier.testTag("favorite_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.action_remove_favorite else R.string.action_save_favorite
                                ),
                                tint = if (isFavorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val isRunning = mockState.status != MockRunStatus.IDLE
                    Button(
                        onClick = { handleStartOrStop() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) StopCrimson else MapBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_stop_mock_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isRunning) R.string.action_stop_mock else R.string.action_start_mock
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    // Google Maps-style Map Layers & Details Bottom Sheet (Default, Satelit, Medan, Malam)
    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = stringResource(R.string.layer_sheet_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapLayerOption(
                        label = stringResource(R.string.layer_default),
                        icon = Icons.Filled.Map,
                        selected = settings.mapLayerStyle == MapLayerStyle.DEFAULT,
                        testTag = "layer_option_default",
                        onClick = { viewModel.setMapLayerStyle(MapLayerStyle.DEFAULT) },
                        modifier = Modifier.weight(1f)
                    )
                    MapLayerOption(
                        label = stringResource(R.string.layer_satellite),
                        icon = Icons.Filled.SatelliteAlt,
                        selected = settings.mapLayerStyle == MapLayerStyle.SATELLITE,
                        testTag = "layer_option_satellite",
                        onClick = { viewModel.setMapLayerStyle(MapLayerStyle.SATELLITE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapLayerOption(
                        label = stringResource(R.string.layer_terrain),
                        icon = Icons.Filled.Terrain,
                        selected = settings.mapLayerStyle == MapLayerStyle.TERRAIN,
                        testTag = "layer_option_terrain",
                        onClick = { viewModel.setMapLayerStyle(MapLayerStyle.TERRAIN) },
                        modifier = Modifier.weight(1f)
                    )
                    MapLayerOption(
                        label = stringResource(R.string.layer_dark),
                        icon = Icons.Filled.DarkMode,
                        selected = settings.mapLayerStyle == MapLayerStyle.DARK,
                        testTag = "layer_option_dark",
                        onClick = { viewModel.setMapLayerStyle(MapLayerStyle.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.detail_trail),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.showTrail,
                        onCheckedChange = viewModel::setShowTrail
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.detail_follow_pin),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.keepMapCentered,
                        onCheckedChange = viewModel::setKeepMapCentered
                    )
                }

                if (mockState.trail.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            viewModel.clearTrail()
                            showLayersSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Jejak Rute")
                    }
                }
            }
        }
    }

    // Quick History & Favorites Bottom Sheet right on Map
    if (showQuickHistorySheet) {
        var sheetTab by remember { mutableIntStateOf(if (history.isNotEmpty()) 0 else 1) }
        ModalBottomSheet(
            onDismissRequest = { showQuickHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_history_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = sheetTab == 0,
                        onClick = { sheetTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.saved_tab_history))
                    }
                    SegmentedButton(
                        selected = sheetTab == 1,
                        onClick = { sheetTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.saved_tab_favorites))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val itemsList = if (sheetTab == 0) history else favorites

                if (itemsList.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (sheetTab == 0) R.string.saved_empty_history else R.string.saved_empty_favorites
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(itemsList, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        showQuickHistorySheet = false
                                        viewModel.teleportTo(
                                            lat = item.latitude,
                                            lng = item.longitude,
                                            title = item.name,
                                            address = item.address
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (sheetTab == 1) Icons.Filled.Star else Icons.Filled.History,
                                        contentDescription = null,
                                        tint = if (sheetTab == 1) WarningAmber else MapBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = GeoMath.formatCoords(item.latitude, item.longitude),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Navigation,
                                        contentDescription = stringResource(R.string.action_teleport_here),
                                        tint = MapBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Naming Dialog when saving Favorite
    if (showSaveFavoriteDialog) {
        SaveFavoriteNameDialog(
            titleText = stringResource(R.string.favorite_dialog_title),
            initialName = placeTitle,
            latitude = mockState.latitude,
            longitude = mockState.longitude,
            onDismiss = { showSaveFavoriteDialog = false },
            onConfirm = { chosenName ->
                viewModel.saveFavoriteWithName(
                    name = chosenName,
                    address = placeAddress,
                    lat = mockState.latitude,
                    lng = mockState.longitude
                )
                showSaveFavoriteDialog = false
                Toast.makeText(context, "\"$chosenName\" disimpan ke Favorit", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Developer Options Guide Dialog
    if (showDevDialog) {
        AlertDialog(
            onDismissRequest = { showDevDialog = false },
            title = { Text(stringResource(R.string.dev_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.dev_dialog_steps),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDevDialog = false
                        openDeveloperSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MapBlue)
                ) {
                    Text(stringResource(R.string.action_open_dev_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevDialog = false }) {
                    Text("Nanti")
                }
            }
        )
    }
}

@Composable
private fun MapCircleControlButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(46.dp)
            .minimumInteractiveComponentSize()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MapLayerOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MapBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(2.dp, MapBlue) else null,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MapBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MapBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun openDeveloperSettings(context: Context) {
    val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(devIntent)
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Buka Pengaturan > Opsi Pengembang secara manual",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
