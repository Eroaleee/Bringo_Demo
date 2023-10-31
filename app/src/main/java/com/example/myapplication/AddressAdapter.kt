package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter (
    private val listener: AddressClickListener
) : ListAdapter<Address, AddressAdapter.AddressViewHolder>(DiffCallback()) {

    // Interface for connecting the RecyclerView to the Screen
    interface AddressClickListener {
        fun onEditClicked(address: String)
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnErase: ImageButton = itemView.findViewById(R.id.btnErase)
    }

    class DiffCallback : DiffUtil.ItemCallback<Address>() {
        override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem.address == newItem.address
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        return AddressViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.address_list,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val currAddress = getItem(position)
        holder.apply {
            tvAddress.text = currAddress.address
            btnEdit.setOnClickListener {
                listener.onEditClicked(currAddress.address)
                // You might want to handle the edit action differently
            }
            btnErase.setOnClickListener {
                // Use this function to remove an item from the list
                val newList = currentList.toMutableList().apply {
                    remove(currAddress)
                }
                submitList(newList)
            }
        }
    }

    fun getAddresses(): MutableList<String> {
        val addressList = mutableListOf<String>()

        currentList.forEach {
            addressList.add(it.address)
        }

        return addressList
    }
}