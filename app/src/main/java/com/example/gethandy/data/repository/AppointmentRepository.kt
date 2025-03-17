package com.example.gethandy.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.AppointmentDao
import com.example.gethandy.data.model.Appointment
import com.example.gethandy.data.model.AppointmentWithDetails
import com.example.gethandy.utils.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID


class AppointmentRepository(
    private val appointmentDao: AppointmentDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Get appointment with details
    fun getAppointmentWithDetails(appointmentId: String): LiveData<AppointmentWithDetails?> {
        return appointmentDao.getAppointmentWithDetails(appointmentId)
    }

    // Get appointments for a user
    fun getAppointmentsForUser(userId: String): LiveData<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForUser(userId)
    }

    // Get appointments for a business
    fun getAppointmentsForBusiness(businessId: String): LiveData<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForBusiness(businessId)
    }

    // Book a new appointment
    suspend fun bookAppointment(
        userId: String,
        businessId: String,
        date: String,
        time: String
    ): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val appointmentId = UUID.randomUUID().toString()

                val appointmentData = mapOf(
                    "appointmentId" to appointmentId,
                    "userId" to userId,
                    "businessId" to businessId,
                    "date" to date,
                    "time" to time
                )

                firestore.collection("appointments")
                    .document(appointmentId)
                    .set(appointmentData)
                    .await()

                // Save to local database
                val appointment = Appointment(
                    appointmentId = appointmentId,
                    userId = userId,
                    businessId = businessId,
                    date = date,
                    time = time
                )

                appointmentDao.insertAppointment(appointment)

                NetworkResult.Success(appointmentId)
            } catch (e: Exception) {
                Log.e(TAG, "Error booking appointment")
                NetworkResult.Error(e.message ?: "Error booking appointment")
            }
        }
    }

    // Cancel appointment
    suspend fun cancelAppointment(appointmentId: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("appointments")
                    .document(appointmentId)
                    .delete()
                    .await()

                // Delete from local database
                val appointment = appointmentDao.getAppointmentWithDetails(appointmentId).value?.appointment
                if (appointment != null) {
                    appointmentDao.deleteAppointment(appointment)
                }

                NetworkResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling appointment")
                NetworkResult.Error(e.message ?: "Error canceling appointment")
            }
        }
    }

    // Fetch appointments for a user from Firestore
    suspend fun fetchAppointmentsForUser(userId: String): NetworkResult<List<Appointment>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val appointmentId = doc.getString("appointmentId") ?: return@mapNotNull null
                        val businessId = doc.getString("businessId") ?: return@mapNotNull null
                        val date = doc.getString("date") ?: return@mapNotNull null
                        val time = doc.getString("time") ?: return@mapNotNull null

                        val appointment = Appointment(
                            appointmentId = appointmentId,
                            userId = userId,
                            businessId = businessId,
                            date = date,
                            time = time
                        )

                        // Save to local database
                        appointmentDao.insertAppointment(appointment)

                        appointment
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing appointment document")
                        null
                    }
                }

                NetworkResult.Success(appointments)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching appointments")
                NetworkResult.Error(e.message ?: "Error fetching appointments")
            }
        }
    }

    // Fetch appointments for a business from Firestore
    suspend fun fetchAppointmentsForBusiness(businessId: String): NetworkResult<List<Appointment>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("businessId", businessId)
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val appointmentId = doc.getString("appointmentId") ?: return@mapNotNull null
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        val date = doc.getString("date") ?: return@mapNotNull null
                        val time = doc.getString("time") ?: return@mapNotNull null

                        val appointment = Appointment(
                            appointmentId = appointmentId,
                            userId = userId,
                            businessId = businessId,
                            date = date,
                            time = time
                        )

                        // Save to local database
                        appointmentDao.insertAppointment(appointment)

                        appointment
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing appointment document")
                        null
                    }
                }

                NetworkResult.Success(appointments)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching business appointments")
                NetworkResult.Error(e.message ?: "Error fetching business appointments")
            }
        }
    }
}