package com.jenkinstowing.dailyloads

data class Load(
    val rowNumber: Int,
    val vehicleDetails: String,
    val vin: String,
    val origin: String,
    val destination: String,
    val notes: String,
    val drivetrain: String,
    val epb: String,
    val completed: Boolean,
)

data class DailySheet(
    val spreadsheetId: String,
    val completedColumn: Int,
    val loads: List<Load>,
)
