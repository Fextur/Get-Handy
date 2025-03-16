package com.example.gethandy.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.adapters.AppointmentsAdapter
import com.example.gethandy.data.model.Appointment

class AppointmentsFragment : Fragment() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentsAdapter: AppointmentsAdapter
    private val appointmentsList = mutableListOf(
        Appointment("1", "January 1, 2025", "10:00 AM", "John Doe", "user_1"),
        Appointment("2", "January 3, 2025", "2:30 PM", "Jane Smith", "user_2")
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
                val bundle = Bundle().apply {
                    putString("userId", userId)
                }
                findNavController().navigate(R.id.action_appointments_to_profile, bundle)

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
                appointmentsList.remove(appointment)
                appointmentsAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
