package com.example.gethandy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gethandy.R
import com.example.gethandy.model.Appointment
import com.google.android.material.button.MaterialButton

class AppointmentsAdapter(
    private val appointments: MutableList<Appointment>,
    private val onProfileClick: (String) -> Unit,
    private val onCancelClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount(): Int = appointments.size

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAppointmentTime)
        private val tvWith: TextView = itemView.findViewById(R.id.tvAppointmentWith)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancelAppointment)

        fun bind(appointment: Appointment) {
            tvDate.text = appointment.date
            tvTime.text = appointment.time
            tvWith.text = itemView.context.getString(R.string.appointment_with, appointment.personName)


            // Navigate to profile when clicking the card
            itemView.setOnClickListener {
                onProfileClick(appointment.personId)
            }

            // Show confirmation popup when clicking "Cancel"
            btnCancel.setOnClickListener {
                onCancelClick(appointment)
            }
        }
    }
}
