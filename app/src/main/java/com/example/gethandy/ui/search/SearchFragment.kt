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
import com.example.gethandy.data.model.Business
import com.example.gethandy.databinding.FragmentSearchBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.showSnackbar
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var businessAdapter: BusinessAdapter
    private val viewModel: SearchViewModel by viewModels()

    private var currentDistanceKm = 5.0
    private var searchDebounceJob: Job? = null

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
        setupSearchInputs()
        setupDistanceSlider()
        setupBusinessList()
        observeViewModel()
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadUserLocationAsync()
        } else {
            showSnackbar(binding.root, getString(R.string.location_permission_denied), SnackbarType.WARNING)
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

        // Add text change listener to profession autocomplete
        binding.professionAutocomplete.addTextChangeListener { _ ->
            debounceSearch()
        }

        // Also listen for item selection
        binding.professionAutocomplete.setOnItemClickListener { _ ->
            debounceSearch()
        }

        viewModel.refreshProfessions()
    }

    private fun setupSearchInputs() {
        // Business name search with debounce
        binding.etBusinessName.doAfterTextChanged { text ->
            debounceSearch()
        }
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.apply {
            addOnChangeListener { _, value, fromUser ->
                currentDistanceKm = value.toDouble()
                binding.tvDistanceValue.text = getString(R.string.distance_km, value.toInt())

                if (fromUser) {
                    debounceSearch()
                }
            }

            binding.tvDistanceValue.text = getString(R.string.distance_km, value.toInt())
        }
    }

    private fun debounceSearch() {
        searchDebounceJob?.cancel()
        searchDebounceJob = MainScope().launch {
            delay(500) // Debounce for 500ms
            updateSearch()
        }
    }

    private fun updateSearch() {
        val businessName = binding.etBusinessName.text.toString()
        val profession = binding.professionAutocomplete.getText()

        viewModel.updateSearchCriteria(
            name = businessName,
            profession = profession,
            distanceKm = currentDistanceKm
        )
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
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    updateBusinessAdapter(result.data)
                    binding.tvResultsCount.text = getString(R.string.results_found, result.data.size)
                    LoadingUtil.showLoading(requireContext(), false)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                // Update adapter with the user location
                if (::businessAdapter.isInitialized) {
                    val currentBusinesses = businessAdapter.getBusinesses()
                    businessAdapter = BusinessAdapter(
                        businesses = currentBusinesses,
                        userLat = location.latitude,
                        userLon = location.longitude
                    ) { userId ->
                        navigateToProfile(userId)
                    }
                    binding.rvBusinesses.adapter = businessAdapter
                }

                // Initial search when location is first available
                updateSearch()
            }
        }
    }

    private fun updateBusinessAdapter(businesses: List<Business>) {
        val location = viewModel.userLocation.value
        if (location != null && ::businessAdapter.isInitialized) {
            businessAdapter.updateList(businesses)
        }
    }

    private fun navigateToProfile(userId: String) {
        val action = SearchFragmentDirections.actionSearchToProfile(userId)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        // Only perform a new search if we already have data
        if (viewModel.searchResults.value is NetworkResult.Success) {
            try {
                updateSearch()
            } catch (e: Exception) {
                // Silently handle error on resume, since we already have results
                // and don't want to disrupt user experience
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchDebounceJob?.cancel()
        _binding = null
    }
}