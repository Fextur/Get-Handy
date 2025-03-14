package com.example.gethandy

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gethandy.utils.UserManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback


class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapLibreMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(requireContext(), BuildConfig.MAPBOX_API_KEY, WellKnownTileServer.MapLibre)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        view.findViewById<FloatingActionButton>(R.id.btnSearch).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_search)
        }

        view.findViewById<FloatingActionButton>(R.id.btnMenu).setOnClickListener { button ->
            showPopupMenu(button)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.menu_home, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    findNavController().navigate(R.id.action_home_to_profile)
                    true
                }
                R.id.action_appointments -> {
                    findNavController().navigate(R.id.action_home_to_appointments)
                    true
                }
                R.id.action_logout -> {
                    FirebaseAuth.getInstance().signOut()  // Firebase logout
                    UserManager.clearUser(requireContext())
                    findNavController().navigate(R.id.action_home_to_login)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    override fun onMapReady(mapboxMap: MapLibreMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPBOX_API_KEY}") {
            requestLocationPermission()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableUserLocation()
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

        val locationComponent: LocationComponent = mapboxMap.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), mapboxMap.style!!)
                .useDefaultLocationEngine(false)
                .build()
        )

        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                updateMapLocation(it)
            }
        }
    }

    private fun updateMapLocation(location: Location) {
        val userLatLng = LatLng(location.latitude, location.longitude)
        mapboxMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(15.0)
                    .tilt(30.0)
                    .bearing(0.0)
                    .build()
            ), 1000
        )
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
}
