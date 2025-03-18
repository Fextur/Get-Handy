package com.example.gethandy.data.repository

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.UserDao
import com.example.gethandy.data.model.User
import com.example.gethandy.data.model.UserWithBusiness
import com.example.gethandy.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun getUserWithBusiness(userId: String): LiveData<UserWithBusiness?> {
        return userDao.getUserWithBusiness(userId)
    }

    suspend fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: return@withContext NetworkResult.Error("Failed to create user")

                val user = User(
                    userId = userId,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    profilePicUrl = "",
                    businessId = null
                )

                val userMap = mapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "profilePicUrl" to ""
                )

                firestore.collection("users").document(userId).set(userMap).await()
                userDao.insertUser(user)
                loadUser(userId)

                NetworkResult.Success(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering user")
                NetworkResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    suspend fun loginUser(email: String, password: String): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: return@withContext NetworkResult.Error("Login failed")

                NetworkResult.Success(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in user")
                NetworkResult.Error(e.message ?: "Invalid credentials")
            }
        }
    }

    suspend fun loadUser(userId: String): NetworkResult<User> {
        return withContext(Dispatchers.IO) {
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

                    userDao.insertUser(user)

                    NetworkResult.Success(user)
                } else {
                    NetworkResult.Error("User not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data")
                NetworkResult.Error(e.message ?: "Error loading user data")
            }
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String,
        phone: String,
        businessId: String? = null,
        profileImageUrl: String? = null
    ): NetworkResult<User> {
        return withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any?>()
                updates["fullName"] = fullName
                updates["phone"] = phone
                Log.d(TAG, "business ID: $businessId")
                if (businessId != null) {
                    updates["businessId"] = businessId
                } else {
                    updates["businessId"] = com.google.firebase.firestore.FieldValue.delete()
                }

                if (profileImageUrl != null) {
                    updates["profilePicUrl"] = profileImageUrl
                }

                Log.d(TAG, "business ID2: ${updates["businessId"]}")
                firestore.collection("users").document(userId).update(updates).await()

                val userDoc = firestore.collection("users").document(userId).get().await()

                if (userDoc.exists()) {
                    val updatedUser = User(
                        userId = userId,
                        fullName = fullName,
                        phone = phone,
                        email = userDoc.getString("email") ?: "",
                        profilePicUrl = profileImageUrl ?: userDoc.getString("profilePicUrl") ?: "",
                        businessId = businessId
                    )

                    userDao.insertUser(updatedUser)

                    NetworkResult.Success(updatedUser)
                } else {
                    NetworkResult.Error("User not found in Firestore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile", e)
                NetworkResult.Error(e.message ?: "Error updating profile")
            }
        }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestId = "$userId-${UUID.randomUUID()}"

                val imageUrl = uploadImageToCloudinary(imageUri, requestId)

                if (imageUrl != null) {
                    NetworkResult.Success(imageUrl)
                } else {
                    NetworkResult.Error("Failed to upload image")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading profile image", e)
                NetworkResult.Error(e.message ?: "Error uploading profile image")
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri, requestId: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = kotlin.coroutines.suspendCoroutine { continuation ->
                MediaManager.get().upload(imageUri)
                    .option("resource_type", "image")
                    .option("public_id", requestId)
                    .callback(object : UploadCallback {
                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            val imageUrl = resultData?.get("secure_url") as? String
                            continuation.resumeWith(Result.success(imageUrl))
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            Log.e(TAG, "Cloudinary upload error: ${error?.description}")
                            continuation.resumeWith(Result.success(null))
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    })
                    .dispatch()
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in Cloudinary upload", e)
            null
        }
    }
}