package com.example.gethandy.ui.review

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LeaveReviewFragment : Fragment() {
    private var _binding: FragmentLeaveReviewBinding? = null
    private val binding get() = _binding!!

    private val args: LeaveReviewFragmentArgs by navArgs()
    private var reviewImageUri: Uri? = null

    private val viewModel: ReviewViewModel by viewModels()

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
        binding.tvReviewTitle.text = getString(R.string.leave_review_for_user, reviewedUserId.take(5)) // TODO: load his name instead of id

        setupImagePicker()
        setupSubmitButton(reviewedUserId)
        observeReviewSubmission()
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

    private fun setupSubmitButton(reviewedUserId: String) {
        binding.btnSubmitReview.setOnClickListener {
            val reviewContent = binding.etReviewContent.text.toString().trim()

            if (reviewContent.isEmpty()) {
                Snackbar.make(binding.root, R.string.error_empty_review, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId.isNullOrEmpty()) {
                Snackbar.make(binding.root, R.string.error_not_logged_in, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reviewId = args.reviewId;
            LoadingUtil.showLoading(requireContext(), true)

            viewModel.submitReview(
                reviewId = reviewId,
                reviewerId = currentUserId,
                reviewedId = reviewedUserId,
                content = reviewContent,
                imageUri = reviewImageUri
            )
        }
    }

    private fun observeReviewSubmission() {
        viewModel.reviewSubmissionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    LoadingUtil.showLoading(requireContext(), false)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.review_submitted_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToProfile(args.reviewedUserId)
                }
                is NetworkResult.Error -> {
                    LoadingUtil.showLoading(requireContext(), false)

                    Snackbar.make(
                        binding.root,
                        result.message ?: getString(R.string.error_creating_review),
                        Snackbar.LENGTH_LONG
                    ).show()
                }else -> {}
            }
        }
    }

    private fun navigateToProfile(userId: String) {
        try {
            findNavController().navigate(LeaveReviewFragmentDirections.actionReviewToProfile(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to profile: ${e.message}")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}