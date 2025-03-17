package com.example.gethandy.ui.appointments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.ui.adapters.AppointmentsAdapter
import com.example.gethandy.data.model.Appointment
import com.example.gethandy.data.model.AppointmentWithDetails
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.User
import org.maplibre.android.geometry.LatLng

class AppointmentsFragment : Fragment() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentsAdapter: AppointmentsAdapter

    // Create sample data that matches our new models
    private val sampleUser1 = User(
        userId = "user_1",
        fullName = "John Doe",
        email = "john@example.com",
        phone = "123456789",
        profilePicUrl = "",
        businessId = null
    )

    private val sampleUser2 = User(
        userId = "user_2",
        fullName = "Jane Smith",
        email = "jane@example.com",
        phone = "987654321",
        profilePicUrl = "",
        businessId = null
    )

    private val sampleBusiness1 = Business(
        businessId = "business_1",
        userId = "user_3",
        businessName = "Plumber Service",
        description = "Plumbing services",
        address = "123 Main St",
        profession = "Plumber",
        location = LatLng(32.0853, 34.7818),
        geoHash = "sv8vb53nxz"
    )

    private val sampleBusiness2 = Business(
        businessId = "business_2",
        userId = "user_4",
        businessName = "Electrician Service",
        description = "Electrical services",
        address = "456 Oak St",
        profession = "Electrician",
        location = LatLng(32.0853, 34.7818),
        geoHash = "sv8vb53nxz"
    )

    private val appointment1 = Appointment(
        appointmentId = "1",
        userId = "user_1",
        businessId = "business_1",
        date = "January 1, 2025",
        time = "10:00 AM"
    )

    private val appointment2 = Appointment(
        appointmentId = "2",
        userId = "user_2",
        businessId = "business_2",
        date = "January 3, 2025",
        time = "2:30 PM"
    )

    private val appointmentsList = mutableListOf(
        AppointmentWithDetails(appointment1, sampleUser1, sampleBusiness1),
        AppointmentWithDetails(appointment2, sampleUser2, sampleBusiness2)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_appointments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvAppointments = view.findViewById(R.id.rvAppointments)

        appointmentsAdapter = AppointmentsAdapter(
            appointmentsList,
            onProfileClick = { userId ->
                val action = AppointmentsFragmentDirections.actionAppointmentsToProfile(userId)
                findNavController().navigate(action)
            },
            onCancelClick = { appointment ->
                showCancelConfirmation(appointment)
            }
        )
        rvAppointments.adapter = appointmentsAdapter
    }

    private fun showCancelConfirmation(appointment: Appointment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment?")
            .setPositiveButton("Yes") { _, _ ->
                // Remove the appointment that contains this appointment object
                val appointmentWithDetails = appointmentsList.find { it.appointment.appointmentId == appointment.appointmentId }
                appointmentWithDetails?.let {
                    appointmentsList.remove(it)
                    appointmentsAdapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}