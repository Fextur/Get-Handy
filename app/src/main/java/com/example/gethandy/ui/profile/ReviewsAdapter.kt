package com.example.gethandy.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gethandy.R
import com.example.gethandy.data.model.Review
import com.example.gethandy.utils.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter(
    private val profileUserId: String,
    private val onReviewClick: (String) -> Unit,
    private val onEditReviewClick: (String, String) -> Unit
) : PagingDataAdapter<Review, ReviewsAdapter.ReviewViewHolder>(REVIEW_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = getItem(position)
        if (review != null) {
            holder.bind(review)
        }
    }

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReviewerId: MaterialTextView = itemView.findViewById(R.id.tvReviewerId)
        private val tvReviewDate: MaterialTextView = itemView.findViewById(R.id.tvReviewDate)
        private val tvReviewContent: MaterialTextView = itemView.findViewById(R.id.tvReviewContent)
        private val ivReviewImage: ShapeableImageView = itemView.findViewById(R.id.ivReviewImage)
        private val btnEditReview: MaterialButton = itemView.findViewById(R.id.btnEditReview)

        fun bind(review: Review) {
            val context = itemView.context
            val currentUserId = UserManager.getUserId(context)

            // Is this review by me or about me?
            val isReviewByMe = review.reviewerId == profileUserId
            val isReviewAboutMe = review.reviewedId == profileUserId

            // Set text based on whether it's a review by me or about me
            val displayText = if (isReviewByMe) {
                context.getString(R.string.my_review_to, review.reviewedId.take(5))
            } else {
                context.getString(R.string.review_from, review.reviewerId.take(5))
            }

            tvReviewerId.text = displayText
            tvReviewDate.text = formatDate(review.date)
            tvReviewContent.text = review.content

            if (review.imageUrl.isNullOrEmpty()) {
                ivReviewImage.visibility = View.GONE
            } else {
                ivReviewImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(review.imageUrl)
                    .placeholder(R.drawable.loading_icon)
                    .error(R.drawable.student_avatar)
                    .into(ivReviewImage)
            }

            // Only show edit button for my reviews
            btnEditReview.visibility = if (review.reviewerId == currentUserId) View.VISIBLE else View.GONE

            btnEditReview.setOnClickListener {
                onEditReviewClick(review.reviewedId, review.reviewId)
            }

            itemView.setOnClickListener {
                // Navigate to the other user's profile
                val otherUserId = if (isReviewByMe) {
                    review.reviewedId
                } else {
                    review.reviewerId
                }
                onReviewClick(otherUserId)
            }
        }

        private fun formatDate(dateString: String): String {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                return if (date != null) outputFormat.format(date) else dateString
            } catch (e: Exception) {
                return dateString
            }
        }
    }

    companion object {
        private val REVIEW_COMPARATOR = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem.reviewId == newItem.reviewId
            }

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem.content == newItem.content &&
                        oldItem.date == newItem.date &&
                        oldItem.imageUrl == newItem.imageUrl
            }
        }
    }
}