//package com.example.moleep1.data
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import com.example.moleep1.data.ListItemEntity
//import com.example.moleep1.data.PlacedImageEntity
//
//@Database(entities = [ListItemEntity::class, PlacedImageEntity::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun listItemDao(): ListItemDao
//    abstract fun placedImageDao(): PlacedImageDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "my-database"
//                ).build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
