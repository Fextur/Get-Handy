package com.example.gethandy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gethandy.databinding.FragmentProfileBinding
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style

class ProfileFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditing = false
    private var isBusinessAccount = false
    private var isCurrentUser = true // Assume it's the current user
    private var mapboxMap: MapboxMap? = null
    private var businessLatLng: LatLng? = null // Business location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId") // Get userId from arguments
        isCurrentUser = userId == null

        loadProfileData()
        updateButtons()

        // 🎯 Initialize Map
        binding.mapViewBusinessLocation.onCreate(savedInstanceState)
        binding.mapViewBusinessLocation.getMapAsync(this)

        // 🔄 Toggle Edit Mode
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) saveProfileChanges() else enableEditMode()
        }

        // 📅 Book Appointment (Only if it's another user's profile)
        binding.btnBookAppointment.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_appointment_booking)
        }

        // 🎯 Handle Business Account Toggle
        binding.radioGroupBusiness.setOnCheckedChangeListener { _, checkedId ->
            isBusinessAccount = checkedId == R.id.radioBusinessYes
            toggleBusinessFields(isBusinessAccount)
        }
    }

    private fun loadProfileData() {
        val userName = "John Doe"
        val userEmail = "john.doe@example.com"
        val userPhone = "+1 123-456-7890"

        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
        binding.tvUserPhone.text = userPhone

        binding.etUserName.setText(userName)
        binding.etUserEmail.setText(userEmail)
        binding.etUserPhone.setText(userPhone)

        // Assume another user is a business, but don't prefill business fields unless they're actually a business.
        isBusinessAccount = !isCurrentUser

        if (isBusinessAccount) {
            val businessName = "Doe Plumbing"
            val businessDescription = "Professional plumbing services"
            val businessAddress = "123 Main Street"
            businessLatLng = LatLng(32.0853, 34.7818) // Example location (Tel Aviv)

            binding.etBusinessName.setText(businessName)
            binding.etBusinessDescription.setText(businessDescription)
            binding.etBusinessAddress.setText(businessAddress)
        } else {
            businessLatLng = null
        }

        setBusinessFieldPlaceholders()
        toggleBusinessFields(isBusinessAccount)
    }

    private fun updateButtons() {
        if (isCurrentUser) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnBookAppointment.visibility = View.GONE
        } else {
            binding.btnEditProfile.visibility = View.GONE
            binding.btnBookAppointment.visibility = View.VISIBLE
        }
    }

    private fun enableEditMode() {
        isEditing = true
        binding.btnEditProfile.text = "Save Profile"

        binding.tvBusinessQuestion.visibility = View.VISIBLE
        binding.radioGroupBusiness.visibility = View.VISIBLE

        toggleField(binding.tvUserName, binding.etUserName, true)
        toggleField(binding.tvUserEmail, binding.etUserEmail, true)
        toggleField(binding.tvUserPhone, binding.etUserPhone, true)

        if (isBusinessAccount) {
            toggleField(binding.tvBusinessName, binding.etBusinessName, true)
            toggleField(binding.tvBusinessDescription, binding.etBusinessDescription, true)
            toggleField(binding.tvBusinessAddress, binding.etBusinessAddress, true)
        }

        mapboxMap?.uiSettings?.setAllGesturesEnabled(true)

        toggleBusinessFields(isBusinessAccount)
    }

    private fun saveProfileChanges() {
        isEditing = false
        binding.btnEditProfile.text = "Edit Profile"

        val newName = binding.etUserName.text.toString()
        val newEmail = binding.etUserEmail.text.toString()
        val newPhone = binding.etUserPhone.text.toString()

        binding.tvUserName.text = newName
        binding.tvUserEmail.text = newEmail
        binding.tvUserPhone.text = newPhone

        toggleField(binding.tvUserName, binding.etUserName, false)
        toggleField(binding.tvUserEmail, binding.etUserEmail, false)
        toggleField(binding.tvUserPhone, binding.etUserPhone, false)

        if (isBusinessAccount) {
            toggleField(binding.tvBusinessName, binding.etBusinessName, false)
            toggleField(binding.tvBusinessDescription, binding.etBusinessDescription, false)
            toggleField(binding.tvBusinessAddress, binding.etBusinessAddress, false)
        }

        binding.tvBusinessQuestion.visibility = View.GONE
        binding.radioGroupBusiness.visibility = View.GONE

        toggleBusinessFields(isBusinessAccount)

        mapboxMap?.uiSettings?.setAllGesturesEnabled(false)
    }

    private fun toggleField(textView: View, editText: View, isEditing: Boolean) {
        textView.visibility = if (isEditing) View.GONE else View.VISIBLE
        editText.visibility = if (isEditing) View.VISIBLE else View.GONE
    }

    private fun toggleBusinessFields(isBusiness: Boolean) {
        binding.cardBusinessInfo.visibility = if (isBusiness) View.VISIBLE else View.GONE
    }

    private fun setBusinessFieldPlaceholders() {
        binding.etBusinessName.hint = "Business Name"
        binding.etBusinessDescription.hint = "Business Description"
        binding.etBusinessAddress.hint = "Business Address"
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPBOX_API_KEY}") {
            businessLatLng?.let { latLng ->
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
                mapboxMap.addMarker(com.mapbox.mapboxsdk.annotations.MarkerOptions().position(latLng))
            } ?: centerMapOnUser()
        }

        mapboxMap.addOnMapClickListener { point ->
            if (isEditing) {
                businessLatLng = point
                mapboxMap.clear()
                mapboxMap.addMarker(com.mapbox.mapboxsdk.annotations.MarkerOptions().position(point))
            }
            true
        }

        mapboxMap.uiSettings.setAllGesturesEnabled(false)
    }

    private fun centerMapOnUser() {
        val defaultLocation = LatLng(32.0853, 34.7818) // Example (Tel Aviv)
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12.0))
    }

    override fun onStart() { super.onStart(); binding.mapViewBusinessLocation.onStart() }
    override fun onResume() { super.onResume(); binding.mapViewBusinessLocation.onResume() }
    override fun onPause() { super.onPause(); binding.mapViewBusinessLocation.onPause() }
    override fun onStop() { super.onStop(); binding.mapViewBusinessLocation.onStop() }
    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapViewBusinessLocation.onDestroy()
        _binding = null
    }
}
