package com.ytvp9.app

import androidx.room.*

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :q || '%' ORDER BY downloadedAt DESC")
    suspend fun search(q: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE videoId = :id")
    suspend fun getById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(v: VideoEntity)

    @Query("UPDATE videos SET resumePositionMs = :pos, lastPlayedAt = :t WHERE videoId = :id")
    suspend fun updateResume(id: String, pos: Long, t: Long)

    @Query("DELETE FROM videos WHERE videoId = :id")
    suspend fun delete(id: String)

    @Query("SELECT SUM(fileSize) FROM videos")
    suspend fun totalSize(): Long?
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<QueueEntity>

    @Query("SELECT * FROM queue WHERE status IN ('QUEUED','DOWNLOADING','PAUSED') ORDER BY createdAt ASC")
    suspend fun getActive(): List<QueueEntity>

    @Insert
    suspend fun insert(q: QueueEntity): Long

    @Query("UPDATE queue SET status = :s, progress = :p WHERE id = :id")
    suspend fun updateProgress(id: Long, s: String, p: Int)

    @Query("UPDATE queue SET status = :s, errorKind = :ek, errorDetail = :ed WHERE id = :id")
    suspend fun markFailed(id: Long, s: String, ek: String?, ed: String?)

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM queue WHERE videoId = :vid AND status IN ('QUEUED','DOWNLOADING','PAUSED')")
    suspend fun countActiveForVideo(vid: String): Int
}

@Dao
interface BlockStateDao {
    @Query("SELECT * FROM block_state WHERE id = 1")
    suspend fun get(): BlockStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(s: BlockStateEntity)

    @Query("DELETE FROM block_state")
    suspend fun clear()
}

@Database(entities = [VideoEntity::class, QueueEntity::class, BlockStateEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun queueDao(): QueueDao
    abstract fun blockStateDao(): BlockStateDao
}
