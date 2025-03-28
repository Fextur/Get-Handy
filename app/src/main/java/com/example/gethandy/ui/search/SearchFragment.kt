package com.example.gethandy.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.data.model.Business
import com.example.gethandy.ui.appointments.AppointmentsFragmentDirections
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.chip.ChipGroup
import org.maplibre.android.geometry.LatLng

class SearchFragment : Fragment() {

    private lateinit var rvBusinesses: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var etSearch: EditText
    private lateinit var chipGroupFilters: ChipGroup

    private val businessesList = mutableListOf(
        Business(
            businessId = "1",
            userId = "SIRVTNalHFfLjpEr39iVUunuPft2",
            businessName = "Plumber Pro",
            description = "Expert plumbing services for all home and commercial needs.",
            profession = "Plumber",
            address = "123 Water Street, Tel Aviv",
            location = LatLng(32.0853, 34.7818),
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.0853, 34.7818))
        )
    )

    private var userLat = 32.0853  // Default Tel Aviv
    private var userLon = 34.7818

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    private val occupations = listOf("Plumber", "Electrician", "Carpenter")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvBusinesses = view.findViewById(R.id.rvBusinesses)
        etSearch = view.findViewById(R.id.etSearch)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)

        businessAdapter = BusinessAdapter(businessesList, userLat, userLon) { userId ->
            val action = AppointmentsFragmentDirections.actionAppointmentsToProfile(userId)
            findNavController().navigate(action)
        }

        rvBusinesses.layoutManager = LinearLayoutManager(requireContext())
        rvBusinesses.adapter = businessAdapter


        businessAdapter.updateList(businessesList)

        chipGroupFilters.removeAllViews()
        for (occupation in occupations) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = occupation
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    filterBusinesses(if (isChecked) occupation else null)
                }
            }
            chipGroupFilters.addView(chip)
        }
    }

    private fun filterBusinesses(selectedOccupation: String?) {
        val filteredList = if (selectedOccupation == null) {
            businessesList
        } else {
            businessesList.filter { it.profession == selectedOccupation }
        }
        businessAdapter.updateList(filteredList)
    }
}