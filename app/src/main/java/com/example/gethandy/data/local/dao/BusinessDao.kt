package com.example.gethandy.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.BusinessWithOwner

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE businessId = :businessId")
    fun getBusinessById(businessId: String): LiveData<Business?>

    @Query("SELECT * FROM businesses")
    fun getAllBusinesses(): LiveData<List<Business>>

    @Transaction
    @Query("SELECT * FROM businesses")
    fun getAllBusinessesWithOwners(): LiveData<List<BusinessWithOwner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business)

    @Delete
    suspend fun deleteBusiness(business: Business)

    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinessesSync(): List<Business>

}