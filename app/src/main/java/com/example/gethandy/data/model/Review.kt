package com.example.gethandy.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
