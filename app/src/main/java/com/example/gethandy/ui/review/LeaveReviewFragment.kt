package com.example.gethandy.ui.review

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentLeaveReviewBinding
import com.example.gethandy.utils.LoadingUtil
import com.example.gethandy.utils.NetworkResult
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.showSnackbar

class LeaveReviewFragment : Fragment() {
    private var _binding: FragmentLeaveReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels()
    private val args: LeaveReviewFragmentArgs by navArgs()
    private var reviewImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaveReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reviewedUserId = args.reviewedUserId

        // Load user name if available
        loadReviewedUserName(reviewedUserId)

        setupImagePicker()
        setupObservers()
        setupSubmitButton(reviewedUserId)
    }

    private fun loadReviewedUserName(userId: String) {
        // Set temporary title with user ID
        binding.tvReviewTitle.text = getString(R.string.leave_review_for_user, userId.take(5))

        // TODO: Use UserRepository to get the user's name and update the title
        // This requires adding code to fetch the user's name from the database
    }

    private fun setupObservers() {
        viewModel.reviewState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    LoadingUtil.showLoading(requireContext(), true)
                    binding.btnSubmitReview.isEnabled = false
                }
                is NetworkResult.Success -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    binding.btnSubmitReview.isEnabled = true
                    showSnackbar(binding.root, getString(R.string.review_submitted_successfully), SnackbarType.SUCCESS)

                    // Refresh reviews for the user
                    result.data.reviewedId?.let { viewModel.refreshReviews(it) }

                    // Navigate back
                    findNavController().navigateUp()
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)
                    binding.btnSubmitReview.isEnabled = true
                    showSnackbar(binding.root, result.message, SnackbarType.ERROR)
                }
            }
        }
    }

    private fun setupSubmitButton(reviewedUserId: String) {
        binding.btnSubmitReview.setOnClickListener {
            val reviewContent = binding.etReviewContent.text.toString().trim()

            if (reviewContent.isEmpty()) {
                binding.etReviewContent.error = getString(R.string.review_content_required)
                return@setOnClickListener
            }

            val currentUserId = UserManager.getUserId(requireContext())

            if (currentUserId != null) {
                // Disable button to prevent multiple submissions
                binding.btnSubmitReview.isEnabled = false

                viewModel.createReview(
                    reviewerId = currentUserId,
                    reviewedId = reviewedUserId,
                    content = reviewContent,
                    imageUri = reviewImageUri
                )
            } else {
                // Handle case where user is not logged in
                showSnackbar(binding.root, getString(R.string.error_invalid_users), SnackbarType.ERROR)
            }
        }
    }

    private fun setupImagePicker() {
        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                reviewImageUri = it
                Glide.with(requireContext())
                    .load(uri)
                    .placeholder(R.drawable.loading_icon)
                    .into(binding.ivReviewImage)

                binding.ivCameraIcon.visibility = View.GONE
            }
        }

        binding.cardReviewImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.ivReviewImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.ivCameraIcon.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}