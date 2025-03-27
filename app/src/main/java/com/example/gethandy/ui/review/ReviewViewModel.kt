package com.example.gethandy.ui.review

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.gethandy.data.local.AppDatabase
import com.example.gethandy.data.model.Review
import com.example.gethandy.data.model.ReviewWithUsers
import com.example.gethandy.data.repository.ReviewRepository
import com.example.gethandy.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    // Get DAOs from database
    private val reviewDao = AppDatabase.getDatabase(application).reviewDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    // Create repository instance
    private val reviewRepository = ReviewRepository(reviewDao, userDao, context = application)

    private val _reviewState = MutableLiveData<NetworkResult<Review>>()
    val reviewState: LiveData<NetworkResult<Review>> = _reviewState

    fun createReview(
        reviewerId: String,
        reviewedId: String,
        content: String,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _reviewState.value = NetworkResult.Loading
            val result = reviewRepository.createReview(reviewerId, reviewedId, content, imageUri)
            _reviewState.value = result
        }
    }

    // Simplified approach that doesn't return ReviewWithUsers
    fun getReviewsPaged(userId: String): Flow<PagingData<Review>> {
        return reviewRepository.getReviewsPaged(userId)
            .cachedIn(viewModelScope)
    }

    fun refreshReviews(userId: String) {
        viewModelScope.launch {
            reviewRepository.fetchReviewsFromFirestore(userId)
        }
    }
}