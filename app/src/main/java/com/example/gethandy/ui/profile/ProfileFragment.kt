package com.example.gethandy.ui.profile

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import applyPhoneFormatting
import com.bumptech.glide.Glide
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentProfileBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.MapUtils
import com.example.gethandy.utils.MapUtils.bindMapLifecycle
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.ValidationUtil
import com.example.gethandy.utils.showSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
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

    private var isSaving = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        MapUtils.initializeMap(requireContext(), binding.mapViewBusinessLocation, savedInstanceState)
        binding.mapViewBusinessLocation.getMapAsync(this)

        bindMapLifecycle(binding.mapViewBusinessLocation)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = args.userId ?: UserManager.getUserId(requireContext())
        isCurrentUser = (userId == UserManager.getUserId(requireContext()))

        setupListeners()
        setupProfessionAutocomplete()
        observeViewModel()

        userId?.let {
            // This will trigger the getUserWithBusiness LiveData in observeViewModel()
            viewModel.getUserProfile(it)

            // If there's a business ID already known, refresh that too
            if (businessId != null) {
                viewModel.refreshBusinessData(businessId)
            }

            viewModel.refreshProfessions()
        }

        MapUtils.setupMapTouchHandler(binding.mapViewBusinessLocation)
    }

    override fun onMapReady(maplibreMap: MapLibreMap) {
        this.maplibreMap = maplibreMap

        MapUtils.setupMapStyle(maplibreMap) {
            MapUtils.enableUserLocation(maplibreMap, requireContext())

            businessLatLng?.let {
                updateMapWithBusinessLocation()
            }

            maplibreMap.uiSettings.setAllGesturesEnabled(true)
        }

        maplibreMap.addOnMapClickListener { point ->
            if (isEditing) {
                businessLatLng = point
                MapUtils.clearMap(maplibreMap)
                MapUtils.addMarker(maplibreMap, point)
                MapUtils.animateCamera(maplibreMap, point)
                true
            } else false
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                saveProfileChanges()
            } else {
                enableEditMode()
            }
        }

        binding.btnBookAppointment.setOnClickListener {
            val localBusinessId = businessId
            if (localBusinessId != null) {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToAppointmentBooking(localBusinessId)
                )
            } else {
                showSnackbar(binding.root, getString(R.string.error_unable_to_book), SnackbarType.ERROR)
            }
        }

        binding.radioGroupBusiness.setOnCheckedChangeListener { _, checkedId ->
            updateBusinessVisibility(checkedId == R.id.radioBusinessYes)

            if (checkedId == R.id.radioBusinessYes && businessLatLng == null && isEditing) {
                centerMapOnUserLocation()
            }
        }

        binding.ivProfilePic.setOnClickListener {
            if (isEditing) selectProfileImage()
        }

        binding.fabChangeProfilePic.setOnClickListener {
            if (isEditing) selectProfileImage()
        }

        binding.etUserPhone.applyPhoneFormatting()
    }

    private fun updateBusinessVisibility(isVisible: Boolean) {
        binding.cardBusinessInfo.visibility = if (isVisible) View.VISIBLE else View.GONE

        if (isVisible && isEditing) {
            binding.tvBusinessName.visibility = View.GONE
            binding.layoutBusinessName.visibility = View.VISIBLE
            binding.tvBusinessDescription.visibility = View.GONE
            binding.layoutBusinessDescription.visibility = View.VISIBLE
            binding.tvBusinessAddress.visibility = View.GONE
            binding.layoutBusinessAddress.visibility = View.VISIBLE
            binding.tvBusinessProfession.visibility = View.GONE
            binding.layoutBusinessProfession.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupProfessionAutocomplete() {

        val professionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ArrayList<String>()
        )
        binding.etBusinessProfession.setAdapter(professionAdapter)

        binding.etBusinessProfession.threshold = 0

        binding.etBusinessProfession.setOnClickListener {
            if (professionAdapter.count == 0) {
                viewModel.searchProfessions("", 15)
            }

            binding.etBusinessProfession.postDelayed({
                if (binding.etBusinessProfession.hasFocus()) {
                    binding.etBusinessProfession.showDropDown()
                }
            }, 100)
        }

        binding.etBusinessProfession.setOnTouchListener { v, _ ->
            val adapter = binding.etBusinessProfession.adapter
            if (adapter != null && adapter.count > 0) {
                v.performClick()
                binding.etBusinessProfession.showDropDown()
            }
            false
        }
        var searchJob: Job? = null
        binding.etBusinessProfession.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    delay(150)
                    val query = s?.toString() ?: ""
                    viewModel.searchProfessions(query, 15)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        viewModel.filteredProfessions.observe(viewLifecycleOwner) { professions ->
            val professionNames = professions.map { it.name }
            if (professionNames.isNotEmpty() || professionAdapter.isEmpty) {
                professionAdapter.clear()
                professionAdapter.addAll(professionNames)
                professionAdapter.notifyDataSetChanged()
                if (binding.etBusinessProfession.hasFocus() && professionNames.isNotEmpty()) {
                    binding.etBusinessProfession.post {
                        binding.etBusinessProfession.showDropDown()
                    }
                }
            }
        }
        binding.etBusinessProfession.setOnItemClickListener { _, _, _, _ ->
            binding.etBusinessProfession.requestFocus()
        }
        viewModel.refreshProfessions()
    }

    private fun observeViewModel() {
        userId?.let { uid ->
            LoadingUtil.showLoading(requireContext(), true)
            viewModel.getUserWithBusiness(uid).observe(viewLifecycleOwner) { userWithBusiness ->
                userWithBusiness?.let { uwb ->
                    if (!isSaving) {
                        val user = uwb.user

                        binding.tvUserName.text = user.fullName
                        binding.tvUserEmail.text = user.email
                        binding.tvUserPhone.text = user.phone

                        binding.etUserName.setText(user.fullName)
                        binding.etUserPhone.setText(user.phone)

                        if (profileImageUri == null) {
                            if (user.profilePicUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(user.profilePicUrl)
                                    .placeholder(R.drawable.loading_icon)
                                    .error(R.drawable.student_avatar)
                                    .into(binding.ivProfilePic)
                            } else {
                                binding.ivProfilePic.setImageResource(R.drawable.student_avatar)
                            }
                        }

                        businessId = user.businessId

                        val business = uwb.business
                        if (business != null) {
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
                        }

                        updateUIState()
                    }

                    if (!isSaving) {
                        LoadingUtil.showLoading(requireContext(), false)
                    }
                }
            }
        }

        viewModel.userProfileState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    if (!isSaving) LoadingUtil.showLoading(requireContext(), true)
                }
                is NetworkResult.Success -> {
                    if (!isSaving) LoadingUtil.showLoading(requireContext(), false)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    Log.e(TAG, "Error loading user profile: ${result.message}")
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }

        viewModel.profileUpdateState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    // Loading state handled by isSaving flag
                }
                is NetworkResult.Success -> {
                    isEditing = false
                    isSaving = false
                    binding.btnEditProfile.text = getString(R.string.edit_profile)

                    // No need to call getUserProfile explicitly as it's already
                    // handled in saveProfileChanges by calling userRepository.loadUser
                    // which triggers the LiveData observers

                    updateUIState()

                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, getString(R.string.profile_update_success), SnackbarType.SUCCESS)
                }
                is NetworkResult.Error -> {
                    isSaving = false
                    LoadingUtil.showLoading(requireContext(), false)
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }
    }

    private fun updateUIState() {
        binding.tvUserName.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.layoutUserName.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.tvUserPhone.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.layoutUserPhone.visibility = if (isEditing) View.VISIBLE else View.GONE

        binding.tvBusinessQuestion.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.radioGroupBusiness.visibility = if (isEditing) View.VISIBLE else View.GONE

        binding.fabChangeProfilePic.visibility = if (isEditing) View.VISIBLE else View.GONE

        if (isEditing) {
            binding.radioBusinessYes.isChecked = (businessId != null)
            binding.radioBusinessNo.isChecked = (businessId == null)
        }

        if (isEditing) {
            updateBusinessVisibility(binding.radioBusinessYes.isChecked)
        } else {
            binding.cardBusinessInfo.visibility = if (businessId != null) View.VISIBLE else View.GONE

            if (businessId != null) {
                binding.tvBusinessName.visibility = View.VISIBLE
                binding.layoutBusinessName.visibility = View.GONE
                binding.tvBusinessDescription.visibility = View.VISIBLE
                binding.layoutBusinessDescription.visibility = View.GONE
                binding.tvBusinessAddress.visibility = View.VISIBLE
                binding.layoutBusinessAddress.visibility = View.GONE
                binding.tvBusinessProfession.visibility = View.VISIBLE
                binding.layoutBusinessProfession.visibility = View.GONE
            }
        }

        updateButtons()

        if (!isEditing && businessLatLng != null) {
            updateMapWithBusinessLocation()
        } else if (isEditing && businessLatLng == null && binding.radioBusinessYes.isChecked) {
            centerMapOnUserLocation()
        }
    }

    private fun updateMapWithBusinessLocation() {
        maplibreMap?.let { map ->
            businessLatLng?.let { location ->
                MapUtils.clearMap(map)
                MapUtils.addMarker(map, location)
                MapUtils.animateCamera(map, location, 15.0)
            }
        }
    }

    private fun centerMapOnUserLocation() {
        maplibreMap?.let { map ->
            val userLocation = MapUtils.getUserLocation(map)
            if (userLocation != null) {
                businessLatLng = userLocation
                MapUtils.clearMap(map)
                MapUtils.addMarker(map, userLocation)
                MapUtils.animateCamera(map, userLocation, 15.0)
            } else {
                centerMapOnDefaultLocation()
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

        updateUIState()

        if (binding.radioBusinessYes.isChecked && businessLatLng == null) {
            centerMapOnUserLocation()
        }
    }

    private fun centerMapOnDefaultLocation() {
        val defaultLocation = LatLng(32.0853, 34.7818) // Example (Tel Aviv)
        maplibreMap?.let { map ->
            MapUtils.animateCamera(map, defaultLocation, 12.0)
        }
    }

    private fun isProfileValid(): Boolean {
        val fullName = binding.etUserName.text.toString().trim()
        val phone = binding.etUserPhone.text.toString().trim()

        if (!ValidationUtil.isValidName(fullName)) {
            binding.layoutUserName.error = getString(R.string.error_full_name_required)
            return false
        } else {
            binding.layoutUserName.error = null
        }

        if (!ValidationUtil.isValidPhoneNumber(phone)) {
            binding.layoutUserPhone.error = getString(R.string.error_invalid_phone)
            return false
        } else {
            binding.layoutUserPhone.error = null
        }

        if (binding.radioBusinessYes.isChecked) {
            val businessName = binding.etBusinessName.text.toString().trim()
            val address = binding.etBusinessAddress.text.toString().trim()
            val profession = binding.etBusinessProfession.text.toString().trim()

            if (!ValidationUtil.isValidBusinessName(businessName)) {
                binding.layoutBusinessName.error = getString(R.string.error_business_name_required)
                return false
            } else {
                binding.layoutBusinessName.error = null
            }

            if (!ValidationUtil.isValidAddress(address)) {
                binding.layoutBusinessAddress.error = getString(R.string.error_business_address_required)
                return false
            } else {
                binding.layoutBusinessAddress.error = null
            }

            if (!isProfessionValid(profession)) {
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

    private fun isProfessionValid(professionName: String): Boolean {
        return viewModel.filteredProfessions.value?.any {
            it.name.equals(professionName, ignoreCase = true)
        } == true
    }

    private fun saveProfileChanges() {
        if (!isProfileValid()) return

        isSaving = true
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

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                profileImageUri = it
                Glide.with(requireContext())
                    .load(uri)
                    .placeholder(R.drawable.loading_icon)
                    .error(R.drawable.student_avatar)
                    .into(binding.ivProfilePic)
            }
        }

    private fun selectProfileImage() {
        imagePickerLauncher.launch("image/*")
    }

    override fun onResume() {
        super.onResume()

        if (!isEditing && !isSaving) {
            userId?.let {
                viewModel.getUserProfile(it)
                // If there's a business ID, refresh that too
                businessId?.let { bid ->
                    viewModel.refreshBusinessData(bid)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}