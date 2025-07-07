//package com.example.moleep1.data
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//
//@Dao
//interface PlacedImageDao {
//    @Query("SELECT * FROM placed_images")
//    suspend fun getAll(): List<PlacedImageEntity>
//
//    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
//    suspend fun insert(item: PlacedImageEntity)
//
//    @Delete
//    suspend fun delete(item: PlacedImageEntity)
//}