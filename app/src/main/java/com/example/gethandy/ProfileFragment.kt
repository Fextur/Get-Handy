package com.example.gethandy

import android.content.Intent
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.gethandy.databinding.FragmentProfileBinding
import com.example.gethandy.utils.UserManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class ProfileFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

    private var isEditing = false
    private var isCurrentUser = true
    private var userId: String? = null
    private var businessId: String? = null
    private var profileImageUri: Uri? = null
    private var businessLatLng: LatLng? = null
    private var maplibreMap: MapLibreMap? = null

    private val professionList = mutableListOf<String>()
    private lateinit var professionAdapter: ArrayAdapter<String>





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            MediaManager.get()
        } catch (e: Exception) {
            val config: HashMap<String, String> = hashMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(requireContext(), config)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getString("userId") ?: UserManager.getUserId(requireContext())
        isCurrentUser = (userId == UserManager.getUserId(requireContext()))

        loadProfileData()
        updateButtons()
        setBusinessFieldPlaceholders();

        binding.mapViewBusinessLocation.onCreate(savedInstanceState)
        binding.mapViewBusinessLocation.getMapAsync(this)

        binding.btnEditProfile.setOnClickListener {
            if (isEditing) saveProfileChanges() else enableEditMode()
        }

        binding.btnBookAppointment.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_appointment_booking)
        }

        binding.radioGroupBusiness.setOnCheckedChangeListener { _, checkedId ->
            toggleBusinessFields()
        }

        binding.ivProfilePic.setOnClickListener {
            if (isEditing) selectProfileImage()
        }

        professionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, professionList)
        binding.etBusinessProfession.setAdapter(professionAdapter)

        loadAllProfessions()

        binding.etBusinessProfession.setOnClickListener {
            binding.etBusinessProfession.showDropDown()
        }

        binding.etBusinessProfession.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim()
                val filteredList = if (searchText.isEmpty()) {
                    professionList
                } else {
                    professionList.filter { it.contains(searchText, ignoreCase = true) }
                }
                professionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filteredList)
                binding.etBusinessProfession.setAdapter(professionAdapter)
                binding.etBusinessProfession.showDropDown() // Ensure dropdown is always visible
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    private fun loadAllProfessions() {
        firestore.collection("professions")
            .orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                professionList.clear()
                for (doc in snapshot.documents) {
                    val profession = doc.getString("name") ?: continue
                    professionList.add(profession)
                }
                professionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading professions: ${e.message}")
            }
    }






    private fun loadProfileData() {
        if(userId === null) return;
        showLoading()
        firestore.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvUserName.text = document.getString("fullName") ?: "N/A"
                    binding.tvUserEmail.text = document.getString("email") ?: "N/A"
                    binding.tvUserPhone.text = document.getString("phone") ?: "N/A"
                    businessId = document.getString("businessId")

                    if (!businessId.isNullOrEmpty()) {
                        loadBusinessData(businessId!!)
                    }
                    toggleBusinessFields()

                    binding.etUserName.setText(binding.tvUserName.text)
                    binding.etUserPhone.setText(binding.tvUserPhone.text)
                    val profileImageUrl = document.getString("profilePicUrl")

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profileImageUrl).placeholder(R.drawable.student_avatar)
                            .error(R.drawable.student_avatar).into(binding.ivProfilePic)
                    } else {
                        binding.ivProfilePic.setImageResource(R.drawable.student_avatar)
                    }
                }
                hideLoading()
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Error loading profile: ${it.message}")
                hideLoading()
            }
    }

    private fun loadBusinessData(businessId: String) {
        firestore.collection("businesses").document(businessId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.tvBusinessName.text = (doc.getString("businessName"))
                    binding.tvBusinessDescription.text = (doc.getString("description"))
                    binding.tvBusinessAddress.text = (doc.getString("address"))
                    binding.tvBusinessProfession.text = (doc.getString("profession"))
                    val locationData = doc.get("location") as? Map<*, *>
                    if (locationData != null) {
                        val latitude = locationData["latitude"] as? Double ?: 0.0
                        val longitude = locationData["longitude"] as? Double ?: 0.0
                        businessLatLng = LatLng(latitude, longitude)
                        if(maplibreMap !== null) {
                            maplibreMap?.clear()
                            maplibreMap!!.addMarker(MarkerOptions().position(businessLatLng))
                            maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                businessLatLng!!, 15.0))
                        }

                    }

                    binding.etBusinessName.setText(binding.tvBusinessName.text)
                    binding.etBusinessDescription.setText(binding.tvBusinessDescription.text)
                    binding.etBusinessAddress.setText(binding.tvBusinessAddress.text)
                    binding.etBusinessProfession.setText(binding.tvBusinessProfession.text)
                }
            }
    }

    private fun updateButtons() {
        if (isCurrentUser) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnBookAppointment.visibility = View.GONE
        }  else {
            binding.btnEditProfile.visibility = View.GONE
            binding.btnBookAppointment.visibility =
                if (businessId.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun enableEditMode() {
        isEditing = true
        binding.btnEditProfile.text = getString(R.string.save_profile)

        binding.tvBusinessQuestion.visibility = View.VISIBLE
        binding.radioGroupBusiness.visibility = View.VISIBLE

        toggleField(binding.tvUserName, binding.layoutUserName)
        toggleField(binding.tvUserPhone, binding.layoutUserPhone)

        if (!businessId.isNullOrEmpty()) {
            binding.radioBusinessYes.isChecked = true
        }

        toggleBusinessFields()
        if(maplibreMap !== null) enableUserLocation()
    }

    private fun saveProfileChanges() {
        lifecycleScope.launch {
            isEditing = false
            binding.btnEditProfile.text = getString(R.string.edit_profile)

            val updatedData = mutableMapOf(
                "fullName" to binding.etUserName.text.toString(),
                "email" to binding.tvUserEmail.text,
                "phone" to binding.etUserPhone.text.toString()
            )

            profileImageUri?.let { uri ->
                updatedData["profilePicUrl"] = uploadProfileImage(uri) ?: ""
                profileImageUri = null
            }

            updatedData["businessId"] = if (binding.radioBusinessYes.isChecked) {
                saveOrUpdateBusiness() ?: ""
            } else {
                deleteBusinessIfNeeded()
                ""
            }

            updateFirestore(updatedData)
            loadProfileData()

            toggleField(binding.tvUserName, binding.layoutUserName)
            toggleField(binding.tvUserPhone, binding.layoutUserPhone)

            binding.tvBusinessQuestion.visibility = View.GONE
            binding.radioGroupBusiness.visibility = View.GONE
        }
    }

    private suspend fun updateFirestore(updatedData: Map<String, Any>) = suspendCoroutine { continuation ->
        userId?.let { id ->
            firestore.collection("users").document(id).update(updatedData)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        } ?: continuation.resumeWithException(Exception("User ID is null"))
    }

    private fun toggleField(textView: View, editText: View) {
        textView.visibility = if (isEditing) View.GONE else View.VISIBLE
        editText.visibility = if (isEditing) View.VISIBLE else View.GONE
    }

    private fun toggleBusinessFields() {
        val shouldShowBusinessFields = if (isEditing) {
            binding.radioBusinessYes.isChecked
        } else {
            !businessId.isNullOrEmpty()
        }

        binding.cardBusinessInfo.visibility = if (shouldShowBusinessFields) View.VISIBLE else View.GONE
        if(shouldShowBusinessFields) {
            toggleField(binding.tvBusinessName, binding.layoutBusinessName)
            toggleField(binding.tvBusinessDescription, binding.layoutBusinessDescription)
            toggleField(binding.tvBusinessAddress, binding.layoutBusinessAddress)
            toggleField(binding.tvBusinessProfession, binding.layoutBusinessProfession)


            maplibreMap?.uiSettings?.setAllGesturesEnabled(true)

        }
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
            }
        }

    private fun selectProfileImage() {
        imagePickerLauncher.launch("image/*")
    }

    private suspend fun uploadProfileImage(uri: Uri): String? = suspendCoroutine { continuation ->
        showLoading()
        MediaManager.get().upload(uri)
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    continuation.resume(imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(null)
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
            })
            .dispatch()
    }

    private suspend fun saveOrUpdateBusiness(): String? = suspendCoroutine { continuation ->
        val latitude = businessLatLng?.latitude ?: 0.0
        val longitude = businessLatLng?.longitude ?: 0.0
        val geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))

        val businessData = mapOf(
            "userId" to userId,
            "businessName" to binding.etBusinessName.text.toString(),
            "description" to binding.etBusinessDescription.text.toString(),
            "address" to binding.etBusinessAddress.text.toString(),
            "profession" to binding.etBusinessProfession.text.toString(),
            "location" to businessLatLng,
            "geoHash" to geoHash
        )

        if (!businessId.isNullOrEmpty()) {
            firestore.collection("businesses").document(businessId!!)
                .update(businessData)
                .addOnSuccessListener { continuation.resume(businessId) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        } else {
            val newBusinessRef = firestore.collection("businesses").document()
            newBusinessRef.set(businessData)
                .addOnSuccessListener {
                    businessId = newBusinessRef.id
                    continuation.resume(businessId)
                }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    private suspend fun deleteBusinessIfNeeded() {
        businessId?.let {
            firestore.collection("businesses").document(it).delete()
            businessId = null
        }
    }


    @SuppressLint("ClickableViewAccessibility")
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


        binding.mapViewBusinessLocation.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            v.onTouchEvent(event)
            true
        }
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

    private fun showLoading() {
        binding.progressBarProfile.visibility = View.VISIBLE
        binding.btnEditProfile.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBarProfile.visibility = View.GONE
        binding.btnEditProfile.isEnabled = true
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}
