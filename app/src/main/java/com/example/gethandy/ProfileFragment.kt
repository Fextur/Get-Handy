package com.example.gethandy

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gethandy.databinding.FragmentProfileBinding
import com.example.gethandy.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback


class ProfileFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var isEditing = false
    private var isBusinessAccount = false
    private var isCurrentUser = true
    private var userId: String? = null
    private var profileImageUri: Uri? = null
    private var businessLatLng: LatLng? = null
    private var maplibreMap: MapLibreMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getString("userId") ?: UserManager.getUserId(requireContext())
        isCurrentUser = (userId == UserManager.getUserId(requireContext()))

        loadProfileData()
        updateButtons()

        binding.mapViewBusinessLocation.onCreate(savedInstanceState)
        binding.mapViewBusinessLocation.getMapAsync(this)

        binding.btnEditProfile.setOnClickListener {
            if (isEditing) saveProfileChanges() else enableEditMode()
        }

        binding.btnBookAppointment.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_appointment_booking)
        }

        binding.radioGroupBusiness.setOnCheckedChangeListener { _, checkedId ->
            isBusinessAccount = checkedId == R.id.radioBusinessYes
            toggleBusinessFields(isBusinessAccount)
        }

        binding.ivProfilePic.setOnClickListener {
            if (isEditing) selectProfileImage()
        }
    }

    private fun loadProfileData() {
        if(userId === null) return;

        firestore.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvUserName.text = document.getString("fullName") ?: "N/A"
                    binding.tvUserEmail.text = document.getString("email") ?: "N/A"
                    binding.tvUserPhone.text = document.getString("phone") ?: "N/A"

                    binding.etUserName.setText(binding.tvUserName.text)
                    binding.etUserEmail.setText(binding.tvUserEmail.text)
                    binding.etUserPhone.setText(binding.tvUserPhone.text)

//                    val profileImageUrl = document.getString("profileImageUrl")
//                    if (!profileImageUrl.isNullOrEmpty()) {
//                        Glide.with(this).load(profileImageUrl).into(binding.ivProfilePic)
//                    }
                }
            }


        isBusinessAccount = !isCurrentUser

        if (isBusinessAccount) {
            val businessName = "Doe Plumbing"
            val businessDescription = "Professional plumbing services"
            val businessAddress = "123 Main Street"
            businessLatLng = LatLng(32.0853, 34.7818)

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
        binding.btnEditProfile.text = getString(R.string.save_profile)

        binding.tvBusinessQuestion.visibility = View.VISIBLE
        binding.radioGroupBusiness.visibility = View.VISIBLE

        toggleField(binding.tvUserName, binding.etUserName, true)
        toggleField(binding.tvUserEmail, binding.etUserEmail, false)
        toggleField(binding.tvUserPhone, binding.etUserPhone, true)

        if (isBusinessAccount) {
            toggleField(binding.tvBusinessName, binding.etBusinessName, true)
            toggleField(binding.tvBusinessDescription, binding.etBusinessDescription, true)
            toggleField(binding.tvBusinessAddress, binding.etBusinessAddress, true)
        }

        maplibreMap?.uiSettings?.setAllGesturesEnabled(true)

        toggleBusinessFields(isBusinessAccount)
    }

    private fun saveProfileChanges() {
        isEditing = false
        binding.btnEditProfile.text = getString(R.string.edit_profile)

        val updatedData = mutableMapOf(
            "fullName" to binding.etUserName.text.toString(),
            "email" to binding.etUserEmail.text.toString(),
            "phone" to binding.etUserPhone.text.toString()
        )

        userId?.let { id ->
            firestore.collection("users").document(id).update(updatedData as Map<String, Any>)
                .addOnSuccessListener {
                    loadProfileData()
                }
        }

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

        maplibreMap?.uiSettings?.setAllGesturesEnabled(false)
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

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                profileImageUri = it
                binding.ivProfilePic.setImageURI(it)
                uploadProfileImage(it)
            }
        }

    private fun selectProfileImage() {
        imagePickerLauncher.launch("image/*")
    }

    private fun uploadProfileImage(uri: Uri) {
//        val imageRef = storage.reference.child("profile_pics/${userId}.jpg")
//        imageRef.putFile(uri)
//            .addOnSuccessListener {
//                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
//                    firestore.collection("users").document(userId!!).update("profilePicUrl", downloadUrl.toString())
//                }
//            }
    }

    override fun onMapReady(maplibreMap: MapLibreMap) {
        this.maplibreMap = maplibreMap
        maplibreMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPLIBRE_API_KEY}") {
            businessLatLng?.let { latLng ->
                maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
                maplibreMap.addMarker(MarkerOptions().position(latLng))
            } ?: enableUserLocation()

        }

        maplibreMap.addOnMapClickListener { point ->
            if (isEditing) {
                businessLatLng = point
                maplibreMap.clear()
                maplibreMap.addMarker(MarkerOptions().position(point))
            }
            true
        }

        maplibreMap.uiSettings.setAllGesturesEnabled(false)
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val locationComponent: LocationComponent = maplibreMap!!.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(requireContext(), maplibreMap!!.style!!)
                    .build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.NORMAL

            val lastLocation = locationComponent.lastKnownLocation
            if (lastLocation != null) {
                val userLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                maplibreMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15.0))
            }
        } else {
            centerMapOnUser()
        }
    }

    private fun centerMapOnUser() {
        val defaultLocation = LatLng(32.0853, 34.7818) // Example (Tel Aviv)
        maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12.0))
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

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}
