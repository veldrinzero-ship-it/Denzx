package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnbanRequestDao {
    @Query("SELECT * FROM unban_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<UnbanRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: UnbanRequest): Long

    @Update
    suspend fun updateRequest(request: UnbanRequest)

    @Delete
    suspend fun deleteRequest(request: UnbanRequest)

    @Query("DELETE FROM unban_requests WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM unban_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: Int): UnbanRequest?
}
