package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AddressAdapter.AddressClickListener {
    private lateinit var addressAdapter: AddressAdapter
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var currentLocation: Location? = null
    private var comingFromSettings = false
    private val token = AutocompleteSessionToken.newInstance()
    private val routeService by lazy { RouteService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actvAddressInput = findViewById<AutoCompleteTextView>(R.id.actvAddressName)

        addressAdapter = AddressAdapter(this)
        autoCompleteAdapter = ArrayAdapter(this, R.layout.autocomplete_item)
        autoCompleteTextView = actvAddressInput
        autoCompleteTextView.setAdapter(autoCompleteAdapter)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fetchLastLocation()
        initializePlacesAPI()

        // Make the RecyclerView have a Linear layout
        val rvAddressList : RecyclerView = findViewById(R.id.rvAddressList)
        rvAddressList.adapter = addressAdapter
        rvAddressList.layoutManager = LinearLayoutManager(this)

        // Adds functionality to the address adding button
        findViewById<ImageButton>(R.id.btnAddAddress).setOnClickListener {
            // Gets the address name, removes extra spaces and trims it
            val addressName = actvAddressInput.text.toString().replace("\\s+".toRegex(), " ").trim()

            if(addressName.isNotEmpty()) {
                val address = Address(addressName)
                val newList = addressAdapter.currentList.toMutableList().apply {
                    add(address)
                }
                addressAdapter.submitList(newList)
                actvAddressInput.text.clear()
            }
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Get the current query from the EditText
                    var query = s?.toString()

                    if(query.isNullOrBlank())
                        query = ""

                    // Update the Autocomplete Predictions based on the new query
                    updateAutocompletePredictions(query)

                    autoCompleteAdapter.notifyDataSetChanged()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })


        // Adds functionality to the start route button
        findViewById<Button>(R.id.btnStartRoute).setOnClickListener{
            val addresses: MutableList<String> = addressAdapter.getAddresses().distinct().toMutableList()

            if(addresses.isEmpty())
                showErrorPopup("No addresses inputted")
            else if (addresses.size == 1 && currentLocation == null){
                showErrorPopup("No destination/intermediate addresses inputted")
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (NetworkUtils.isNetworkConnected(this@MainActivity)) {
                        if (NetworkUtils.hasInternetAccess()) {
                            // Connected to the internet and have internet access
                            getFastestRoute(currentLocation, addresses, findViewById<CheckBox>(R.id.cbReturnToOrigin).isChecked)
                        } else {
                            // Connected to the network, but no internet access
                            showErrorPopup("No Internet Access")
                        }
                    } else {
                        // Not connected to any network
                        showErrorPopup("No Internet Connection")
                    }
                }
            }
        }
    }

    // Handler function that allows async calls to Google Maps API and obtaining address
    private fun getFastestRoute(currentLocation: Location?, addresses: MutableList<String>, returnToOrigin: Boolean) {
        lifecycleScope.launch {
            val result = routeService.getFastestRoute(currentLocation, addresses, returnToOrigin)

            if (result.isSuccess) {
                val webLink = result.getOrNull()
                if (webLink != null) {
                    // Redirect to Google Maps
                    println(webLink)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webLink))
                    startActivity(intent)
                } else {
                    // Handle the error, the success value was null
                    showErrorPopup("An unexpected error occurred")
                }
            } else {
                val error = result.exceptionOrNull() // This returns the Throwable that was passed to Result.failure or null
                // Handle the error, e.g., show a popup
                showErrorPopup(error?.message ?: "An unknown error occurred")
            }
        }
    }

    private fun showErrorPopup(message: String) {
        // Show an error popup with the provided message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun fetchLastLocation() {
        // If permissions aren't explicitly granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION_PERMISSION
            )
        }
        else {
            // Otherwise just get the location
            getLastLocation()
        }
    }

    // Suppress the missing permission because the function can only be called after permission is granted
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                Log.d("LOCATION STATUS", "${currentLocation!!.latitude}, ${currentLocation!!.longitude}")
            } else {
                Log.d("LOCATION STATUS", "Location is null")
            }
        }.addOnFailureListener { exception ->
            Log.e("LOCATION STATUS", "Failed to get location", exception)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastLocation()
                } else {
                    Log.d("LOCATION STATUS", "Permission denied")

                    showPermissionDeniedDialog()
                }
                return
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        // Prepare the popup
        val mDialog = Dialog(this)
        mDialog.setContentView(R.layout.popup_denied_location_permission)
        mDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        mDialog.show()

        // Redirects the user to the app's settings after clicking the switch permissions button
        mDialog.findViewById<Button>(R.id.btnPopupAskPermissionAgain).setOnClickListener{
            mDialog.hide()

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            comingFromSettings = true
            startActivity(intent)
        }

        mDialog.findViewById<Button>(R.id.btnPopupOK).setOnClickListener{
            mDialog.hide()
        }
    }

    // If the user is returning from switching permissions in the settings, refetch the location
    override fun onResume() {
        super.onResume()
        if (comingFromSettings) {
            fetchLastLocation()
            comingFromSettings = false
        }
    }

    override fun onEditClicked(address: String) {
        val actvAddressInput = findViewById<AutoCompleteTextView>(R.id.actvAddressName)

        actvAddressInput.setText(address)
        actvAddressInput.setSelection(actvAddressInput.text.length)

        val addressToRemove = Address(address)
        val newList = addressAdapter.currentList.toMutableList().apply {
            remove(addressToRemove)
        }
        addressAdapter.submitList(newList)

    }

    private fun initializePlacesAPI() {
        val applicationInfo: ApplicationInfo = this.packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
        val apiKey: String = applicationInfo.metaData["MAPS_API_KEY"].toString()
        Log.d("API KEY", apiKey)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        placesClient = Places.createClient(this)
    }

    private fun updateAutocompletePredictions(query: String) {
        // Define the bounds for your autocomplete predictions
        val bounds = RectangularBounds.newInstance(
            // Romania's box coordinates
            com.google.android.gms.maps.model.LatLng(43.6884447292, 20.2201924985),
            com.google.android.gms.maps.model.LatLng(48.2208812526, 29.62654341)
        )
        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request =
            FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(bounds)
                //.setLocationRestriction(bounds)
                .setOrigin(currentLocation?.let { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) })
                .setCountries("RO")
                .setSessionToken(token)
                .setQuery(query)
                .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                // Handle the success scenario here, you can update the UI with the predictions
                val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }

                autoCompleteAdapter.clear()
                autoCompleteAdapter.addAll(predictions)

                for (prediction in predictions)
                    Log.d("AUTOCOMPLETE", prediction)

            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Log.d("AUTOCOMPLETE", "Place not found: ${exception.statusCode}")
                }
            }
    }
}