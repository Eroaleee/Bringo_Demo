package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.net.InetAddress

interface GoogleRouteAPI {
    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: originIndex,destinationIndex,duration,distanceMeters,status,condition"
    )
    @POST("distanceMatrix/v2:computeRouteMatrix")
    fun computeRouteMatrix(@Body payload: String, @Header("X-Goog-Api-Key") apiKey: String): Call<String>
}

fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo?.isConnected == true
}

fun hasInternetAccess(): Boolean {
    return try {
        val ipAddr = InetAddress.getByName("google.com")
        !ipAddr.equals("")
    } catch (e: Exception) {
        false
    }
}

fun callMapsAPI(payload: String, onSuccess: (String) -> Unit, onFailure: (Throwable) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    Log.d("Retrofit Builder", "Generated retrofit builder")
    val api = retrofit.create(GoogleRouteAPI::class.java)
    Log.d("api call", "generated api var")

    val apiKey = "AIzaSyDfvdvd2KFTdyK-zsTb93Etwl1oc2_Gd1o"  // TODO: gaseste o cale mai buna
    val call = api.computeRouteMatrix(payload, apiKey)
    Log.d("api call", "generated call")
    call.enqueue(object : Callback<String> {
        override fun onResponse(call: Call<String>, response: Response<String>) {
            Log.d("onResponse", "entered onResponse function")
            if(response.isSuccessful) {
                Log.d("onResponse", "successful response")
                response.body()?.let {
                    onSuccess(it)   // Call the onSuccess lambda with the response
                }
            } else {
                Log.d("onResponse", "failed response")
                onFailure(Exception("API Response Not Successful"))
            }
        }

        override fun onFailure(call: Call<String>, t: Throwable) {
            Log.d("onFailure", "no response")
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

fun fastestRoute(context: Context, addresses: MutableList<String>, returnToOrigin: Boolean) {
    println(isNetworkConnected(context))
    println(hasInternetAccess())

    val routeMatrix: MutableList<MutableList<MutableList<Int>>> = mutableListOf()

    // Get route times for every half hour in the next 2-hour interval
    for (timeAdded in 0..3) {
        println(getJSONPayload(addresses, timeAdded))

        var responseJSON = ""
        callMapsAPI(
            getJSONPayload(addresses, timeAdded),
            onSuccess = {
                responseJSON = it
            },
            onFailure = { error ->
                error.printStackTrace()
            }
        )

        Log.d("API call", "finished api call")
        println(responseJSON)

        routeMatrix.add(readJSONResponse(responseJSON, addresses.size))
    }

    val currentRoute: MutableList<Int> = mutableListOf(0)
    val routeData = RouteData(1, Int.MAX_VALUE, mutableListOf())

    getRoute(currentRoute, routeMatrix, addresses.size, 0, 0, routeData, returnToOrigin)
    val webLink = generateWebLink(routeData.minRoute, addresses)

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webLink))
    startActivity(context, intent, null)
    // TODO: baga in google maps app daca are
}