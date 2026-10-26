package com.mealory.app.data.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MealWithDetails(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId",
    )
    val photos: List<MealPhotoEntity>,
    @Relation(
        parentColumn = "placeId",
        entityColumn = "id",
    )
    val place: PlaceEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealPersonCrossRef::class,
            parentColumn = "mealId",
            entityColumn = "personId",
        ),
    )
    val people: List<PersonEntity>,
)
