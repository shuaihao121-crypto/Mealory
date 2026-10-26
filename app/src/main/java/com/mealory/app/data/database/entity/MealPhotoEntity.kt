package com.mealory.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_photos",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealId")],
)
data class MealPhotoEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val relativePath: String,
    val sortOrder: Int,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String,
    val originalTakenAt: Long?,
    val createdAt: Long,
)
