package com.example.gethandy.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.gethandy.data.model.Review

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)

    // Simpler query that's less likely to cause issues
    @Query("SELECT * FROM reviews WHERE reviewedId = :userId ORDER BY date DESC")
    suspend fun getReviewsByUserIdList(userId: String): List<Review>

    // For pagination, use a simpler approach at first
    @Query("SELECT * FROM reviews WHERE reviewedId = :userId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getReviewsByUserIdPaged(userId: String, limit: Int, offset: Int): List<Review>

    @Query("SELECT * FROM reviews WHERE reviewId = :reviewId")
    suspend fun getReviewById(reviewId: String): Review?

    @Query("DELETE FROM reviews WHERE reviewId = :reviewId")
    suspend fun deleteReview(reviewId: String)

    // Safe PagingSource implementation
    @Query("SELECT * FROM reviews WHERE reviewedId = :userId ORDER BY date DESC")
    fun getReviewsByUserIdPaging(userId: String): PagingSource<Int, Review>
}