package com.example.gethandy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.data.model.Appointment
import com.example.gethandy.data.model.AppointmentWithDetails
import com.google.android.material.button.MaterialButton

class AppointmentsAdapter(
    private val appointments: MutableList<AppointmentWithDetails>,
    private val onProfileClick: (String) -> Unit,
    private val onCancelClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointmentWithDetails = appointments[position]
        holder.bind(appointmentWithDetails)
    }

    override fun getItemCount(): Int = appointments.size

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAppointmentTime)
        private val tvWith: TextView = itemView.findViewById(R.id.tvAppointmentWith)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancelAppointment)

        fun bind(appointmentWithDetails: AppointmentWithDetails) {
            val appointment = appointmentWithDetails.appointment
            val user = appointmentWithDetails.user

            tvDate.text = appointment.date
            tvTime.text = appointment.time
            tvWith.text = itemView.context.getString(R.string.appointment_with, user.fullName)

            itemView.setOnClickListener {
                onProfileClick(appointment.businessId)
            }

            btnCancel.setOnClickListener {
                onCancelClick(appointment)
            }
        }
    }
}