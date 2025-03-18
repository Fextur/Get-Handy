package com.example.gethandy.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.BusinessDao
import com.example.gethandy.data.local.dao.UserDao
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.BusinessWithOwner
import com.example.gethandy.data.model.User
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
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // In BusinessRepository.kt, modify the getNearbyBusinesses method
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

                            // Check if the user exists in the local database
                            val existingUser = userDao.getUserByIdSync(userId)

                            if (existingUser == null) {
                                // User doesn't exist locally, fetch from Firestore and save
                                try {
                                    val userDoc = firestore.collection("users").document(userId).get().await()
                                    if (userDoc.exists()) {
                                        val fullName = userDoc.getString("fullName") ?: ""
                                        val email = userDoc.getString("email") ?: ""
                                        val phone = userDoc.getString("phone") ?: ""
                                        val profilePicUrl = userDoc.getString("profilePicUrl") ?: ""
                                        val businessId = userDoc.getString("businessId")

                                        val user = User(
                                            userId = userId,
                                            fullName = fullName,
                                            email = email,
                                            phone = phone,
                                            profilePicUrl = profilePicUrl,
                                            businessId = businessId
                                        )

                                        // Insert user first to satisfy foreign key constraint
                                        userDao.insertUser(user)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error fetching user data for business: $businessId", e)
                                    // Continue without inserting this business
                                    continue
                                }
                            }

                            try {
                                // Now insert the business after ensuring user exists
                                businessDao.insertBusiness(business)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error inserting business: $businessId", e)
                            }
                        }
                    }
                }

                NetworkResult.Success(matchingBusinesses)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby businesses", e)
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
                    Log.d(TAG, "Updated business ID: $businessId")
                    businessId
                } else {
                    val docRef = firestore.collection("businesses").document()
                    docRef.set(businessData).await()
                    Log.d(TAG, "New business ID: $businessId")

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


                Log.d(TAG, "PRE-INSERT BUSINESS OBJECT:")
                Log.d(TAG, "ID: ${business.businessId}")
                Log.d(TAG, "UserID: ${business.userId}")
                Log.d(TAG, "Name: ${business.businessName}")
                Log.d(TAG, "Description: ${business.description}")
                Log.d(TAG, "Address: ${business.address}")
                Log.d(TAG, "Profession: ${business.profession}")
                Log.d(TAG, "Location: ${business.location}")
                Log.d(TAG, "GeoHash: ${business.geoHash}")

                try {
                    businessDao.insertBusiness(business)
                    Log.d(TAG, "BUSINESS INSERT SUCCEEDED")
                } catch (e: Exception) {
                    Log.e(TAG, "BUSINESS INSERT FAILED", e)
                }
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