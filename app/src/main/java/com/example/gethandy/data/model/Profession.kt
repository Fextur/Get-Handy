package com.example.gethandy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "professions")
data class Profession(
    @PrimaryKey val name: String
)