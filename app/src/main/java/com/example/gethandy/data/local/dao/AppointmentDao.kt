package com.example.gethandy.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gethandy.data.model.Appointment
import com.example.gethandy.data.model.AppointmentWithDetails

@Dao
interface AppointmentDao {
    @Transaction
    @Query("SELECT * FROM appointments WHERE appointmentId = :appointmentId")
    fun getAppointmentWithDetails(appointmentId: String): LiveData<AppointmentWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)
}