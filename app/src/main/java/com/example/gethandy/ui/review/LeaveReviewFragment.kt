package com.example.gethandy.ui.review

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.gethandy.R
import com.example.gethandy.TAG

/**
 * Skeleton Review Fragment - will be implemented in future
 */
class LeaveReviewFragment : Fragment() {
    private val args: LeaveReviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leave_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reviewedUserId = args.reviewedUserId
        Log.d(TAG, "ReviewFragment opened for user: $reviewedUserId")

        view.findViewById<TextView>(R.id.tvReviewTitle).text =
            "Leave a Review (User ID: ${reviewedUserId.take(5)}...)"
    }
}