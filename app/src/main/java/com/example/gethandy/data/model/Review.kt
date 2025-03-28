package com.example.gethandy.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

// Review entity with proper foreign key references
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["reviewerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["reviewedId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Review(
    @PrimaryKey
    val reviewId: String,

    @ColumnInfo(index = true)
    val reviewerId: String,

    @ColumnInfo(index = true)
    val reviewedId: String,

    val content: String,
    val date: String,
    val imageUrl: String? = null
)

// Class to represent a review with related user information
//data class ReviewWithUsers(
//    @Relation(
//        parentColumn = "reviewId",
//        entityColumn = "userId",
//        associateBy = Junction(
//            value = Review::class,
//            parentColumn = "reviewId",
//            entityColumn = "reviewerId"
//        )
//    )
//    val reviewer: User,
//
//    @Relation(
//        parentColumn = "reviewId",
//        entityColumn = "userId",
//        associateBy = Junction(
//            value = Review::class,
//            parentColumn = "reviewId",
//            entityColumn = "reviewedId"
//        )
//    )
//    val reviewedUser: User,
//
//)