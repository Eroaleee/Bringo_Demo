package com.example.myapplication

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

const val REQUEST_CODE_LOCATION_PERMISSION = 1001

interface GoogleRouteMatrixAPI {
    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: originIndex,destinationIndex,duration,distanceMeters,status,condition"
    )
    @POST("distanceMatrix/v2:computeRouteMatrix")
    suspend fun computeRouteMatrix(@Body payload: RequestDataRouteMatrix, @Header("X-Goog-Api-Key") apiKey: String): Response<List<ResponseDataRouteMatrix>>
}

interface GoogleRouteOptimizationAPI {
    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: routes.optimizedIntermediateWaypointIndex"
    )
    @POST("directions/v2:computeRoutes")
    suspend fun computeOptimizedRoute(@Body payload: RequestDataRouteOptimization, @Header("X-Goog-Api-Key") apiKey: String): Response<ResponseDataRouteOptimization>
}

object RetrofitService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val googleRouteMatrixAPI: GoogleRouteMatrixAPI by lazy {
        retrofit.create(GoogleRouteMatrixAPI::class.java)
    }

    val googleRouteOptimizationAPI: GoogleRouteOptimizationAPI by lazy {
        retrofit.create(GoogleRouteOptimizationAPI::class.java)
    }
}

class RouteService(private val context: Context) {

    private val apiKey: String by lazy {
        val applicationInfo: ApplicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        applicationInfo.metaData["MAPS_API_KEY"].toString()
    }

    suspend fun getFastestRoute(currentLocation: Location?, addresses: MutableList<String>, returnToOrigin: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val routeData = RouteData(1, Int.MAX_VALUE, mutableListOf())

                if (addresses.size == 1 || (addresses.size == 2 && currentLocation == null)) {
                    routeData.minRoute = mutableListOf(0, 1)
                    if(returnToOrigin)
                        routeData.minRoute.add(0)
                }
                else {
                    if (!returnToOrigin)
                        getRouteNoReturn(currentLocation, addresses, routeData, apiKey)
                    else
                        getRouteWithReturn(currentLocation, addresses, routeData, apiKey)
                }


                println("Minimum route is: ${routeData.minRoute}")
                val webLink = generateWebLink(routeData.minRoute, currentLocation, addresses)

                Result.success(webLink)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun generateWebLink(minRoute: MutableList<Int>, currentLocation: Location?, addresses: List<String>): String {
        var webLink = "https://www.google.com/maps/dir/"

        if(currentLocation != null) {
            minRoute.forEach {
                if(it == 0)
                    webLink += "${currentLocation.latitude},${currentLocation.longitude}/"
                else {
                    // Web link cannot have spaces, so it uses '+' as a separator
                    val addressForLink = addresses[it - 1].replace(' ', '+')
                    webLink += "$addressForLink/"
                }
            }
        } else {
            minRoute.forEach {
                // Web link cannot have spaces, so it uses '+' as a separator
                val addressForLink = addresses[it].replace(' ', '+')
                webLink += "$addressForLink/"
            }
        }

        return webLink
    }
}

suspend fun callMapsRouteMatrixAPI(payload: RequestDataRouteMatrix,
                                   apiKey: String
): Result<List<ResponseDataRouteMatrix>> = withContext(Dispatchers.IO) {
    try {
        val response = RetrofitService.googleRouteMatrixAPI.computeRouteMatrix(payload, apiKey)

        if (response.isSuccessful) {
            response.body()?.let {
                Result.success(it)
            } ?: Result.failure(Exception("No Response Body"))
        } else {
            Result.failure(Exception("API Response Not Successful"))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }

    // TODO: s-ar putea pe android sa gasim alta susta pt API key
}

suspend fun callMapsRouteOptimizerAPI(payload: RequestDataRouteOptimization,
                                      apiKey: String
): Result<ResponseDataRouteOptimization> = withContext(Dispatchers.IO) {
    try {
        val response = RetrofitService.googleRouteOptimizationAPI.computeOptimizedRoute(payload, apiKey)

        if (response.isSuccessful) {
            response.body()?.let {
                Result.success(it)
            } ?: Result.failure(Exception("No Response Body"))
        } else {
            // Log the error response
            val statusCode = response.code()
            val errorBody = response.errorBody()?.string()
            Log.e("RouteOptimizerAPI", "API Response Not Successful: StatusCode: $statusCode, ErrorBody: $errorBody")
            Result.failure(Exception("API Response Not Successful: StatusCode: $statusCode, ErrorBody: $errorBody"))
        }

    } catch (e: Exception) {
        Log.e("RouteOptimizerAPI", "API Request Failed", e)
        Result.failure(e)
    }
}

suspend fun getRouteNoReturn(currentLocation: Location?,
                             addresses: MutableList<String>,
                             routeData: RouteData,
                             apiKey: String
) = withContext(Dispatchers.IO) {
    val routeMatrix: MutableList<MutableList<MutableList<Int>>> = mutableListOf()
    val currentLocationExists: Int = if (currentLocation != null) 1 else 0

    // Get route times for every half hour in the next 2-hour interval
    for (timeAdded in 0..3) {
        try {
            val payload = getRouteMatrixJSONPayload(currentLocation, addresses, timeAdded)
            val responseJSON = callMapsRouteMatrixAPI(payload, apiKey).getOrNull()

            if(responseJSON != null)
                routeMatrix.add(readRouteMatrixJSONResponse(responseJSON, addresses.size + currentLocationExists))
            else
                println("Error: responseJSON is null")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    launch(Dispatchers.Default) {
        val currentRoute: MutableList<Int> = mutableListOf(0)
        getRoute(currentRoute, routeMatrix, addresses.size + currentLocationExists, 0, 0, routeData)
    }
}

suspend fun getRouteWithReturn(currentLocation: Location?,
                               addresses: MutableList<String>,
                               routeData: RouteData,
                               apiKey: String
) = withContext(Dispatchers.IO) {
    try {
        val payload = getRouteOptimizationJSONPayload(currentLocation, addresses)

        println("API Request Payload: $payload")

        val responseJSON = callMapsRouteOptimizerAPI(payload, apiKey).getOrNull()

        println("Response JSON after calling api: $responseJSON")

        if(responseJSON != null) {
            routeData.minRoute = readRouteOptimizationJSONResponse(responseJSON)
        } else {
            val errorMessage = "Error: responseJSON is null"
            println(errorMessage)
            throw Exception(errorMessage)
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}