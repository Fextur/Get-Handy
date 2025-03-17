package com.example.gethandy.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gethandy.BuildConfig
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentHomeBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.showSnackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var maplibreMap: MapLibreMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(requireContext(), BuildConfig.MAPLIBRE_API_KEY, WellKnownTileServer.MapLibre)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_search)
        }

        binding.btnMenu.setOnClickListener { button ->
            showPopupMenu(button)
        }
    }

    private fun observeViewModel() {
        viewModel.nearbyBusinesses.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    displayBusinessesOnMap(result.data)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            updateMapLocation(location)
        }

        viewModel.localBusinessesWithOwners.observe(viewLifecycleOwner) { businessesWithOwners ->
            Log.d(TAG,"Loaded ${businessesWithOwners.size} businesses with owners from local database")
        }
    }

    private fun displayBusinessesOnMap(businesses: List<com.example.gethandy.data.model.Business>) {
        maplibreMap.clear()

        businesses.forEach { business ->
            val markerOptions = org.maplibre.android.annotations.MarkerOptions()
                .position(business.location)
                .title(business.businessName)
                .snippet(business.profession)

            maplibreMap.addMarker(markerOptions)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.menu_home, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeToProfile(null))
                    true
                }
                R.id.action_appointments -> {
                    findNavController().navigate(R.id.action_home_to_appointments)
                    true
                }
                R.id.action_logout -> {
                    viewModel.logout()
                    UserManager.clearUser(requireContext())
                    findNavController().navigate(R.id.action_home_to_login)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    override fun onMapReady(maplibreMap: MapLibreMap) {
        this.maplibreMap = maplibreMap
        maplibreMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPLIBRE_API_KEY}") {
            requestLocationPermission()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableUserLocation()
        } else {
            Log.d(TAG,"Location permission denied")
            showSnackbar(binding.root, getString(R.string.location_permission_denied), SnackbarType.WARNING)
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            enableUserLocation()
        }
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationComponent: LocationComponent = maplibreMap.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), maplibreMap.style!!)
                .build()
        )

        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLatLng = LatLng(location.latitude, location.longitude)
                viewModel.setCurrentLocation(userLatLng)
            }
        }
    }

    private fun updateMapLocation(location: LatLng) {
        maplibreMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(location)
                    .zoom(15.0)
                    .tilt(30.0)
                    .bearing(0.0)
                    .build()
            ), 1000
        )
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}