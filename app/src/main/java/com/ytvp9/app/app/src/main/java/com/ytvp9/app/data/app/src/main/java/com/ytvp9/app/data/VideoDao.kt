package com.ytvp9.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity)

    @Update
    suspend fun update(video: VideoEntity)

    @Delete
    suspend fun delete(video: VideoEntity)

    @Query("SELECT * FROM videos ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE videoId = :videoId")
    suspend fun getById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' ORDER BY downloadedAt DESC")
    suspend fun search(query: String): List<VideoEntity>
}
