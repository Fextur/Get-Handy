package com.example.gethandy.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gethandy.data.model.User
import com.example.gethandy.data.model.UserWithBusiness

@Dao
interface UserDao {
    @Transaction
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserWithBusiness(userId: String): LiveData<UserWithBusiness?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdSync(userId: String): User?
}
