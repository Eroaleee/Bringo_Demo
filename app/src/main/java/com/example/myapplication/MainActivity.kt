package com.example.myapplication

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var addressAdapter: AddressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addressAdapter = AddressAdapter(mutableListOf())

        val rvAddressList : RecyclerView = findViewById(R.id.rvAddressList)
        rvAddressList.adapter = addressAdapter
        rvAddressList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnAddAddress).setOnClickListener {
            val addressName = findViewById<EditText>(R.id.etAddressName).text.toString()
            if(addressName.isNotEmpty()) {
                val address = Address(addressName)
                addressAdapter.addAddress(address)
                findViewById<EditText>(R.id.etAddressName).text.clear()
            }
        }

    }
}