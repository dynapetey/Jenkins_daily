package com.jenkinstowing.dailyloads.loadsheet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM extracted_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ExtractedRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ExtractedRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<ExtractedRecord>)

    @Delete
    suspend fun deleteRecord(record: ExtractedRecord)

    @Query("DELETE FROM extracted_records")
    suspend fun clearAll()
}
