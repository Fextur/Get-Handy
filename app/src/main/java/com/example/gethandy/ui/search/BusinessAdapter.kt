package com.example.gethandy.ui.search

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.data.model.Business

class BusinessAdapter(
    private var businesses: List<Business>,
    private val userLat: Double,
    private val userLon: Double,
    private val onBusinessClick: (String) -> Unit
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = businesses[position]
        holder.bind(business)
    }

    override fun getItemCount(): Int = businesses.size

    fun updateList(newList: List<Business>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = businesses.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return businesses[oldItemPosition].businessId == newList[newItemPosition].businessId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return businesses[oldItemPosition] == newList[newItemPosition]
            }
        })

        businesses = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBusinessName)
        private val tvOccupation: TextView = itemView.findViewById(R.id.tvBusinessOccupation)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvBusinessDistance)

        fun bind(business: Business) {
            tvName.text = business.businessName
            tvOccupation.text = business.profession
            tvDistance.text = itemView.context.getString(R.string.distance_km_away, calculateDistance(business))

            itemView.setOnClickListener {
                onBusinessClick(business.userId)
            }
        }

        private fun calculateDistance(business: Business): Double {
            val userLocation = Location("").apply {
                latitude = userLat
                longitude = userLon
            }
            val businessLocation = Location("").apply {
                latitude = business.location.latitude
                longitude = business.location.longitude
            }
            return userLocation.distanceTo(businessLocation) / 1000.0
        }
    }
}