package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter (
    private val addresses: MutableList<Address>
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        return AddressViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.address_list,
                parent,
                false
            )
        )
    }

    fun addAddress(address: Address) {
        addresses.add(address)
        notifyItemInserted(addresses.size - 1)
    }

    fun deleteAddress(position: Int) {
        addresses.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val currAddress = addresses[position]
        holder.itemView.apply {
            findViewById<TextView>(R.id.tvAddress).text = currAddress.address

            // findViewById<ImageButton>(R.id.btnEdit).
        }
    }

    override fun getItemCount(): Int {
        return addresses.size
    }
}