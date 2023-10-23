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
data class RequestData(
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
data class ResponseData(
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

fun getJSONPayload(currentLocation: Location?, addresses: List<String>, timeAdded: Int): RequestData {
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

    return RequestData(
        origins = originList,
        destinations = destinationList,
        travelMode = "DRIVE",
        routingPreference = "TRAFFIC_AWARE",
        departureTime = getCurrentTimeAsISO8601(timeAdded)
    )
}

fun readJSONResponse(responseDataList: List<ResponseData>, nrLocations: Int): MutableList<MutableList<Int>> {
    val routeMatrix = MutableList(nrLocations) { MutableList(nrLocations) {0} }

    for(responseData in responseDataList) {
        if(responseData.originIndex == responseData.destinationIndex)
            continue // throw exception or smthn


        routeMatrix[responseData.originIndex][responseData.destinationIndex] = responseData.duration.removeSuffix("s").toInt()
    }

    return routeMatrix
}

data class AddressResult(
    val place_id: String,
    val display_name: String,
    val type: String
)