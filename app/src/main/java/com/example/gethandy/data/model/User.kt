package com.example.gethandy.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val profilePicUrl: String,
    val businessId: String?
)

data class UserWithBusiness(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val business: Business?
)

data class UserWithReviews(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "reviewedId"
    )
    val reviews: List<Review>
)

data class UserReviewRelation(
    @Embedded val review: Review,
    @Relation(
        parentColumn = "reviewerId",
        entityColumn = "userId"
    )
    val reviewer: User,
    @Relation(
        parentColumn = "reviewedId",
        entityColumn = "userId"
    )
    val reviewedUser: User
)