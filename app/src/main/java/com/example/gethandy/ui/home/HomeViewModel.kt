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
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

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

    fun setCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        fetchNearbyBusinesses(location)
    }

    fun fetchNearbyBusinesses(location: LatLng, radiusInKm: Double = 10.0) {
        viewModelScope.launch {
            _nearbyBusinesses.value = NetworkResult.Loading
            try {
                val result = businessRepository.getNearbyBusinesses(location, radiusInKm)
                _nearbyBusinesses.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby businesses")
                _nearbyBusinesses.value = NetworkResult.Error(e.message ?: "Error fetching nearby businesses")
            }
        }
    }

    fun logout() {
        userRepository.signOut()
    }
}