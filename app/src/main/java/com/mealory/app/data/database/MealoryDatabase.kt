package com.mealory.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mealory.app.data.database.dao.MealDao
import com.mealory.app.data.database.dao.BackupDao
import com.mealory.app.data.database.dao.MealPhotoDao
import com.mealory.app.data.database.dao.PersonDao
import com.mealory.app.data.database.dao.PlaceDao
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity

@Database(
    entities = [
        MealEntity::class,
        MealPhotoEntity::class,
        PersonEntity::class,
        PlaceEntity::class,
        MealPersonCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MealoryConverters::class)
abstract class MealoryDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun mealPhotoDao(): MealPhotoDao
    abstract fun personDao(): PersonDao
    abstract fun placeDao(): PlaceDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val DATABASE_NAME = "mealory.db"

        fun build(context: Context): MealoryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MealoryDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
