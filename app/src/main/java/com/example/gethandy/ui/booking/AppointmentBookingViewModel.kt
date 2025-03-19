package com.example.gethandy.ui.booking

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gethandy.TAG
import com.example.gethandy.data.local.AppDatabase
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.repository.AppointmentRepository
import com.example.gethandy.data.repository.BusinessRepository
import com.example.gethandy.data.repository.UserRepository
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.UserManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppointmentBookingViewModel(application: Application) : AndroidViewModel(application) {
    private val appointmentDao = AppDatabase.getDatabase(application).appointmentDao()
    private val businessDao = AppDatabase.getDatabase(application).businessDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val userRepository = UserRepository(userDao)
    private val businessRepository = BusinessRepository(businessDao, userDao)
    private val appointmentRepository = AppointmentRepository(appointmentDao, userRepository, businessRepository)

    private val _selectedDate = MutableLiveData<String>()

    private val _selectedTime = MutableLiveData<String>()

    private val _availableTimeSlots = MutableLiveData<List<String>>()
    val availableTimeSlots: LiveData<List<String>> = _availableTimeSlots

    private val _bookingState = MutableLiveData<NetworkResult<String>>()
    val bookingState: LiveData<NetworkResult<String>> = _bookingState

    private val _business = MutableLiveData<Business?>()
    val business: MutableLiveData<Business?> = _business


    fun loadBusiness(businessId: String) {
        viewModelScope.launch {
            try {
                when (val result = businessRepository.getBusinessById(businessId)) {
                    is NetworkResult.Success -> {
                        _business.value = result.data
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "loadBusiness: Failed to load business: ${result.message}")
                    }
                    is NetworkResult.Loading -> {
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadBusiness: Exception occurred", e)
            }
        }
    }
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        updateAvailableTimeSlots()
    }

    fun setSelectedTime(time: String) {
        _selectedTime.value = time
    }

    private fun updateAvailableTimeSlots() {
        viewModelScope.launch {
            val date = _selectedDate.value ?: return@launch

            val allPossibleSlots = generateTimeSlotsForDate(date)

            val business = _business.value
            if (business == null) {
                Log.e(TAG, "Business is null, cannot get booked slots")
                _availableTimeSlots.value = allPossibleSlots
                return@launch
            }

            try {
                val result = appointmentRepository.fetchAppointmentsForBusinessOnDate(business.businessId, date)
                if (result is NetworkResult.Success) {
                    val bookedSlots = result.data
                        .map { it.time }

                    val availableSlots = allPossibleSlots.filter { time ->
                        !bookedSlots.contains(time)
                    }

                    _availableTimeSlots.value = availableSlots
                } else {
                    Log.e(TAG, "Error fetching appointments: ${(result as? NetworkResult.Error)?.message}")
                    _availableTimeSlots.value = allPossibleSlots
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching appointments", e)
                _availableTimeSlots.value = allPossibleSlots
            }
        }
    }


    @SuppressLint("DefaultLocale")
    private fun generateTimeSlotsForDate(dateString: String): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString) ?: return emptyList()

        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY) {
            return emptyList()
        }

        val slots = mutableListOf<String>()
        val startHour = 8
        val endHour = if (dayOfWeek == Calendar.FRIDAY) 14 else 20

        val today = Calendar.getInstance()
        val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

        val currentHour = if (isToday) today.get(Calendar.HOUR_OF_DAY) else -1
        val currentMinute = if (isToday) today.get(Calendar.MINUTE) else -1

        for (hour in startHour until endHour) {
            if (isToday && hour < currentHour) continue

            if (isToday && hour == currentHour) {
                if (currentMinute < 30) {
                    slots.add(String.format("%02d:30", hour))
                }
            } else {
                slots.add(String.format("%02d:00", hour))
                slots.add(String.format("%02d:30", hour))
            }
        }
        return slots
    }

    fun bookAppointment() {

        val userId = UserManager.getUserId(getApplication())
        if (userId == null) {
            Log.e(TAG, "bookAppointment: User ID is null")
            _bookingState.value = NetworkResult.Error("User not logged in")
            return
        }

        val business = _business.value
        if (business == null) {
            Log.e(TAG, "bookAppointment: Business is null")
            _bookingState.value = NetworkResult.Error("Business information not available")
            return
        }

        val businessId = business.businessId

        val date = _selectedDate.value
        if (date == null) {
            Log.e(TAG, "bookAppointment: Date is null")
            _bookingState.value = NetworkResult.Error("Please select a date")
            return
        }

        val time = _selectedTime.value
        if (time == null) {
            Log.e(TAG, "bookAppointment: Time is null")
            _bookingState.value = NetworkResult.Error("Please select a time")
            return
        }

        viewModelScope.launch {
            _bookingState.value = NetworkResult.Loading
            _bookingState.value = appointmentRepository.bookAppointment(userId, businessId, date, time)

        }
    }

    fun isDateAvailable(dateString: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString) ?: return false

        val calendar = Calendar.getInstance()
        calendar.time = date

        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return false
        }

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis >= today.timeInMillis
    }
}