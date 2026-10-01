package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.TrackEntity
import com.example.data.model.TrackListType
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks WHERE listType = :type ORDER BY timestamp DESC")
    fun getTracksByType(type: TrackListType): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE listType = 'QUEUE' ORDER BY queueOrder ASC")
    fun getQueueTracks(): Flow<List<TrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM tracks WHERE id = :id AND listType = 'LIKED')")
    fun isLiked(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id = :id AND listType = :type")
    suspend fun deleteTrack(id: String, type: TrackListType)

    @Query("DELETE FROM tracks WHERE listType = :type")
    suspend fun clearTracksByType(type: TrackListType)

    @Transaction
    suspend fun replaceQueue(tracks: List<TrackEntity>) {
        clearTracksByType(TrackListType.QUEUE)
        insertTracks(tracks)
    }
}
