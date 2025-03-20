package com.example.gethandy.data.model

data class Review(
    val reviewId: String,
    val reviewerId: String,
    val reviewedId: String,
    val content: String,
    val date: String,
    val imageUrl: String?
)