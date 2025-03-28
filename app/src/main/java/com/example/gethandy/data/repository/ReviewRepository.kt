package com.example.gethandy.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.ReviewDao
import com.example.gethandy.data.local.dao.UserDao
import com.example.gethandy.data.model.Review
import com.example.gethandy.utils.ImageUploadService
import com.example.gethandy.utils.NetworkResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context
) {
    suspend fun createReview(
        reviewerId: String,
        reviewedId: String,
        content: String,
        imageUri: Uri? = null
    ): NetworkResult<Review> = withContext(Dispatchers.IO) {
        try {
            val reviewer = userDao.getUserByIdSync(reviewerId)
            val reviewedUser = userDao.getUserByIdSync(reviewedId)

            if (reviewer == null || reviewedUser == null) {
                return@withContext NetworkResult.Error(context.getString(R.string.error_invalid_users))
            }

            val imageUrl = imageUri?.let {
                ImageUploadService.uploadImage(imageUri, "review-image-${UUID.randomUUID()}")
            }
            if (imageUrl !== null){ Log.e(TAG, imageUrl) }else{Log.e(TAG, "nulll")}

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val review = Review(
                reviewId = UUID.randomUUID().toString(),
                reviewerId = reviewerId,
                reviewedId = reviewedId,
                content = content,
                date = currentDate,
                imageUrl = imageUrl
            )

            val reviewMap = mapOf(
                "reviewId" to review.reviewId,
                "reviewerId" to review.reviewerId,
                "reviewedId" to review.reviewedId,
                "content" to review.content,
                "date" to review.date,
                "imageUrl" to (review.imageUrl ?: "")
            )

            firestore.collection("reviews")
                .document(review.reviewId)
                .set(reviewMap)
                .await()

            reviewDao.insertReview(review)

            NetworkResult.Success(review)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating review", e)
            NetworkResult.Error(context.getString(R.string.error_creating_review))
        }
    }

    fun getCombinedUserReviewsPaged(userId: String, pageSize: Int = 10): Flow<PagingData<Review>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = { reviewDao.getCombinedUserReviewsPaged(userId) }
        ).flow
    }

    suspend fun refreshCombinedUserReviews(userId: String): NetworkResult<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {
                val combinedReviews = mutableListOf<Review>()

                val reviewerQuery = firestore.collection("reviews")
                    .whereEqualTo("reviewerId", userId)
                    .get()
                    .await()

                val reviewerReviews = reviewerQuery.documents.mapNotNull { processReviewsDocument(it) }
                combinedReviews.addAll(reviewerReviews)

                val reviewedQuery = firestore.collection("reviews")
                    .whereEqualTo("reviewedId", userId)
                    .get()
                    .await()

                val reviewedReviews = reviewedQuery.documents.mapNotNull { processReviewsDocument(it) }
                combinedReviews.addAll(reviewedReviews)

                val sortedReviews = combinedReviews.sortedByDescending { it.date }

                reviewDao.clearUserReviews(userId)
                if (sortedReviews.isNotEmpty()) {
                    reviewDao.insertReviews(sortedReviews)
                }

                return@withContext NetworkResult.Success(sortedReviews)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing user reviews: ${e.message}", e)
                return@withContext NetworkResult.Error(context.getString(R.string.error_fetching_review))
            }
        }
    }

    private fun processReviewsDocument(doc: DocumentSnapshot): Review? {
        return try {
            val reviewId = doc.id
            val reviewerId = doc.getString("reviewerId")
            val reviewedId = doc.getString("reviewedId")
            val date = doc.getString("date")
            val content = doc.getString("content")

            val imageUrl = doc.getString("imageUrl")

            if (reviewerId == null || reviewedId == null || date == null || content == null) {
                Log.e(TAG, "processReviewDocument: missing fields in doc ${doc.id}")
                return null
            }

            val review = Review(
                reviewId = reviewId,
                reviewerId = reviewerId,
                reviewedId = reviewedId,
                date = date,
                content = content,
                imageUrl = imageUrl
            )
            review
        } catch (e: Exception) {
            Log.e(TAG, "processReviewDocument: error parsing doc ${doc.id}", e)
            null
        }
    }
}