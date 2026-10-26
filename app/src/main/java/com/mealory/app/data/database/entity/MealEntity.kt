package com.mealory.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("dateEpochDay"),
        Index("deletedAt"),
        Index("placeId"),
        Index(value = ["deletedAt", "dateEpochDay", "minuteOfDay"]),
    ],
)
data class MealEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val minuteOfDay: Int,
    val mealType: MealType,
    val title: String?,
    val note: String?,
    val placeId: String?,
    val coverPhotoId: String?,
    val timeZoneId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
