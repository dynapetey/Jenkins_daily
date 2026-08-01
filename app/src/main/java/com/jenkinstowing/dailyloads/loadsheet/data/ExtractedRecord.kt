package com.jenkinstowing.dailyloads.loadsheet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extracted_records")
data class ExtractedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val manifest: String,
    val dateHauled: String,
    val vehicleDetails: String,
    val vin: String,
    val origin: String,
    val destination: String,
    val forClient: String,
    val price: Double,
    val handWritten: String,
    val drivetrain: String,
    val epb: String,
    val timestamp: Long = System.currentTimeMillis()
)
