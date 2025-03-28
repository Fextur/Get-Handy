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

    @Query("SELECT * FROM reviews WHERE reviewerId = :userId OR reviewedId = :userId ORDER BY date DESC")
    fun getCombinedUserReviewsPaged(userId: String): PagingSource<Int, Review>

    @Query("DELETE FROM reviews WHERE reviewerId = :userId OR reviewedId = :userId")
    suspend fun clearUserReviews(userId: String)

}