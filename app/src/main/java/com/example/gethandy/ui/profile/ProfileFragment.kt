package com.example.gethandy.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.gethandy.BuildConfig
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentProfileBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.showSnackbar
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

    private val args: ProfileFragmentArgs by navArgs()
    private val viewModel: ProfileViewModel by viewModels()

    private var isEditing = false
    private var isCurrentUser = true
    private var userId: String? = null
    private var businessId: String? = null
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
        Log.d("ProfileFragment", "onViewCreated called ${UserManager.getUserId(requireContext())}")
        userId = args.userId ?: UserManager.getUserId(requireContext())
        isCurrentUser = (userId == UserManager.getUserId(requireContext()))

        binding.mapViewBusinessLocation.onCreate(savedInstanceState)
        binding.mapViewBusinessLocation.getMapAsync(this)

        setupListeners()
        setupProfessionAutocomplete()
        observeViewModel()

        userId?.let {
            viewModel.getUserProfile(it)
            viewModel.refreshProfessions()
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) saveProfileChanges() else enableEditMode()
        }

        binding.btnBookAppointment.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToAppointmentBooking())
        }

        binding.radioGroupBusiness.setOnCheckedChangeListener { _, _ ->
            toggleBusinessFields()
        }

        binding.ivProfilePic.setOnClickListener {
            if (isEditing) selectProfileImage()
        }
    }

    private fun setupProfessionAutocomplete() {
        val professionAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.etBusinessProfession.setAdapter(professionAdapter)

        binding.etBusinessProfession.setOnClickListener {
            binding.etBusinessProfession.showDropDown()
        }

        binding.etBusinessProfession.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etBusinessProfession.showDropDown()
            }
        }

        viewModel.professions.observe(viewLifecycleOwner) { professions ->
            val professionNames = professions.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                professionNames
            )
            binding.etBusinessProfession.setAdapter(adapter)

            binding.etBusinessProfession.showDropDown()
        }
    }

    private fun observeViewModel() {
        userId?.let { uid ->
            LoadingUtil.showLoading(requireContext(), true)
            viewModel.getUserWithBusiness(uid).observe(viewLifecycleOwner) { userWithBusiness ->
                userWithBusiness?.let { uwb ->
                    val user = uwb.user
                    binding.tvUserName.text = user.fullName
                    binding.tvUserEmail.text = user.email
                    binding.tvUserPhone.text = user.phone

                    binding.etUserName.setText(user.fullName)
                    binding.etUserPhone.setText(user.phone)

                    if (user.profilePicUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(user.profilePicUrl)
                            .placeholder(R.drawable.loading_icon)
                            .error(R.drawable.student_avatar)
                            .into(binding.ivProfilePic)
                    } else {
                        binding.ivProfilePic.setImageResource(R.drawable.student_avatar)
                    }

                    businessId = user.businessId

                    val business = uwb.business
                    if (business != null) {
                        binding.radioBusinessYes.isChecked = true

                        binding.tvBusinessName.text = business.businessName
                        binding.tvBusinessDescription.text = business.description
                        binding.tvBusinessAddress.text = business.address
                        binding.tvBusinessProfession.text = business.profession

                        binding.etBusinessName.setText(business.businessName)
                        binding.etBusinessDescription.setText(business.description)
                        binding.etBusinessAddress.setText(business.address)
                        binding.etBusinessProfession.setText(business.profession)

                        businessLatLng = business.location
                        updateMapWithBusinessLocation()
                    } else {
                        binding.radioBusinessNo.isChecked = false
                    }

                    toggleBusinessFields()
                    updateButtons()

                    LoadingUtil.showLoading(requireContext(), false)
                }
            }
        }

        viewModel.userProfileState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    LoadingUtil.showLoading(requireContext(), false)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    Log.e(TAG, "Error loading user profile: ${result.message}")
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

        viewModel.professions.observe(viewLifecycleOwner) { professions ->
            val professionNames = professions.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                professionNames
            )
            binding.etBusinessProfession.setAdapter(adapter)
        }

        viewModel.profileUpdateState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    isEditing = false
                    binding.btnEditProfile.text = getString(R.string.edit_profile)

                    toggleField(binding.tvUserName, binding.layoutUserName)
                    toggleField(binding.tvUserPhone, binding.layoutUserPhone)

                    binding.tvBusinessQuestion.visibility = View.GONE
                    binding.radioGroupBusiness.visibility = View.GONE
                    binding.etBusinessProfession.dismissDropDown()

                    toggleBusinessFields()

                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, getString(R.string.profile_update_success), SnackbarType.SUCCESS)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

    }

    private fun updateMapWithBusinessLocation() {
        maplibreMap?.let { map ->
            businessLatLng?.let { location ->
                map.clear()
                map.addMarker(MarkerOptions().position(location))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0))
            }
        }
    }

    private fun updateButtons() {
        if (isCurrentUser) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnBookAppointment.visibility = View.GONE
        } else {
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

        toggleBusinessFields()
        if (maplibreMap != null) enableUserLocation()
    }

    private fun isProfileValid(): Boolean {
        val fullName = binding.etUserName.text.toString().trim()
        val phone = binding.etUserPhone.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.layoutUserName.error = getString(R.string.error_full_name_required)
            return false
        } else {
            binding.layoutUserName.error = null
        }

        if (!isValidPhoneNumber(phone)) {
            binding.layoutUserPhone.error = getString(R.string.error_invalid_phone)
            return false
        } else {
            binding.layoutUserPhone.error = null
        }

        if (binding.radioBusinessYes.isChecked) {
            val businessName = binding.etBusinessName.text.toString().trim()
            val address = binding.etBusinessAddress.text.toString().trim()
            val profession = binding.etBusinessProfession.text.toString().trim()

            if (businessName.isEmpty()) {
                binding.layoutBusinessName.error = getString(R.string.error_business_name_required)
                return false
            } else {
                binding.layoutBusinessName.error = null
            }

            if (address.isEmpty()) {
                binding.layoutBusinessAddress.error = getString(R.string.error_business_address_required)
                return false
            } else {
                binding.layoutBusinessAddress.error = null
            }

            if (profession.isEmpty()) {
                binding.layoutBusinessProfession.error = getString(R.string.error_invalid_profession)
                return false
            } else {
                binding.layoutBusinessProfession.error = null
            }

            if (businessLatLng == null) {
                showSnackbar(binding.root, getString(R.string.error_select_location), SnackbarType.ERROR)
                return false
            }
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val regex = Regex("^\\+?[0-9]{7,15}\$")
        return regex.matches(phone)
    }

    private fun saveProfileChanges() {
        if (!isProfileValid()) return

        LoadingUtil.showLoading(requireContext(), true)

        val fullName = binding.etUserName.text.toString().trim()
        val phone = binding.etUserPhone.text.toString().trim()

        val businessDetails = if (binding.radioBusinessYes.isChecked) {
            BusinessDetails(
                name = binding.etBusinessName.text.toString().trim(),
                description = binding.etBusinessDescription.text.toString().trim(),
                address = binding.etBusinessAddress.text.toString().trim(),
                profession = binding.etBusinessProfession.text.toString().trim(),
                location = businessLatLng ?: LatLng(0.0, 0.0)
            )
        } else null

        userId?.let { uid ->
            viewModel.saveProfileChanges(
                userId = uid,
                fullName = fullName,
                phone = phone,
                isBusiness = binding.radioBusinessYes.isChecked,
                businessId = businessId,
                businessDetails = businessDetails,
                profileImageUri = profileImageUri
            )

            profileImageUri = null
        }
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

        if (!isEditing) {
            businessId?.let {
                binding.radioBusinessYes.isChecked = true
                binding.radioBusinessNo.isChecked = false
            }
        }

        binding.cardBusinessInfo.visibility = if (shouldShowBusinessFields) View.VISIBLE else View.GONE
        if (shouldShowBusinessFields) {
            toggleField(binding.tvBusinessName, binding.layoutBusinessName)
            toggleField(binding.tvBusinessDescription, binding.layoutBusinessDescription)
            toggleField(binding.tvBusinessAddress, binding.layoutBusinessAddress)
            toggleField(binding.tvBusinessProfession, binding.layoutBusinessProfession)
        }
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onMapReady(maplibreMap: MapLibreMap) {
        this.maplibreMap = maplibreMap
        maplibreMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPLIBRE_API_KEY}") {

            enableUserLocation();

            if (businessLatLng != null) {
                updateMapWithBusinessLocation()
            }

            maplibreMap.uiSettings.setAllGesturesEnabled(true)

        }

        maplibreMap.addOnMapClickListener { point ->
            if (isEditing) {
                businessLatLng = point
                maplibreMap.clear()
                maplibreMap.addMarker(MarkerOptions().position(point))
                return@addOnMapClickListener true
            }
            false
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

                if (isEditing && businessLatLng == null) {
                    businessLatLng = userLatLng
                    maplibreMap!!.clear()
                    maplibreMap!!.addMarker(MarkerOptions().position(userLatLng))
                }
            }
        } else {
            centerMapOnDefaultLocation()
        }
    }

    private fun centerMapOnDefaultLocation() {
        val defaultLocation = LatLng(32.0853, 34.7818) // Example (Tel Aviv)
        maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12.0))
    }

    override fun onStart() {
        super.onStart()
        binding.mapViewBusinessLocation.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapViewBusinessLocation.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapViewBusinessLocation.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapViewBusinessLocation.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapViewBusinessLocation.onDestroy()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapViewBusinessLocation.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapViewBusinessLocation.onLowMemory()
    }
}