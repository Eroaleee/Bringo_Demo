package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), AddressAdapter.AddressClickListener {
    private lateinit var addressAdapter: AddressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addressAdapter = AddressAdapter(mutableListOf(), this)

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

        findViewById<Button>(R.id.btnStartRoute).setOnClickListener{
            val addresses: MutableList<String> = addressAdapter.getAddresses()

            fastestRoute(this, addresses, findViewById<CheckBox>(R.id.cbReturnToOrigin).isChecked)
        }
    }

    override fun onEditClicked(address: String) {
        val editText: EditText = findViewById(R.id.etAddressName)

        editText.setText(address)
        editText.setSelection(editText.text.length)
    }
}