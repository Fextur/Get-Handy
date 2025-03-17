package com.example.gethandy.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.BusinessDao
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.BusinessWithOwner
import com.example.gethandy.utils.NetworkResult
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng


class BusinessRepository(
    private val businessDao: BusinessDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getNearbyBusinesses(center: LatLng, radiusInKm: Double): NetworkResult<List<Business>> {
        return withContext(Dispatchers.IO) {
            try {
                val centerLocation = GeoLocation(center.latitude, center.longitude)
                val bounds = GeoFireUtils.getGeoHashQueryBounds(centerLocation, radiusInKm * 1000)

                val matchingBusinesses = mutableListOf<Business>()

                bounds.forEach { bound ->
                    val query = firestore.collection("businesses")
                        .orderBy("geoHash")
                        .startAt(bound.startHash)
                        .endAt(bound.endHash)
                        .get()
                        .await()

                    for (doc in query.documents) {
                        val businessId = doc.id
                        val userId = doc.getString("userId") ?: continue
                        val businessName = doc.getString("businessName") ?: continue
                        val description = doc.getString("description") ?: ""
                        val address = doc.getString("address") ?: ""
                        val profession = doc.getString("profession") ?: ""
                        val geoHash = doc.getString("geoHash") ?: continue

                        val locationMap = doc.get("location") as? Map<*, *> ?: continue
                        val lat = locationMap["latitude"] as? Double ?: continue
                        val lng = locationMap["longitude"] as? Double ?: continue
                        val location = LatLng(lat, lng)

                        // Calculate distance to filter out businesses outside the radius
                        val distanceInM = GeoFireUtils.getDistanceBetween(
                            GeoLocation(lat, lng),
                            centerLocation
                        )

                        if (distanceInM <= radiusInKm * 1000) {
                            val business = Business(
                                businessId = businessId,
                                userId = userId,
                                businessName = businessName,
                                description = description,
                                address = address,
                                profession = profession,
                                location = location,
                                geoHash = geoHash
                            )

                            matchingBusinesses.add(business)
                            businessDao.insertBusiness(business)
                        }
                    }
                }

                NetworkResult.Success(matchingBusinesses)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby businesses")
                NetworkResult.Error(e.message ?: "Error fetching nearby businesses")
            }
        }
    }

    suspend fun saveOrUpdateBusiness(
        businessId: String?,
        userId: String,
        businessName: String,
        description: String,
        address: String,
        profession: String,
        location: LatLng
    ): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val geoHash = GeoFireUtils.getGeoHashForLocation(
                    GeoLocation(location.latitude, location.longitude)
                )

                val businessData = mapOf(
                    "userId" to userId,
                    "businessName" to businessName,
                    "description" to description,
                    "address" to address,
                    "profession" to profession,
                    "location" to mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude
                    ),
                    "geoHash" to geoHash
                )

                val newBusinessId = if (businessId != null) {
                    firestore.collection("businesses").document(businessId)
                        .update(businessData).await()
                    businessId
                } else {
                    val docRef = firestore.collection("businesses").document()
                    docRef.set(businessData).await()
                    docRef.id
                }

                val business = Business(
                    businessId = newBusinessId,
                    userId = userId,
                    businessName = businessName,
                    description = description,
                    address = address,
                    profession = profession,
                    location = location,
                    geoHash = geoHash
                )

                businessDao.insertBusiness(business)

                NetworkResult.Success(newBusinessId)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving business")
                NetworkResult.Error(e.message ?: "Error saving business")
            }
        }
    }

    suspend fun deleteBusiness(businessId: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("businesses").document(businessId).delete().await()

                val business = businessDao.getBusinessById(businessId).value
                if (business != null) {
                    businessDao.deleteBusiness(business)
                }

                NetworkResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting business")
                NetworkResult.Error(e.message ?: "Error deleting business")
            }
        }
    }
}