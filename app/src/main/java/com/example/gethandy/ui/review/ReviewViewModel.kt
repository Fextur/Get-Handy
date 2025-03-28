package com.example.gethandy.ui.review

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
import com.example.gethandy.data.model.Review
import com.example.gethandy.data.repository.ReviewRepository
import com.example.gethandy.utils.NetworkResult
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val reviewDao = AppDatabase.getDatabase(application).reviewDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val reviewRepository = ReviewRepository(
        reviewDao = reviewDao,
        userDao = userDao,
        context = getApplication()
    )

    private val _reviewSubmissionState = MutableLiveData<NetworkResult<Review>>()
    val reviewSubmissionState: LiveData<NetworkResult<Review>> = _reviewSubmissionState

    fun submitReview(
        reviewerId: String,
        reviewedId: String,
        content: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _reviewSubmissionState.value = NetworkResult.Loading

            try {
                val result = reviewRepository.createReview(
                    reviewerId = reviewerId,
                    reviewedId = reviewedId,
                    content = content,
                    imageUri = imageUri
                )

                _reviewSubmissionState.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review", e)
                _reviewSubmissionState.value = NetworkResult.Error(
                    getApplication<Application>().getString(R.string.error_creating_review)
                )
            }
        }
    }
}