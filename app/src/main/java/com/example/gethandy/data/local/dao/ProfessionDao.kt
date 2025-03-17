package com.example.gethandy.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gethandy.data.model.Profession

@Dao
interface ProfessionDao {
    @Query("SELECT * FROM professions ORDER BY name ASC")
    fun getAllProfessions(): LiveData<List<Profession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProfessions(professions: List<Profession>)
}
