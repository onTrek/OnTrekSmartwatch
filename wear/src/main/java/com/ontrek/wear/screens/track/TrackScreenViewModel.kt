package com.ontrek.wear.screens.track

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ontrek.shared.data.TrackPoint
import com.ontrek.shared.data.toSimplePoint
import com.ontrek.wear.utils.functions.getDistanceTo
import com.ontrek.wear.utils.functions.getNearestPoints
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import kotlin.collections.maxOfOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/* * The threshold to consider a track point as surpassed.
 * If the distance to the user is below this threshold, the point is considered surpassed.
 * This value is used to filter out points that are near from the user.
 */
const val trackPointThreshold = 15

/* * The minimum rotation angle to consider the direction changed.
    * If the angle between the current direction and the new direction is above this threshold,
    * the direction is considered changed, and the arrow direction is updated.
 */
const val degreesThreshold: Double = 5.0

/* * The distance threshold to notify the user when they are going off track.
 * If the user is above this distance from the track, they will be notified that they are going off track.
 * This value is used to alert the user when they are deviating too far from the track.
 */
const val notificationTrackDistanceThreshold: Double = 25.0

class TrackScreenViewModel : ViewModel() {

    private val trackPoints = MutableStateFlow(listOf<TrackPoint>())
    val trackPointListState: StateFlow<List<TrackPoint>> = trackPoints
    private val parsingError = MutableStateFlow<String>("")
    val parsingErrorState: StateFlow<String> = parsingError
    private val _isInitialized = MutableStateFlow<Boolean?>(null)
    val isInitialized: StateFlow<Boolean?> = _isInitialized
    private val arrowDirection = MutableStateFlow<Float>(0F)
    val arrowDirectionState: StateFlow<Float> = arrowDirection

    private val _distanceFromTrack = MutableStateFlow<Double?>(null)
    val distanceFromTrack: StateFlow<Double?> = _distanceFromTrack

    private val _notifyOffTrack = MutableStateFlow(false)
    val notifyOffTrack: StateFlow<Boolean> = _notifyOffTrack

    private val _alreadyNotifiedOffTrack = MutableStateFlow(false)

    private val progress = MutableStateFlow(0F) // Progress along the track
    val progressState: StateFlow<Float> = progress
    private val _isOffTrack = MutableStateFlow(false)
    val isOffTrack: StateFlow<Boolean> = _isOffTrack

    // States only used inside the viewModel functions
    private val nextTrackPoint =
        MutableStateFlow<TrackPoint?>(null) // Track point for direction calculation
    private val probablePointIndex = MutableStateFlow<Int?>(null) // Track point for direction calculation, used to avoid recomputing the same point
    private val position = MutableStateFlow<Location?>(null) // Current position of the user
    private val totalLength = MutableStateFlow(0F)
    private val lastPublishedDirection = MutableStateFlow<Double?>(null)
    private var isAtStartup by mutableStateOf(true)

    fun loadGpx(context: Context, fileName: String) {
        val parser = GPXParser()
        viewModelScope.launch {
            try {
                val gpxFile = context.openFileInput(fileName)
                val parsedGpx: Gpx? = parser.parse(gpxFile)
                var partialDistance = 0F
                parsedGpx?.let {
                    Log.d(
                        "TrackScreenViewModel", "GPX file parsed successfully: ${it.metadata?.name}"
                    )
                    trackPoints.value = it.tracks.flatMap { track ->
                        track.trackSegments.flatMap { segment ->
                            segment.trackPoints.mapIndexed { index, point ->
                                val distance = if (index > 0) {
                                    getDistanceTo(
                                        point.toSimplePoint(),
                                        segment.trackPoints[index - 1].toSimplePoint()
                                    )
                                } else {
                                    0.0
                                }
                                partialDistance = partialDistance + distance.toFloat()
                                TrackPoint(
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    elevation = point.elevation,
                                    distanceToPrevious = distance,
                                    totalDistanceTraveled = partialDistance,
                                    index = index
                                )
                            }
                        }
                    }
                    totalLength.value = partialDistance
                } ?: {
                    Log.e("TrackScreenViewModel", "Generic GPX parsing error")
                    parsingError.value = "Error parsing GPX file: No data found"
                }
            } catch (e: IOException) {
                e.printStackTrace()
                parsingError.value = "Error reading GPX file: ${e.message}"
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
                parsingError.value = "Error parsing GPX file: ${e.message}"
            }
        }
    }

    // Checks the distance to the track and initializes the direction track point
    fun checkTrackDistanceAndInitialize(currentLocation: Location, direction: Float) {
        position.value = currentLocation
        if (trackPoints.value.isNotEmpty()) {
            val nearestPoint = getNearestPoints(
                currentLocation, trackPoints.value
            )[0]
            val thresholdDistance =
                (trackPoints.value.maxOfOrNull { it.distanceToPrevious } ?: Double.MIN_VALUE)
            val isNearTrackValue =
                nearestPoint.distanceToUser < 100 || nearestPoint.distanceToUser < thresholdDistance
            Log.d("TRACK_SCREEN_VIEW_MODEL", "Nearest point: ${nearestPoint.index}, distance to user: ${nearestPoint.distanceToUser}")
            if (isNearTrackValue) {
                val finderResult = findNextTrackPoint(
                    currentLocation, trackPoints.value, null
                )
                nextTrackPoint.value = finderResult.nextTrackPoint
                probablePointIndex.value = finderResult.nextProbablePoint
                Log.d(
                    "TRACK_SCREEN_VIEW_MODEL",
                    "Starting from ${nextTrackPoint.value?.index ?: "unknown"}"
                )
                progress.value = (nextTrackPoint.value!!.totalDistanceTraveled / totalLength.value)
                //Accuracy may be low, since this code may be running while the user is in the "improve accuracy screen"
                //but this is a first approximation, more accurate results will be obtained when accuracy improves
                computeIfOnTrack(currentLocation)
                elaborateDirection(direction)
            }
            //Change UI screen state
            _isInitialized.value = isNearTrackValue
        }
    }

    // Elaborates the distance to the track based on the current location and the current track points
    fun elaboratePosition(currentLocation: Location) {
        position.value = currentLocation

        val oldIndex = nextTrackPoint.value?.index

        val finderResult = findNextTrackPoint(currentLocation, trackPoints.value, probablePointIndex.value)
        nextTrackPoint.value = finderResult.nextTrackPoint
        probablePointIndex.value = finderResult.nextProbablePoint


        val newIndex = nextTrackPoint.value!!.index

        if (oldIndex != newIndex) {
            Log.d("TRACK_SCREEN_VIEW_MODEL", "Next track point index: $newIndex")
            if (isAtStartup) isAtStartup = false
            progress.value = (nextTrackPoint.value!!.totalDistanceTraveled / totalLength.value)
        }

        //ONLY FOR DEBUG PURPOSES, REMOVE IN PRODUCTION
        elaborateDirection(0.0F)
        computeIfOnTrack(currentLocation)
    }

    fun computeIfOnTrack(currentLocation: Location) {
        _distanceFromTrack.value = computeDistanceFromTrack(currentLocation, trackPoints.value, probablePointIndex.value!!)

        val distance = if (isAtStartup) getDistanceTo(currentLocation.toSimplePoint(),
            nextTrackPoint.value!!.toSimplePoint()) else _distanceFromTrack.value!!

        val notificationThreshold = if (isAtStartup) {
            val initialThreshold = max((trackPoints.value.maxOfOrNull { it.distanceToPrevious } ?: Double.MIN_VALUE), 100.0) + notificationTrackDistanceThreshold
            Log.d("ON_TRACK_COMPUTATION", "At startup, using initial threshold: $initialThreshold, distance: $distance, notification threshold: $notificationTrackDistanceThreshold")
            initialThreshold
        } else {
            //Log.d("ON_TRACK_COMPUTATION", "distance: $distance")
            notificationTrackDistanceThreshold
        }

        _isOffTrack.value = distance > notificationThreshold

        if (_isOffTrack.value && !_alreadyNotifiedOffTrack.value) {
            _notifyOffTrack.value = true
            _alreadyNotifiedOffTrack.value = true
            Log.d("ON_TRACK_COMPUTATION", "User is off track, notifying, distance from track: $distance")
        } else if (!_isOffTrack.value) {
            _notifyOffTrack.value = false
            _alreadyNotifiedOffTrack.value = false
        }
    }

    fun dismissOffTrackNotification() {
        _notifyOffTrack.value = false
    }

    fun elaborateDirection(compassDirection: Float) {

        val threadSafePosition = position.value
        val threadSafeNextPoint = nextTrackPoint.value
        if (threadSafePosition == null || threadSafeNextPoint == null) {
            Log.w("TRACK_SCREEN_VIEW_MODEL", "Position is null, cannot calculate direction")
            return
        }

        val lat1Rad = Math.toRadians(threadSafePosition.latitude)
        val lat2Rad = Math.toRadians(threadSafeNextPoint.latitude)
        val deltaLonRad =
            Math.toRadians(threadSafeNextPoint.longitude - threadSafePosition.longitude)

        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)

        val initialBearing = atan2(y, x)
        val targetBearing = (Math.toDegrees(initialBearing) + 360) % 360

        val angle = (compassDirection - targetBearing + 360) % 360
        if (shouldUpdateDirection(angle, lastPublishedDirection.value)) {
            lastPublishedDirection.value = angle
            arrowDirection.value = angle.toFloat()
        }
    }

    fun reset() {
        Log.d("TRACK_SCREEN_VIEW_MODEL", "Resetting track data")
        trackPoints.value = emptyList()
        totalLength.value = 0F
        parsingError.value = ""
        _isInitialized.value = null
        nextTrackPoint.value = null
        arrowDirection.value = 0F
        position.value = null
    }
}
