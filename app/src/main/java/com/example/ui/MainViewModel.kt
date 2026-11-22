package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoJoyApplication
import com.example.data.GeoJoySettings
import com.example.data.MapLayerStyle
import com.example.data.SavedLocationEntity
import com.example.data.SpeedMode
import com.example.location.GeoMath
import com.example.location.GeoPointData
import com.example.location.MockLocationState
import com.example.location.MockRunStatus
import com.example.network.GeocodingClient
import com.example.network.NominatimPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LandmarkPreset(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

data class SearchUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val results: List<NominatimPlace> = emptyList(),
    val hasSearched: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeoJoyApplication
    private val locationRepo = app.locationRepository
    private val settingsRepo = app.settingsRepository
    private val controller = app.mockLocationController

    val mockState: StateFlow<MockLocationState> = controller.state

    val settings: StateFlow<GeoJoySettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GeoJoySettings()
    )

    val favorites: StateFlow<List<SavedLocationEntity>> = locationRepo.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<SavedLocationEntity>> = locationRepo.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isCurrentFavorite: StateFlow<Boolean> = mockState
        .map { Pair(it.latitude, it.longitude) }
        .distinctUntilChanged { old, new ->
            GeoMath.distanceMeters(old.first, old.second, new.first, new.second) < 15.0
        }
        .flatMapLatest { (lat, lng) -> locationRepo.isFavorite(lat, lng) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _placeTitle = MutableStateFlow("Monas, Jakarta Pusat")
    val placeTitle: StateFlow<String> = _placeTitle.asStateFlow()

    private val _placeAddress = MutableStateFlow("Gambir, Jakarta Pusat, DKI Jakarta")
    val placeAddress: StateFlow<String> = _placeAddress.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _recenterCounter = MutableStateFlow(0)
    val recenterCounter: StateFlow<Int> = _recenterCounter.asStateFlow()

    private val _tappedPreview = MutableStateFlow<GeoPointData?>(null)
    val tappedPreview: StateFlow<GeoPointData?> = _tappedPreview.asStateFlow()

    val quickPresets = listOf(
        LandmarkPreset("Monas", "Jakarta Pusat", -6.175392, 106.827153),
        LandmarkPreset("Simpang Lima", "Semarang", -6.990432, 110.422974),
        LandmarkPreset("Malioboro", "Yogyakarta", -7.792576, 110.365845),
        LandmarkPreset("Gedung Sate", "Bandung", -6.902481, 107.618811),
        LandmarkPreset("Tugu Pahlawan", "Surabaya", -7.245873, 112.737846),
        LandmarkPreset("Shibuya", "Tokyo, Jepang", 35.659482, 139.700559),
        LandmarkPreset("Times Square", "New York, AS", 40.758896, -73.985130)
    )

    private var lastGeocodedPoint: GeoPointData? = GeoPointData(-6.175392, 106.827153)
    private var searchJob: Job? = null
    private var reverseJob: Job? = null

    init {
        viewModelScope.launch {
            mockState
                .map { GeoPointData(it.latitude, it.longitude) }
                .collect { point ->
                    val prev = lastGeocodedPoint
                    val movedEnough = prev == null ||
                        GeoMath.distanceMeters(prev.latitude, prev.longitude, point.latitude, point.longitude) >= 180.0
                    if (movedEnough) {
                        lastGeocodedPoint = point
                        triggerReverseGeocode(point.latitude, point.longitude)
                    }
                }
        }
    }

    fun refreshAuthorization() {
        controller.refreshMockAuthorization()
    }

    fun toggleSpoofing() {
        val current = mockState.value
        if (current.status == MockRunStatus.IDLE) {
            controller.startSpoofing()
            viewModelScope.launch {
                locationRepo.recordHistory(
                    name = _placeTitle.value,
                    address = _placeAddress.value,
                    lat = current.latitude,
                    lng = current.longitude
                )
            }
        } else {
            val endLat = current.latitude
            val endLng = current.longitude
            val endTitle = _placeTitle.value
            val endAddress = _placeAddress.value
            controller.stopSpoofing()
            viewModelScope.launch {
                locationRepo.recordHistory(
                    name = endTitle,
                    address = endAddress,
                    lat = endLat,
                    lng = endLng
                )
            }
        }
    }

    fun updateJoystick(dx: Float, dy: Float) {
        controller.updateJoystickVector(dx, dy)
    }

    fun toggleCruiseLock() {
        controller.toggleCruiseLock()
    }

    fun clearTrail() {
        controller.clearTrail()
    }

    fun teleportTo(
        lat: Double,
        lng: Double,
        title: String? = null,
        address: String? = null,
        clearTrail: Boolean = false
    ) {
        _tappedPreview.value = null
        controller.teleportTo(lat, lng, clearTrail = clearTrail)
        _recenterCounter.value += 1

        if (!title.isNullOrBlank()) {
            _placeTitle.value = title
            _placeAddress.value = address ?: GeoMath.formatCoords(lat, lng)
            lastGeocodedPoint = GeoPointData(lat, lng)
        } else {
            _placeTitle.value = "Titik Peta"
            _placeAddress.value = GeoMath.formatCoords(lat, lng)
            lastGeocodedPoint = GeoPointData(lat, lng)
            triggerReverseGeocode(lat, lng)
        }

        if (mockState.value.status != MockRunStatus.IDLE) {
            viewModelScope.launch {
                locationRepo.recordHistory(
                    name = _placeTitle.value,
                    address = _placeAddress.value,
                    lat = lat,
                    lng = lng
                )
            }
        }
    }

    fun onMapSingleTap(lat: Double, lng: Double) {
        _tappedPreview.value = GeoPointData(lat, lng)
    }

    fun dismissMapTapPreview() {
        _tappedPreview.value = null
    }

    fun confirmTeleportToTapped() {
        val target = _tappedPreview.value ?: return
        teleportTo(target.latitude, target.longitude)
    }

    fun recenterCamera() {
        _recenterCounter.value += 1
    }

    fun saveFavoriteWithName(
        name: String,
        address: String = _placeAddress.value,
        lat: Double = mockState.value.latitude,
        lng: Double = mockState.value.longitude
    ) {
        viewModelScope.launch {
            locationRepo.saveFavorite(
                name = name,
                address = address,
                lat = lat,
                lng = lng
            )
        }
    }

    fun renameFavorite(id: Long, newName: String) {
        viewModelScope.launch {
            locationRepo.renameLocation(id, newName)
        }
    }

    fun removeCurrentFavorite() {
        val current = mockState.value
        viewModelScope.launch {
            locationRepo.removeFavoriteNear(current.latitude, current.longitude)
        }
    }

    fun deleteSavedLocation(id: Long) {
        viewModelScope.launch {
            locationRepo.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            locationRepo.clearHistory()
        }
    }

    fun setSearchActive(active: Boolean) {
        _searchState.value = _searchState.value.copy(isSearchActive = active)
    }

    fun updateSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(
            query = query,
            isSearchActive = true
        )
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = _searchState.value.copy(
                results = emptyList(),
                isLoading = false,
                hasSearched = false
            )
            return
        }

        // Check if query is raw coordinates
        val coords = GeocodingClient.parseCoordinates(query)
        if (coords != null) {
            val coordPlace = NominatimPlace(
                placeId = -1L,
                displayName = "Koordinat Langsung: ${GeoMath.formatCoords(coords.first, coords.second)}",
                lat = coords.first.toString(),
                lon = coords.second.toString(),
                name = "Lompat ke Koordinat"
            )
            _searchState.value = _searchState.value.copy(
                results = listOf(coordPlace),
                isLoading = false,
                hasSearched = true
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(450L)
            executeSearch(query)
        }
    }

    fun submitSearch() {
        val q = _searchState.value.query.trim()
        if (q.isEmpty()) return
        val coords = GeocodingClient.parseCoordinates(q)
        if (coords != null) {
            closeSearchAndClear()
            teleportTo(coords.first, coords.second)
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(q)
        }
    }

    private suspend fun executeSearch(query: String) {
        _searchState.value = _searchState.value.copy(isLoading = true)
        try {
            val results = GeocodingClient.api.search(query)
            _searchState.value = _searchState.value.copy(
                results = results,
                isLoading = false,
                hasSearched = true
            )
        } catch (_: Exception) {
            _searchState.value = _searchState.value.copy(
                results = emptyList(),
                isLoading = false,
                hasSearched = true
            )
        }
    }

    fun selectSearchResult(place: NominatimPlace) {
        closeSearchAndClear()
        teleportTo(
            lat = place.latitude,
            lng = place.longitude,
            title = place.shortTitle,
            address = place.displayName
        )
    }

    fun closeSearchAndClear() {
        searchJob?.cancel()
        _searchState.value = SearchUiState()
    }

    private fun triggerReverseGeocode(lat: Double, lng: Double) {
        reverseJob?.cancel()
        reverseJob = viewModelScope.launch {
            delay(350L)
            try {
                val res = GeocodingClient.api.reverse(lat, lng)
                _placeTitle.value = res.toShortTitle()
                _placeAddress.value = res.displayName?.takeIf { it.isNotBlank() }
                    ?: GeoMath.formatCoords(lat, lng)
            } catch (_: Exception) {
                if (_placeTitle.value.isBlank()) {
                    _placeTitle.value = "Titik Peta"
                }
                _placeAddress.value = GeoMath.formatCoords(lat, lng)
            }
        }
    }

    // Settings updates
    fun setSpeedMode(mode: SpeedMode) {
        viewModelScope.launch { settingsRepo.setSpeedMode(mode) }
    }

    fun setWalkSpeed(kmh: Float) {
        viewModelScope.launch { settingsRepo.setWalkSpeed(kmh) }
    }

    fun setCycleSpeed(kmh: Float) {
        viewModelScope.launch { settingsRepo.setCycleSpeed(kmh) }
    }

    fun setDriveSpeed(kmh: Float) {
        viewModelScope.launch { settingsRepo.setDriveSpeed(kmh) }
    }

    fun setMapLayerStyle(style: MapLayerStyle) {
        viewModelScope.launch { settingsRepo.setMapLayerStyle(style) }
    }

    fun setUpdateInterval(ms: Long) {
        viewModelScope.launch { settingsRepo.setUpdateInterval(ms) }
    }

    fun setEnableGpsJitter(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setEnableGpsJitter(enabled) }
    }

    fun setKeepMapCentered(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setKeepMapCentered(enabled) }
    }

    fun setJoystickOnLeft(onLeft: Boolean) {
        viewModelScope.launch { settingsRepo.setJoystickOnLeft(onLeft) }
    }

    fun setShowTrail(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setShowTrail(enabled) }
    }
}
