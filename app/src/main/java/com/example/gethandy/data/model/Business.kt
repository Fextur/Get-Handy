package com.example.gethandy.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.maplibre.android.geometry.LatLng

@Entity(

    tableName = "businesses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Business(
    @PrimaryKey val businessId: String,
    val userId: String,
    val businessName: String,
    val description: String,
    val address: String,
    val profession: String,
    val location: LatLng,
    val geoHash: String
)

data class BusinessWithOwner(
    @Embedded val business: Business,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val owner: User
)
