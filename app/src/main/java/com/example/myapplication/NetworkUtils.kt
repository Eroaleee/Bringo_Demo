package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    suspend fun hasInternetAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val urlConnection = (URL("https://www.google.com").openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Test")
                setRequestProperty("Connection", "close")
                connectTimeout = 3000
                connect()
            }
            urlConnection.responseCode == 200
        } catch (e: IOException) {
            false
        }
    }
}