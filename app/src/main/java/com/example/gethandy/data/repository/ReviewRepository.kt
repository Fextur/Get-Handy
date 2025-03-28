package com.example.gethandy.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun fetchPaginatedReview(
        userId: String,
        limit: Int = 15,
    ): NetworkResult<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {

                return@withContext coroutineScope {
                    val userReviewsDeferred = async {
                        firestore.collection("reviews")
                            .whereEqualTo("reviewerId", userId)
                            .orderBy("date", Query.Direction.DESCENDING)
                            .get()
                            .await()
                            .documents
                            .mapNotNull { processReviewsDocument(it) }
                    }

                    val userReviews = userReviewsDeferred.await().take(limit)

                    NetworkResult.Success(userReviews)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchPaginatedReviews: ERROR", e)
                NetworkResult.Error(context.getString(R.string.error_fetching_review))
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

    suspend fun uploadReviewImage(imageUri: Uri): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val imageUrl = ImageUploadService.uploadImage(imageUri, "review-id-${UUID.randomUUID()}")

                if (imageUrl != null) {
                    NetworkResult.Success(imageUrl)
                } else {
                    NetworkResult.Error(context.getString(R.string.error_upload_image))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading review image: ${e.message}")
                NetworkResult.Error(context.getString(R.string.error_upload_image))
            }
        }
    }
}








//    // Simplified implementation using a custom RemoteMediator
//    fun getReviewsPaged(userId: String): Flow<PagingData<Review>> {
//        return Pager(
//            config = PagingConfig(
//                pageSize = 10,
//                enablePlaceholders = false,
//                maxSize = 30
//            ),
//            pagingSourceFactory = { reviewDao.getReviewsByUserIdPaging(userId) }
//        ).flow
//    }
//
//    // Manual transformation to add User information
//    suspend fun getReviewWithUserInfo(review: Review): NetworkResult<ReviewWithUsers?> = withContext(Dispatchers.IO) {
//        try {
//            val reviewer = userDao.getUserByIdSync(review.reviewerId)
//            val reviewedUser = userDao.getUserByIdSync(review.reviewedId)
//
//            if (reviewer != null && reviewedUser != null) {
//                val reviewWithUsers = ReviewWithUsers(
//                    reviewer = reviewer,
//                    reviewedUser = reviewedUser,
//                    review = review
//                )
//                return@withContext NetworkResult.Success(reviewWithUsers)
//            }
//            return@withContext NetworkResult.Success(null)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting review with user info", e)
//            return@withContext NetworkResult.Error(e.message ?: "Unknown error")
//        }
//    }
//
//    suspend fun getReviewById(reviewId: String): NetworkResult<ReviewWithUsers> = withContext(Dispatchers.IO) {
//        try {
//            // Try to get from local database first
//            val review = reviewDao.getReviewById(reviewId)
//
//            if (review != null) {
//                // Manually construct ReviewWithUsers
//                val reviewer = userDao.getUserByIdSync(review.reviewerId)
//                val reviewedUser = userDao.getUserByIdSync(review.reviewedId)
//
//                if (reviewer != null && reviewedUser != null) {
//                    return@withContext NetworkResult.Success(
//                        ReviewWithUsers(
//                            reviewer = reviewer,
//                            reviewedUser = reviewedUser,
//                            review = review
//                        )
//                    )
//                }
//            }
//
//            // If not found in local database, try to fetch from Firestore
//            val document = firestore.collection("reviews").document(reviewId).get().await()
//            if (document.exists()) {
//                val reviewData = document.toObject(Review::class.java)
//                if (reviewData != null) {
//                    reviewDao.insertReview(reviewData)
//
//                    // Fetch user data if needed
//                    fetchUserIfNeeded(reviewData.reviewerId)
//                    fetchUserIfNeeded(reviewData.reviewedId)
//
//                    // Get updated user data
//                    val reviewer = userDao.getUserByIdSync(reviewData.reviewerId)
//                    val reviewedUser = userDao.getUserByIdSync(reviewData.reviewedId)
//
//                    if (reviewer != null && reviewedUser != null) {
//                        return@withContext NetworkResult.Success(
//                            ReviewWithUsers(
//                                reviewer = reviewer,
//                                reviewedUser = reviewedUser,
//                                review = reviewData
//                            )
//                        )
//                    }
//                }
//            }
//
//            return@withContext NetworkResult.Error(context.getString(R.string.error_review_not_found))
//        } catch (e: Exception) {
//            Log.e(TAG, "Error fetching review", e)
//            return@withContext NetworkResult.Error(context.getString(R.string.error_fetching_review))
//        }
//    }

//    private suspend fun fetchUserIfNeeded(userId: String) {
//        try {
//            val localUser = userDao.getUserByIdSync(userId)
//            if (localUser == null) {
//                val userDoc = firestore.collection("users").document(userId).get().await()
//                if (userDoc.exists()) {
//                    val userData = userDoc.toObject(User::class.java)
//                    userData?.let { userDao.insertUser(it) }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error fetching user data", e)
//        }
//    }
//
//    suspend fun fetchReviewsFromFirestore(reviewedId: String) = withContext(Dispatchers.IO) {
//        try {
//            // Fetch reviews for this user (where they are being reviewed)
//            val reviewDocs = firestore.collection("reviews")
//                .whereEqualTo("reviewedId", reviewedId)
//                .orderBy("date", Query.Direction.DESCENDING)
//                .get()
//                .await()
//
//            // Convert Firestore documents to Review objects
//            val reviews = reviewDocs.documents.mapNotNull { doc ->
//                try {
//                    doc.toObject(Review::class.java)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing review document", e)
//                    null
//                }
//            }
//
//            // Ensure all related users exist in local database
//            val userIds = reviews.flatMap { listOf(it.reviewerId, it.reviewedId) }.distinct()
//            for (id in userIds) {
//                fetchUserIfNeeded(id)
//            }
//
//            // Insert reviews into local database
//            if (reviews.isNotEmpty()) {
//                reviewDao.insertReviews(reviews)
//            } else {
//
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error fetching reviews", e)
//        }
//    }
//}