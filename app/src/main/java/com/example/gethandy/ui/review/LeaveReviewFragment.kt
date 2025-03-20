package com.example.gethandy.ui.review

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.gethandy.R
import com.example.gethandy.TAG
import com.example.gethandy.databinding.FragmentLeaveReviewBinding

class LeaveReviewFragment : Fragment() {
    private var _binding: FragmentLeaveReviewBinding? = null
    private val binding get() = _binding!!

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
        binding.tvReviewTitle.text = getString(R.string.leave_review_for_user, reviewedUserId.take(5)) // TODO: load his name instead of id

        setupImagePicker()
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