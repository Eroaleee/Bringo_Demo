package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GoogleRouteAPI {
    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: originIndex,destinationIndex,duration,distanceMeters,status,condition"
    )
    @POST("distanceMatrix/v2:computeRouteMatrix")
    fun computeRouteMatrix(@Body payload: RequestData, @Header("X-Goog-Api-Key") apiKey: String): Call<List<ResponseData>>
}

fun callMapsAPI(payload: RequestData, apiKey: String, onSuccess: (List<ResponseData>) -> Unit, onFailure: (Throwable) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(GoogleRouteAPI::class.java)

    val call = api.computeRouteMatrix(payload, apiKey)
    call.enqueue(object : Callback<List<ResponseData>> {
        override fun onResponse(
            call: Call<List<ResponseData>>,
            response: Response<List<ResponseData>>
        ) {
            if(response.isSuccessful) {
                response.body()?.let {
                    onSuccess(it)   // Call the onSuccess lambda with the response
                }
            } else {
                onFailure(Exception("API Response Not Successful"))
            }
        }

        override fun onFailure(call: Call<List<ResponseData>>, t: Throwable) {
            t.printStackTrace()
            onFailure(t)    // Call the onFailure lambda with the error
        }
    })

    // TODO: s-ar putea pe android sa gasim alta susta pt API key
}

fun generateWebLink(minRoute: MutableList<Int>, addresses: List<String>): String {
    var webLink = "https://www.google.com/maps/dir/"

    // TODO: Adauga coordonatele la inceput (ex: .../dir/44.4865,26.1738/Jolie+Ville+Galleria/...

    minRoute.forEach {
        // Web link cannot have spaces, so it uses '+' as a separator
        val addressForLink = addresses[it].replace(' ', '+')
        webLink += "$addressForLink/"
    }

    return webLink
}

@OptIn(DelicateCoroutinesApi::class)
fun fastestRoute(context: Context, addresses: MutableList<String>, returnToOrigin: Boolean) {
    val applicationInfo: ApplicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    val apiKey: String = applicationInfo.metaData["MAPS_API_KEY"].toString()

    val routeMatrix: MutableList<MutableList<MutableList<Int>>> = mutableListOf()

    // Get route times for every half hour in the next 2-hour interval
    for (timeAdded in 0..3) {
        println(getJSONPayload(addresses, timeAdded))

        var responseJSON: List<ResponseData>
        callMapsAPI(
            getJSONPayload(addresses, timeAdded),
            apiKey,
            onSuccess = {
                responseJSON = it
                routeMatrix.add(readJSONResponse(responseJSON, addresses.size))
            },
            onFailure = { error ->
                error.printStackTrace()
            }
        )
    }

    GlobalScope.launch(Dispatchers.Main) {
        delay(4000)
        val currentRoute: MutableList<Int> = mutableListOf(0)
        val routeData = RouteData(1, Int.MAX_VALUE, mutableListOf())

        getRoute(currentRoute, routeMatrix, addresses.size, 0, 0, routeData, returnToOrigin)
        val webLink = generateWebLink(routeData.minRoute, addresses)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webLink))
        startActivity(context, intent, null)
    }
}