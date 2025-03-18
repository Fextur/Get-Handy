package com.example.gethandy.ui.booking

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentAppointmentBookingBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.showSnackbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppointmentBookingFragment : Fragment() {
    private var _binding: FragmentAppointmentBookingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentBookingViewModel by viewModels()
    private val args: AppointmentBookingFragmentArgs by navArgs()

    private var selectedDate: String? = null
    private var selectedTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadBusiness(args.businessId)

        setupListeners()
        observeViewModel()

        updateConfirmButtonState()
    }

    private fun setupListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnConfirmAppointment.setOnClickListener {
            confirmAppointment()
        }
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .setFirstDayOfWeek(Calendar.SUNDAY)

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Appointment Date")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateString = selectedDateFormat.format(selection)

            if (!viewModel.isDateAvailable(dateString)) {
                showSnackbar(binding.root, "Appointments are not available on Saturdays", SnackbarType.ERROR)
                return@addOnPositiveButtonClickListener
            }

            selectedDate = dateString

            val displayFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
            binding.btnSelectDate.text = displayFormat.format(Date(selection))

            viewModel.setSelectedDate(selectedDate!!)

            selectedTime = null
            binding.btnSelectTime.text = getString(R.string.pick_a_time)
            updateConfirmButtonState()
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        if (selectedDate == null) {
            showSnackbar(binding.root, "Please select a date first", SnackbarType.WARNING)
            return
        }

        val timeSlots = viewModel.availableTimeSlots.value
        if (timeSlots.isNullOrEmpty()) {
            showSnackbar(binding.root, "No available time slots for selected date", SnackbarType.ERROR)
            return
        }

        val timeDialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Time")
            .setItems(timeSlots.toTypedArray()) { _, which ->
                selectedTime = timeSlots[which]
                binding.btnSelectTime.text = selectedTime
                viewModel.setSelectedTime(selectedTime!!)
                updateConfirmButtonState()
            }
            .create()

        timeDialog.show()
    }

    private fun confirmAppointment() {
        if (selectedDate == null || selectedTime == null) {
            Log.e(TAG, "confirmAppointment: Missing date or time selection")
            showSnackbar(binding.root, "Please select both date and time", SnackbarType.WARNING)
            return
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val date = inputFormat.parse(selectedDate!!)
        val displayDate = if (date != null) displayFormat.format(date) else selectedDate

        val confirmDialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Appointment")
            .setMessage("Book appointment on $displayDate at $selectedTime?")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.bookAppointment()
            }
            .setNegativeButton("Cancel") { _, _ ->
            }
            .create()

        confirmDialog.show()
    }

    private fun updateConfirmButtonState() {
        binding.btnConfirmAppointment.isEnabled = selectedDate != null && selectedTime != null
    }

    private fun observeViewModel() {

        viewModel.bookingState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, "Appointment booked successfully", SnackbarType.SUCCESS)
                    findNavController().navigate(R.id.action_booking_to_appointments)
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "observeViewModel: Error - ${result.message}")
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

        viewModel.availableTimeSlots.observe(viewLifecycleOwner) { slots ->
            if (slots.isEmpty() && selectedDate != null) {
                showSnackbar(binding.root, "No available time slots for selected date", SnackbarType.WARNING)
            }
        }

        viewModel.business.observe(viewLifecycleOwner) { business ->
            if (business === null)  {
                Log.e(TAG, "observeViewModel: Business data is null")
                showSnackbar(binding.root, "Could not load business information", SnackbarType.ERROR)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}