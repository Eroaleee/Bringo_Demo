package com.example.myapplication

import kotlin.math.min

const val HALF_HOUR = 1800

data class RouteData (
    var addressMask: Int,
    var minTime: Int,
    var minRoute: MutableList<Int>
)

fun getTimeToNextLocation(iteration: Int,
                          routeMatrix: MutableList<MutableList<MutableList<Int>>>,
                          currentTime: Int,
                          currentLocation: Int,
                          nextLocation: Int): Int {
    // Get the time to the next location
    var timeToNextLocation = routeMatrix[iteration][currentLocation][nextLocation]

    // If the current time is in between 2 time iterations, calculate
    // the time to the next location using a weighted average
    if (iteration < 3) {
        val weight: Double = 1.0 * (currentTime % HALF_HOUR) / HALF_HOUR

        timeToNextLocation = (timeToNextLocation * (1.0 - weight) + routeMatrix[iteration + 1][currentLocation][nextLocation] * weight).toInt()
    }

    return timeToNextLocation
}

fun getRoute(currentRoute: MutableList<Int>,
             routeMatrix: MutableList<MutableList<MutableList<Int>>>,
             nrLocations: Int,
             currentLocation: Int,
             currentTime: Int,
             routeData: RouteData,
             returnToOrigin: Boolean)
{
    // Get the current time iteration
    val iteration = min(3, currentTime / HALF_HOUR)

    // If all addresses have been visited, check if route is minimum and return
    if (routeData.addressMask == (1 shl nrLocations) - 1) {
        var finalTime = currentTime

        // If we must also return to origin add the route from the last location to the initial location
        if (returnToOrigin) {
            currentRoute.add(0)

            finalTime += getTimeToNextLocation(iteration, routeMatrix, currentTime, currentLocation, 0)
        }

        // Update the route and the time if the current route is faster
        if(finalTime < routeData.minTime) {
            routeData.minTime = finalTime
            routeData.minRoute = currentRoute.toMutableList()
        }

        // Remove the initial location
        if (returnToOrigin)
            currentRoute.removeLast()

        return
    }

    // For each address that has not been visited yet
    for (nextLocation in 0..<nrLocations) {
        // If the address is already added or the nextLocation is the same as the currentLocation
        if (nextLocation == currentLocation || (routeData.addressMask and (1 shl nextLocation) != 0))
            continue

        val nextTime = currentTime + getTimeToNextLocation(iteration, routeMatrix, currentTime, currentLocation, nextLocation)

        // Don't check the route if it's longer than the minimum time
        if (nextTime < routeData.minTime) {
            // Temporarily add the next location to the current route
            currentRoute.add(nextLocation)
            routeData.addressMask = routeData.addressMask or (1 shl nextLocation)

            getRoute(currentRoute, routeMatrix, nrLocations, nextLocation, nextTime, routeData, returnToOrigin)

            // Remove the last location in the current route
            routeData.addressMask = routeData.addressMask xor (1 shl nextLocation)
            currentRoute.removeLast()
        }
    }
}