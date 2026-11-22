package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.MapLayerStyle
import com.example.location.GeoPointData
import com.example.location.MockRunStatus
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.cos
import kotlin.math.sin

private object GeoJoyTileSources {
    val cartoDark: OnlineTileSourceBase = XYTileSource(
        "CartoDark",
        0,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/dark_all/",
            "https://b.basemaps.cartocdn.com/rastertiles/dark_all/",
            "https://c.basemaps.cartocdn.com/rastertiles/dark_all/"
        )
    )

    val esriSatellite: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriWorldImagery",
        0,
        18,
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }

    val esriTerrain: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriWorldTopo",
        0,
        18,
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }
}

private class LocationPuckOverlay : Overlay() {
    var currentPoint: GeoPoint = GeoPoint(-6.175392, 106.827153)
    var bearing: Float = 0f
    var status: MockRunStatus = MockRunStatus.IDLE
    var isMoving: Boolean = false
    var tappedTarget: GeoPoint? = null

    private val screenPoint = Point()
    private val targetScreenPoint = Point()

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val haloStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }
    private val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val targetPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEA4335.toInt()
        style = Paint.Style.FILL
    }
    private val targetInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF961B12.toInt()
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        // Draw tapped target pin if present
        tappedTarget?.let { target ->
            mapView.projection.toPixels(target, targetScreenPoint)
            val tx = targetScreenPoint.x.toFloat()
            val ty = targetScreenPoint.y.toFloat()
            canvas.drawCircle(tx, ty, 8f, dropShadowPaint)
            val pinPath = Path().apply {
                moveTo(tx, ty)
                cubicTo(tx - 24f, ty - 28f, tx - 22f, ty - 56f, tx, ty - 56f)
                cubicTo(tx + 22f, ty - 56f, tx + 24f, ty - 28f, tx, ty)
                close()
            }
            canvas.drawPath(pinPath, targetPinPaint)
            canvas.drawCircle(tx, ty - 38f, 8f, targetInnerPaint)
        }

        // Draw Google Maps-style blue location puck
        mapView.projection.toPixels(currentPoint, screenPoint)
        val cx = screenPoint.x.toFloat()
        val cy = screenPoint.y.toFloat()

        val primaryColor = when (status) {
            MockRunStatus.ACTIVE_SYSTEM -> 0xFF0F9D58.toInt()
            MockRunStatus.ACTIVE_PREVIEW -> 0xFF1A73E8.toInt()
            MockRunStatus.IDLE -> 0xFF1A73E8.toInt()
        }
        val haloColor = when (status) {
            MockRunStatus.ACTIVE_SYSTEM -> 0x2E0F9D58
            else -> 0x291A73E8
        }
        val haloStroke = when (status) {
            MockRunStatus.ACTIVE_SYSTEM -> 0x660F9D58
            else -> 0x551A73E8
        }

        val haloRadius = if (status != MockRunStatus.IDLE) 54f else 38f
        haloPaint.color = haloColor
        haloStrokePaint.color = haloStroke
        canvas.drawCircle(cx, cy, haloRadius, haloPaint)
        canvas.drawCircle(cx, cy, haloRadius, haloStrokePaint)

        if (isMoving || status != MockRunStatus.IDLE) {
            val coneRadius = 68f
            val oval = RectF(cx - coneRadius, cy - coneRadius, cx + coneRadius, cy + coneRadius)
            val startAngle = bearing - 90f - 28f
            conePaint.color = if (status == MockRunStatus.ACTIVE_SYSTEM) 0x550F9D58 else 0x551A73E8
            canvas.drawArc(oval, startAngle, 56f, true, conePaint)
        }

        canvas.drawCircle(cx, cy + 2f, 24f, dropShadowPaint)
        canvas.drawCircle(cx, cy, 22f, whiteRingPaint)
        dotPaint.color = primaryColor
        canvas.drawCircle(cx, cy, 15.5f, dotPaint)

        if (isMoving) {
            val rad = Math.toRadians((bearing - 90f).toDouble())
            val px = cx + (cos(rad) * 10f).toFloat()
            val py = cy + (sin(rad) * 10f).toFloat()
            canvas.drawCircle(px, py, 3.5f, whiteRingPaint)
        }
    }
}

@Composable
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    isMoving: Boolean,
    status: MockRunStatus,
    trail: List<GeoPointData>,
    showTrail: Boolean,
    keepCentered: Boolean,
    layerStyle: MapLayerStyle,
    tappedTarget: GeoPointData?,
    recenterTrigger: Int,
    zoomDeltaTrigger: Pair<Int, Double>,
    onSingleTap: (Double, Double) -> Unit,
    onLongPress: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    val puckOverlay = remember { LocationPuckOverlay() }
    val trailPolyline = remember {
        Polyline().apply {
            outlinePaint.color = 0xCC1A73E8.toInt()
            outlinePaint.strokeWidth = 9f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
    }

    val mapView = remember {
        ensureOsmdroidConfig(context.applicationContext)
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setUseDataConnection(true)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 19.5
            isTilesScaledToDpi = true
            overlayManager.tilesOverlay.loadingBackgroundColor = 0xFFE8EAED.toInt()
            overlayManager.tilesOverlay.loadingLineColor = 0xFFDADCE0.toInt()
            setTileSource(TileSourceFactory.MAPNIK)

            val initialPoint = GeoPoint(latitude, longitude)
            controller.setZoom(16.2)
            controller.setCenter(initialPoint)
            addOnFirstLayoutListener { _, _, _, _, _ ->
                controller.setZoom(16.2)
                controller.setCenter(GeoPoint(latitude, longitude))
                invalidate()
            }

            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let { currentOnSingleTap(it.latitude, it.longitude) }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p?.let { currentOnLongPress(it.latitude, it.longitude) }
                    return true
                }
            })

            overlays.add(eventsOverlay)
            overlays.add(trailPolyline)
            overlays.add(puckOverlay)
        }
    }

    var lastHandledRecenter by remember { mutableIntStateOf(0) }
    var lastHandledZoom by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(recenterTrigger, mapView) {
        if (recenterTrigger > lastHandledRecenter) {
            lastHandledRecenter = recenterTrigger
            val targetZoom = mapView.zoomLevelDouble.coerceAtLeast(16.2)
            mapView.controller.animateTo(GeoPoint(latitude, longitude), targetZoom, 420L)
        }
    }

    LaunchedEffect(zoomDeltaTrigger, mapView) {
        if (zoomDeltaTrigger.first > lastHandledZoom) {
            lastHandledZoom = zoomDeltaTrigger.first
            val nextZoom = (mapView.zoomLevelDouble + zoomDeltaTrigger.second).coerceIn(3.0, 19.0)
            mapView.controller.setZoom(nextZoom)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        onRelease = { mv ->
            mv.onPause()
            mv.onDetach()
        },
        update = { mv ->
            val desiredSource = when (layerStyle) {
                MapLayerStyle.DEFAULT -> TileSourceFactory.MAPNIK
                MapLayerStyle.SATELLITE -> GeoJoyTileSources.esriSatellite
                MapLayerStyle.TERRAIN -> GeoJoyTileSources.esriTerrain
                MapLayerStyle.DARK -> GeoJoyTileSources.cartoDark
            }
            if (mv.tileProvider.tileSource.name() != desiredSource.name()) {
                mv.setTileSource(desiredSource)
            }

            if (layerStyle == MapLayerStyle.DARK) {
                mv.overlayManager.tilesOverlay.loadingBackgroundColor = 0xFF1B1F26.toInt()
                mv.overlayManager.tilesOverlay.loadingLineColor = 0xFF282D37.toInt()
            } else {
                mv.overlayManager.tilesOverlay.loadingBackgroundColor = 0xFFE8EAED.toInt()
                mv.overlayManager.tilesOverlay.loadingLineColor = 0xFFDADCE0.toInt()
            }

            val currentGeo = GeoPoint(latitude, longitude)
            puckOverlay.currentPoint = currentGeo
            puckOverlay.bearing = bearing
            puckOverlay.status = status
            puckOverlay.isMoving = isMoving
            puckOverlay.tappedTarget = tappedTarget?.let { GeoPoint(it.latitude, it.longitude) }

            if (showTrail && trail.size >= 2) {
                trailPolyline.isVisible = true
                trailPolyline.outlinePaint.color = if (status == MockRunStatus.ACTIVE_SYSTEM) {
                    0xCC0F9D58.toInt()
                } else {
                    0xCC1A73E8.toInt()
                }
                trailPolyline.setPoints(trail.map { GeoPoint(it.latitude, it.longitude) })
            } else {
                trailPolyline.isVisible = false
            }

            if (keepCentered && (isMoving || status != MockRunStatus.IDLE)) {
                mv.controller.setCenter(currentGeo)
            }

            mv.invalidate()
        }
    )
}

private fun ensureOsmdroidConfig(context: Context) {
    val config = Configuration.getInstance()
    val tileCacheDir = context.cacheDir.resolve("osmdroid_tiles").apply { mkdirs() }
    if (config.osmdroidTileCache != tileCacheDir) {
        val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        val baseDir = context.filesDir.resolve("osmdroid").apply { mkdirs() }
        config.load(context, prefs)
        config.osmdroidBasePath = baseDir
        config.osmdroidTileCache = tileCacheDir
    }
    config.userAgentValue = "GeoJoy-Android/1.0 (${context.packageName})"
}
