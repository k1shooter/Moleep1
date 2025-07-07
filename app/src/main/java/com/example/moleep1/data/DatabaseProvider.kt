//package com.example.moleep1.data
//
//import android.content.Context
//import androidx.room.Room
//import androidx.room.databaseBuilder
//
//object DatabaseProvider {
//    @Volatile private var INSTANCE: AppDatabase? = null
//
//    fun getDatabase(context: Context): AppDatabase {
//        return INSTANCE ?: synchronized(this) {
//            val instance = Room.databaseBuilder(
//                "my_database",
//                context.applicationContext,
//                AppDatabase::class.java
//            ).build()
//            INSTANCE = instance
//            instance
//        }
//    }
//}