package com.example.gethandy.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gethandy.TAG
import com.example.gethandy.data.local.AppDatabase
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.BusinessWithOwner
import com.example.gethandy.data.repository.BusinessRepository
import com.example.gethandy.data.repository.UserRepository
import com.example.gethandy.utils.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import kotlin.math.abs

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val businessDao = AppDatabase.getDatabase(application).businessDao()
    private val userRepository = UserRepository(userDao)
    private val businessRepository = BusinessRepository(businessDao, userDao)

    private val _nearbyBusinesses = MutableLiveData<NetworkResult<List<Business>>>()
    val nearbyBusinesses: LiveData<NetworkResult<List<Business>>> = _nearbyBusinesses

    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    val localBusinessesWithOwners: LiveData<List<BusinessWithOwner>> = businessDao.getAllBusinessesWithOwners()

    private var lastFetchedCenter: LatLng? = null
    private var lastFetchedRadius: Double? = null
    private var fetchJob: Job? = null

    fun setCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        fetchNearbyBusinesses(location)
    }

    fun fetchNearbyBusinesses(location: LatLng, radiusInKm: Double = 2.0) {
        if (shouldSkipFetch(location, radiusInKm)) {
            return
        }
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _nearbyBusinesses.value = NetworkResult.Loading
            try {
                delay(100)

                val result = businessRepository.getNearbyBusinesses(location, radiusInKm)
                _nearbyBusinesses.value = result

                lastFetchedCenter = location
                lastFetchedRadius = radiusInKm

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby businesses")
                _nearbyBusinesses.value = NetworkResult.Error(e.message ?: "Error fetching nearby businesses")
            }
        }
    }

    private fun shouldSkipFetch(location: LatLng, radiusInKm: Double): Boolean {
        val lastCenter = lastFetchedCenter ?: return false
        val lastRadius = lastFetchedRadius ?: return false

        val distanceThreshold = radiusInKm * 0.2
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            lastCenter.latitude, lastCenter.longitude,
            location.latitude, location.longitude,
            results
        )

        val distanceKm = results[0] / 1000
        val radiusDifference = abs(radiusInKm - lastRadius)

        return distanceKm < distanceThreshold && radiusDifference < (lastRadius * 0.2)
    }

    fun logout() {
        userRepository.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }
}