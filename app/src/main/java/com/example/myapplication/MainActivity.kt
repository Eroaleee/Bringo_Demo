package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity(), AddressAdapter.AddressClickListener {
    private lateinit var addressAdapter: AddressAdapter
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var comingFromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addressAdapter = AddressAdapter(mutableListOf(), this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fetchLastLocation()

        // Make the RecyclerView have a Linear layout
        val rvAddressList : RecyclerView = findViewById(R.id.rvAddressList)
        rvAddressList.adapter = addressAdapter
        rvAddressList.layoutManager = LinearLayoutManager(this)

        // Adds functionality to the address adding button
        findViewById<ImageButton>(R.id.btnAddAddress).setOnClickListener {
            // Gets the address name, removes extra spaces and trims it
            val addressName = findViewById<EditText>(R.id.etAddressName).text.toString().replace("\\s+".toRegex(), " ").trim()

            if(addressName.isNotEmpty()) {
                val address = Address(addressName)
                addressAdapter.addAddress(address)
                findViewById<EditText>(R.id.etAddressName).text.clear()
            }
        }

        // Adds functionality to the start route button
        findViewById<Button>(R.id.btnStartRoute).setOnClickListener{
            val addresses: MutableList<String> = addressAdapter.getAddresses()

            fastestRoute(this, currentLocation, addresses, findViewById<CheckBox>(R.id.cbReturnToOrigin).isChecked)
        }
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
        val editText: EditText = findViewById(R.id.etAddressName)

        editText.setText(address)
        editText.setSelection(editText.text.length)
    }
}