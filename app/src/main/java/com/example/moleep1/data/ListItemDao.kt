//package com.example.moleep1.data
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//
//@Dao
//interface ListItemDao {
//    @Query("SELECT * FROM list_items")
//    suspend fun getAll(): List<ListItemEntity>
//
//    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
//    suspend fun insert(item: ListItemEntity)
//
//    @Delete
//    suspend fun delete(item: ListItemEntity)
//}