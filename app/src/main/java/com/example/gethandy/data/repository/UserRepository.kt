package com.example.gethandy.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.UserDao
import com.example.gethandy.data.model.User
import com.example.gethandy.data.model.UserWithBusiness
import com.example.gethandy.utils.ImageUploadService
import com.example.gethandy.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val context: Context
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
                val userId = authResult.user?.uid ?: return@withContext NetworkResult.Error(context.getString(R.string.error_creating_user))

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
                NetworkResult.Error(e.message ?: context.getString(R.string.error_unknown))
            }
        }
    }

    suspend fun loginUser(email: String, password: String): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: return@withContext NetworkResult.Error(context.getString(R.string.error_login_failed))

                NetworkResult.Success(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in user")
                NetworkResult.Error(context.getString(R.string.error_invalid_credentials))
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
                    NetworkResult.Error(context.getString(R.string.error_user_not_found))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data")
                NetworkResult.Error(context.getString(R.string.error_loading_user_data))
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
                if (businessId != null) {
                    updates["businessId"] = businessId
                } else {
                    updates["businessId"] = com.google.firebase.firestore.FieldValue.delete()
                }

                if (profileImageUrl != null) {
                    updates["profilePicUrl"] = profileImageUrl
                }

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
                    NetworkResult.Error(context.getString(R.string.error_user_not_found))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile", e)
                NetworkResult.Error(context.getString(R.string.error_updating_profile))
            }
        }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val imageUrl = ImageUploadService.uploadImage(imageUri, "user-profile-$userId")

                if (imageUrl != null) {
                    NetworkResult.Success(imageUrl)
                } else {
                    NetworkResult.Error(context.getString(R.string.error_upload_image))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading profile image: ${e.message}")
                NetworkResult.Error(context.getString(R.string.error_upload_image))
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}