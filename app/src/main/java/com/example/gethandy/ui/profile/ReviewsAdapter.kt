package com.example.gethandy.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gethandy.R
import com.example.gethandy.data.model.Review
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter(
    private val profileUserId: String,
    private val onReviewClick: (String) -> Unit
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    private var reviews: List<Review> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        val diffCallback = ReviewDiffCallback(reviews, newReviews)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        reviews = newReviews
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReviewerId: MaterialTextView = itemView.findViewById(R.id.tvReviewerId)
        private val tvReviewDate: MaterialTextView = itemView.findViewById(R.id.tvReviewDate)
        private val tvReviewContent: MaterialTextView = itemView.findViewById(R.id.tvReviewContent)
        private val ivReviewImage: ShapeableImageView = itemView.findViewById(R.id.ivReviewImage)

        fun bind(review: Review) {
            val context = itemView.context

            val isReviewOfThisProfile = profileUserId == review.reviewedId

            val displayId = if (isReviewOfThisProfile) {
                context.getString(R.string.reviewer_user_id, review.reviewerId.take(5))
            } else {
                context.getString(R.string.reviewed_user_id, review.reviewedId.take(5))
            }

            tvReviewerId.text = displayId
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

            itemView.setOnClickListener {
                val otherUserId = if (isReviewOfThisProfile) {
                    review.reviewerId
                } else {
                    review.reviewedId
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

    private class ReviewDiffCallback(
        private val oldList: List<Review>,
        private val newList: List<Review>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].reviewId == newList[newItemPosition].reviewId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.content == newItem.content &&
                    oldItem.date == newItem.date &&
                    oldItem.imageUrl == newItem.imageUrl
        }
    }
}