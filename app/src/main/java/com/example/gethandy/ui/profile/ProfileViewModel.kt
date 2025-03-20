package com.example.gethandy.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.data.local.AppDatabase
import com.example.gethandy.data.model.User
import com.example.gethandy.data.model.UserWithBusiness
import com.example.gethandy.data.repository.BusinessRepository
import com.example.gethandy.data.repository.ProfessionRepository
import com.example.gethandy.data.repository.UserRepository
import com.example.gethandy.utils.NetworkResult
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val businessDao = AppDatabase.getDatabase(application).businessDao()
    private val professionDao = AppDatabase.getDatabase(application).professionDao()

    private val userRepository = UserRepository(userDao, context = getApplication())
    private val businessRepository = BusinessRepository(businessDao, userDao, context = getApplication())
    private val professionRepository = ProfessionRepository(professionDao)

    private val _userProfileState = MutableLiveData<NetworkResult<User>>()
    val userProfileState: LiveData<NetworkResult<User>> = _userProfileState

    private val _profileUpdateState = MutableLiveData<NetworkResult<Boolean>>()
    val profileUpdateState: LiveData<NetworkResult<Boolean>> = _profileUpdateState

    val filteredProfessions = professionRepository.filteredProfessions

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            _userProfileState.value = NetworkResult.Loading
            try {
                val result = userRepository.loadUser(userId)
                _userProfileState.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile")
                _userProfileState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_loading_user_profile))
            }
        }
    }

    fun getUserWithBusiness(userId: String): LiveData<UserWithBusiness?> {
        return userRepository.getUserWithBusiness(userId)
    }

    fun refreshBusinessData(businessId: String?) {
        if (businessId != null) {
            viewModelScope.launch {
                try {
                    val result = businessRepository.getBusinessById(businessId)
                    Log.d(TAG, "Business data refreshed: ${result is NetworkResult.Success}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing business data", e)
                }
            }
        }
    }

    fun saveProfileChanges(
        userId: String,
        fullName: String,
        phone: String,
        isBusiness: Boolean,
        businessId: String?,
        businessDetails: BusinessDetails?,
        profileImageUri: Uri?
    ) {
        viewModelScope.launch {
            _profileUpdateState.value = NetworkResult.Loading

            try {
                var newBusinessId: String? = businessId
                var profileImageUrl: String? = null

                if (isBusiness) {
                    if (businessDetails != null) {
                        val businessResult = businessRepository.saveOrUpdateBusiness(
                            businessId = businessId,
                            userId = userId,
                            businessName = businessDetails.name,
                            description = businessDetails.description,
                            address = businessDetails.address,
                            profession = businessDetails.profession,
                            location = businessDetails.location
                        )

                        if (businessResult is NetworkResult.Success) {
                            newBusinessId = businessResult.data
                        } else if (businessResult is NetworkResult.Error) {
                            _profileUpdateState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_business_update_failed))
                            return@launch
                        }
                    } else {
                        _profileUpdateState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_missing_business_details))
                        return@launch
                    }
                } else if (businessId !== null) {
                    val deleteResult = businessRepository.deleteBusiness(businessId)
                    if (deleteResult is NetworkResult.Error) {
                        Log.e(TAG, "Failed to delete business: ${deleteResult.message}")
                    }
                    newBusinessId = null
                }

                if (profileImageUri != null) {
                    val imageResult = userRepository.uploadProfileImage(userId, profileImageUri)
                    if (imageResult is NetworkResult.Success) {
                        profileImageUrl = imageResult.data
                    } else if (imageResult is NetworkResult.Error) {
                        _profileUpdateState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_image_upload_failed))
                        return@launch
                    }
                }

                val userResult = userRepository.updateUserProfile(
                    userId = userId,
                    fullName = fullName,
                    phone = phone,
                    businessId = newBusinessId,
                    profileImageUrl = profileImageUrl
                )

                if (userResult is NetworkResult.Success) {
                    refreshBusinessData(newBusinessId)

                    val loadResult = userRepository.loadUser(userId)
                    if (loadResult is NetworkResult.Error) {
                        Log.e(TAG, "Failed to reload user after update: ${loadResult.message}")
                    }

                    _profileUpdateState.value = NetworkResult.Success(true)
                } else if (userResult is NetworkResult.Error) {
                    _profileUpdateState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_profile_update_failed))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in complete profile update", e)
                _profileUpdateState.value = NetworkResult.Error(getApplication<Application>().getString(R.string.error_updating_profile))
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

data class BusinessDetails(
    val name: String,
    val description: String,
    val address: String,
    val profession: String,
    val location: LatLng
)