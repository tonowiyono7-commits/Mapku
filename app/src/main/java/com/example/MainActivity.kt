package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.MapScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GeoJoyTheme
import com.example.ui.theme.MapBlue

enum class AppDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    MAP(R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
    SAVED(R.string.nav_saved, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "nav_saved"),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoJoyTheme {
                GeoJoyAppRoot()
            }
        }
    }
}

@Composable
fun GeoJoyAppRoot(viewModel: MainViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAuthorization()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mockState by viewModel.mockState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val placeTitle by viewModel.placeTitle.collectAsStateWithLifecycle()
    val placeAddress by viewModel.placeAddress.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val tappedPreview by viewModel.tappedPreview.collectAsStateWithLifecycle()
    val recenterCounter by viewModel.recenterCounter.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.MAP) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppDestination.entries.forEach { destination ->
                            val selected = currentDestination == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = stringResource(destination.labelRes)
                                    )
                                },
                                label = { Text(stringResource(destination.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MapBlue,
                                    selectedTextColor = MapBlue,
                                    indicatorColor = MapBlue.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AppDestination.entries.forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentDestination) {
                    AppDestination.MAP -> {
                        MapScreen(
                            viewModel = viewModel,
                            mockState = mockState,
                            settings = settings,
                            placeTitle = placeTitle,
                            placeAddress = placeAddress,
                            isFavorite = isFavorite,
                            favorites = favorites,
                            history = history,
                            searchState = searchState,
                            tappedPreview = tappedPreview,
                            recenterCounter = recenterCounter
                        )
                    }
                    AppDestination.SAVED -> {
                        SavedScreen(
                            favorites = favorites,
                            history = history,
                            onTeleportTo = { saved ->
                                viewModel.teleportTo(
                                    lat = saved.latitude,
                                    lng = saved.longitude,
                                    title = saved.name,
                                    address = saved.address
                                )
                                currentDestination = AppDestination.MAP
                            },
                            onRenameFavorite = viewModel::renameFavorite,
                            onSaveHistoryAsFavorite = { name, addr, lat, lng ->
                                viewModel.saveFavoriteWithName(
                                    name = name,
                                    address = addr,
                                    lat = lat,
                                    lng = lng
                                )
                            },
                            onDeleteLocation = viewModel::deleteSavedLocation,
                            onClearHistory = viewModel::clearHistory,
                            onNavigateBackToMap = { currentDestination = AppDestination.MAP }
                        )
                    }
                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            isDeveloperMockAuthorized = mockState.isDeveloperMockAuthorized,
                            onNavigateBackToMap = { currentDestination = AppDestination.MAP }
                        )
                    }
                }
            }
        }
        }
    }
}
