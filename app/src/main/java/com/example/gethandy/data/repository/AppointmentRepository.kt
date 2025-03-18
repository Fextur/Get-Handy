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


class AppointmentRepository(
    private val appointmentDao: AppointmentDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getAppointmentWithDetails(appointmentId: String): LiveData<AppointmentWithDetails?> {
        return appointmentDao.getAppointmentWithDetails(appointmentId)
    }

    fun getAppointmentsForUser(userId: String): LiveData<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForUser(userId)
    }

    fun getAppointmentsForBusiness(businessId: String): LiveData<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForBusiness(businessId)
    }

    suspend fun bookAppointment(
        userId: String,
        businessId: String,
        date: String,
        time: String
    ): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {

                val appointmentData = mapOf(
                    "userId" to userId,
                    "businessId" to businessId,
                    "date" to date,
                    "time" to time
                )

                try {
                    val docRef = firestore.collection("appointments")
                        .document()
                    docRef.set(appointmentData).await()
                    val appointment = Appointment(
                        appointmentId = docRef.id,
                        userId = userId,
                        businessId = businessId,
                        date = date,
                        time = time
                    )
                    try {
                        appointmentDao.insertAppointment(appointment)
                    } catch (e: Exception) {
                        Log.e(TAG, "bookAppointment: Error saving to local database", e)
                    }

                    NetworkResult.Success(appointment.appointmentId)
                } catch (e: Exception) {
                    Log.e(TAG, "bookAppointment: Error saving to Firestore", e)
                    return@withContext NetworkResult.Error("Error saving to Firestore: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "bookAppointment: Unexpected error", e)
                NetworkResult.Error(e.message ?: "Error booking appointment")
            }
        }
    }

    suspend fun cancelAppointment(appointmentId: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("appointments")
                    .document(appointmentId)
                    .delete()
                    .await()

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

    suspend fun fetchAppointmentsForBusiness(businessId: String): NetworkResult<List<Appointment>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("businessId", businessId)
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val appointmentId = doc.id
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

    suspend fun fetchAppointmentsForBusinessOnDate(businessId: String, date: String): NetworkResult<List<Appointment>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("businessId", businessId)
                    .whereEqualTo("date", date)
                    .get()
                    .await()


                val appointments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val appointmentId = doc.id
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        val time = doc.getString("time") ?: return@mapNotNull null

                        val appointment = Appointment(
                            appointmentId = appointmentId,
                            userId = userId,
                            businessId = businessId,
                            date = date,
                            time = time
                        )

                        appointmentDao.insertAppointment(appointment)

                        appointment
                    } catch (e: Exception) {
                        Log.e(TAG, "fetchAppointmentsForBusinessOnDate: Error parsing document", e)
                        null
                    }
                }

                NetworkResult.Success(appointments)
            } catch (e: Exception) {
                Log.e(TAG, "fetchAppointmentsForBusinessOnDate: Error", e)
                NetworkResult.Error(e.message ?: "Error fetching appointments for date")
            }
        }
    }
}