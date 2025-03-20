package com.example.gethandy.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gethandy.TAG
import com.example.gethandy.data.local.AppDatabase
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.repository.ProfessionRepository
import com.example.gethandy.utils.MapUtils
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val professionDao = AppDatabase.getDatabase(application).professionDao()
    private val professionRepository = ProfessionRepository(professionDao)

    val filteredProfessions = professionRepository.filteredProfessions

    private val _searchResults = MutableLiveData<List<Business>>()
    val searchResults: LiveData<List<Business>> = _searchResults

    private val _userLocation = MutableLiveData<LatLng?>()
    val userLocation: MutableLiveData<LatLng?> = _userLocation

    private val defaultLocation = LatLng(32.0853, 34.7818)

    private val businessesList = mutableListOf(
        Business(
            businessId = "1",
            userId = "SIRVTNalHFfLjpEr39iVUunuPft2",
            businessName = "Plumber Pro",
            description = "Expert plumbing services for all home and commercial needs.",
            profession = "Plumber",
            address = "123 Water Street, Tel Aviv",
            location = LatLng(32.0953, 34.7818),  // 1.1 km away
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.0953, 34.7818))
        ),
        Business(
            businessId = "2",
            userId = "user2",
            businessName = "ElectriCity",
            description = "Professional electrical services for residential and commercial properties.",
            profession = "Electrician",
            address = "456 Circuit Avenue, Tel Aviv",
            location = LatLng(32.1153, 34.7918),  // 3.5 km away
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.1153, 34.7918))
        ),
        Business(
            businessId = "3",
            userId = "user3",
            businessName = "Carpenter's Corner",
            description = "Quality carpentry work for all your woodworking needs.",
            profession = "Carpenter",
            address = "789 Wood Street, Tel Aviv",
            location = LatLng(32.0653, 34.8018),  // 4.2 km away
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.0653, 34.8018))
        ),
        Business(
            businessId = "4",
            userId = "user4",
            businessName = "Paint Masters",
            description = "Professional painting services with attention to detail.",
            profession = "Painter",
            address = "101 Color Boulevard, Tel Aviv",
            location = LatLng(32.1253, 34.8118),  // 7.8 km away
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.1253, 34.8118))
        ),
        Business(
            businessId = "5",
            userId = "user5",
            businessName = "Garden Gurus",
            description = "Full-service gardening and landscaping for beautiful outdoor spaces.",
            profession = "Gardener",
            address = "202 Green Lane, Tel Aviv",
            location = LatLng(32.1453, 34.8218),  // 10.5 km away
            geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(32.1453, 34.8218))
        )
    )

    init {
        _searchResults.value = businessesList
        _userLocation.value = defaultLocation
    }

    fun loadUserLocationAsync() {
        viewModelScope.launch {
            try {
                val location = MapUtils.getUserLocationAsync(getApplication())
                if (location != null) {
                    _userLocation.value = location
                } else {
                    Log.d(TAG, "Using default location as user location is unavailable")
                    _userLocation.value = defaultLocation
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user location: ${e.message}")
                _userLocation.value = defaultLocation
            }
        }
    }

    fun searchProfessions(query: String, limit: Int = 15) {
        viewModelScope.launch {
            professionRepository.searchProfessions(query, limit)
        }
    }

    fun refreshProfessions() {
        viewModelScope.launch {
            try {
                professionRepository.refreshProfessions()
                professionRepository.searchProfessions("", 15)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing professions: ${e.message}")
            }
        }
    }
}