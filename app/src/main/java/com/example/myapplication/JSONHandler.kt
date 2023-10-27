package com.example.myapplication

import android.location.Location
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class Location(
    val latLng: LatLng
)

@Serializable
data class Waypoint(
    val location: com.example.myapplication.Location? = null,
    @SerialName("address")
    val address: String? = null,
)

@Serializable
data class Origins(
    val waypoint: Waypoint
)

@Serializable
data class Destinations(
    val waypoint: Waypoint
)

@Serializable
data class RequestDataRouteMatrix(
    val origins: List<Origins>,
    val destinations: List<Destinations>,
    @SerialName("travelMode")
    val travelMode: String,
    @SerialName("routingPreference")
    val routingPreference: String,
    @SerialName("departureTime")
    val departureTime: String
)

@Serializable
data class StatusMessage(
    val message: String? = null
)

@Serializable
data class ResponseDataRouteMatrix(
    @SerialName("originIndex")
    val originIndex: Int,
    @SerialName("destinationIndex")
    val destinationIndex: Int,
    @SerialName("status")
    val status: StatusMessage,
    @SerialName("distanceMeters")
    val distanceMeters: Int? = null,
    @SerialName("duration")
    val duration: String,
    @SerialName("condition")
    val condition: String,
)

fun getCurrentTimeAsISO8601(timeAdded: Int): String {
    // Gets the current time and adds the specified (timeAdded) amount * 30 minutes
    val time = LocalDateTime.now().plusMinutes(timeAdded * 30L)
    val formatter = DateTimeFormatter.ISO_DATE_TIME

    return time.format(formatter) + 'Z'
}

fun getRouteMatrixJSONPayload(currentLocation: Location?, addresses: List<String>, timeAdded: Int): RequestDataRouteMatrix {
    val originList = mutableListOf<Origins>()
    val destinationList = mutableListOf<Destinations>()

    if(currentLocation != null) {
        originList.add(Origins(Waypoint(location = Location(LatLng(currentLocation.latitude, currentLocation.longitude)))))
        destinationList.add(Destinations(Waypoint(location = Location(LatLng(currentLocation.latitude, currentLocation.longitude)))))
    }

    addresses.forEach {
        originList.add(Origins(Waypoint(address = it)))
        destinationList.add(Destinations(Waypoint(address = it)))
    }

    return RequestDataRouteMatrix(
        origins = originList,
        destinations = destinationList,
        travelMode = "DRIVE",
        routingPreference = "TRAFFIC_AWARE",
        departureTime = getCurrentTimeAsISO8601(timeAdded)
    )
}

fun readRouteMatrixJSONResponse(responseDataList: List<ResponseDataRouteMatrix>, nrLocations: Int): MutableList<MutableList<Int>> {
    val routeMatrix = MutableList(nrLocations) { MutableList(nrLocations) {0} }

    for(responseData in responseDataList) {
        if(responseData.originIndex == responseData.destinationIndex)
            continue // throw exception or smthn


        routeMatrix[responseData.originIndex][responseData.destinationIndex] = responseData.duration.removeSuffix("s").toInt()
    }

    return routeMatrix
}

@Serializable
sealed class WaypointRouteOptimization {
    @Serializable
    data class Address(
        @SerialName("address")
        val address: String
    ) : WaypointRouteOptimization()

    @Serializable
    data class Location(
        @SerialName("location")
        val location: com.example.myapplication.Location
    ) : WaypointRouteOptimization()
}

data class RequestDataRouteOptimization(
    @SerialName("origin")
    val origin: WaypointRouteOptimization,
    @SerialName("destination")
    val destination: WaypointRouteOptimization,
    @SerialName("intermediates")
    val intermediates: List<WaypointRouteOptimization>,
    @SerialName("travelMode")
    val travelMode: String,
    @SerialName("optimizeWaypointOrder")
    val optimizeWaypointOrder: String = "true"
)

data class Routes(
    @SerialName("optimizedIntermediateWaypointIndex")
    val optimizedIntermediateWaypointIndex: MutableList<Int>
)

data class ResponseDataRouteOptimization(
    @SerialName("routes")
    val routes: MutableList<Routes>
)

fun getRouteOptimizationJSONPayload(currentLocation: Location?, addresses: List<String>): RequestDataRouteOptimization {
    val originVal: WaypointRouteOptimization
    val destinationVal: WaypointRouteOptimization

    // If we have access to current location add precise coords
    if(currentLocation != null) {
        originVal = WaypointRouteOptimization.Location(Location(LatLng(currentLocation.latitude, currentLocation.longitude)))
        destinationVal = WaypointRouteOptimization.Location(Location(LatLng(currentLocation.latitude, currentLocation.longitude)))
    } else {
        // Otherwise add first address in the given list
        originVal = WaypointRouteOptimization.Address(addresses[0])
        destinationVal = WaypointRouteOptimization.Address(addresses[0])
    }

    val intermediatesList = mutableListOf<WaypointRouteOptimization>()

    addresses.forEach {
        // If currentLocation isn't set, skip the first address (that is the origin/destination)
        if (currentLocation != null || it != addresses[0])
            intermediatesList.add(WaypointRouteOptimization.Address(it))
    }

    return RequestDataRouteOptimization(
        origin = originVal,
        destination = destinationVal,
        intermediates = intermediatesList,
        travelMode = "DRIVE",
    )
}

fun readRouteOptimizationJSONResponse(responseDataRouteOptimization: ResponseDataRouteOptimization): MutableList<Int> {
    println("response from JSON: $responseDataRouteOptimization")

    // Add origin
    val route = mutableListOf<Int>(0)
    responseDataRouteOptimization.routes[0].optimizedIntermediateWaypointIndex.forEach {
        // intermediate stops are indexed starting from 0, but 0 is the origin/destination, so we increment each index
        route.add(it + 1)
    }
    // Add destination
    route.add(0)
    return route
}