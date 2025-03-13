package com.example.gethandy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.adapter.BusinessAdapter
import com.example.gethandy.model.Business
import com.google.android.material.chip.ChipGroup

class SearchFragment : Fragment() {

    private lateinit var rvBusinesses: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var etSearch: EditText
    private lateinit var chipGroupFilters: ChipGroup

    private val businessesList = mutableListOf(
        Business("1", "Plumber Pro", "Plumber", 32.0853, 34.7818),
        Business("2", "Electric Fix", "Electrician", 32.0723, 34.7741),
        Business("3", "WoodWorks", "Carpenter", 32.0700, 34.7800)
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

        businessAdapter = BusinessAdapter(businessesList, userLat, userLon) { businessId ->
            findNavController().navigate(R.id.action_search_to_profile, Bundle().apply {
                putString("userId", businessId)
            })
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
            businessesList.filter { it.occupation == selectedOccupation }
        }
        businessAdapter.updateList(filteredList)
    }

}
