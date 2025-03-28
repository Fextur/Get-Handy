package com.example.gethandy.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gethandy.data.model.Profession

@Dao
interface ProfessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfessions(professions: List<Profession>)

    @Query("SELECT * FROM professions ORDER BY name ASC LIMIT :limit")
    suspend fun getProfessionsWithLimit(limit: Int): List<Profession>

    @Query("SELECT * FROM professions WHERE LOWER(name) LIKE LOWER(:query) ORDER BY name ASC LIMIT :limit")
    suspend fun searchProfessionsWithLimit(query: String, limit: Int): List<Profession>

}