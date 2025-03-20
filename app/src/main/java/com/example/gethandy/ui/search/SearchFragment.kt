package com.example.gethandy.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gethandy.R
import com.example.gethandy.databinding.FragmentSearchBinding
import com.google.android.material.slider.Slider

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var businessAdapter: BusinessAdapter
    private val viewModel: SearchViewModel by viewModels()

    private var currentDistanceKm = 5.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkLocationPermission()
        setupProfessionAutocomplete()
        setupDistanceSlider()
        setupBusinessList()
        observeViewModel()

    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadUserLocationAsync()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadUserLocationAsync()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupProfessionAutocomplete() {
        binding.professionAutocomplete.setup(
            lifecycleOwner = viewLifecycleOwner,
            professions = viewModel.filteredProfessions,
            onSearch = { query, limit -> viewModel.searchProfessions(query, limit) }
        )

        viewModel.refreshProfessions()
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.apply {
            addOnChangeListener { _, value, _ ->
                currentDistanceKm = value.toDouble()
                binding.tvDistanceValue.text = getString(R.string.distance_km, value.toInt())
            }

            binding.tvDistanceValue.text = getString(R.string.distance_km, value.toInt())
        }
    }

    private fun setupBusinessList() {
        businessAdapter = BusinessAdapter(
            businesses = emptyList(),
            userLat = 0.0,
            userLon = 0.0
        ) { userId ->
            navigateToProfile(userId)
        }

        binding.rvBusinesses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = businessAdapter
        }
    }


    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { businesses ->
            businessAdapter.updateList(businesses)
            binding.tvResultsCount.text = getString(R.string.results_found, businesses.size)
        }

        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                businessAdapter = BusinessAdapter(
                    businesses = viewModel.searchResults.value ?: emptyList(),
                    userLat = location.latitude,
                    userLon = location.longitude
                ) { userId ->
                    navigateToProfile(userId)
                }
            }
            binding.rvBusinesses.adapter = businessAdapter

        }
    }

    private fun navigateToProfile(userId: String) {
        val action = SearchFragmentDirections.actionSearchToProfile(userId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}