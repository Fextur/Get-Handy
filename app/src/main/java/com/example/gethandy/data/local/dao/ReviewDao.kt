package com.example.gethandy.data.local.dao

import androidx.room.*
import com.example.gethandy.data.model.Review

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
}