package com.jenkinstowing.dailyloads.loadsheet.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.jenkinstowing.dailyloads.loadsheet.data.ExtractedRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun saveDailyLoadsToDocuments(context: Context, records: List<ExtractedRecord>): Pair<String, Uri?> {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val filename = "${dateStr}_LOAD Sheet.csv"
        
        val csvHeader = "Manifest #,Vehicle Details,VIN,Origin,Destination,Hand Written Notes,Drivetrain,EPB,Price,Date Hauled,For Client\n"
        val csvBody = StringBuilder().apply {
            append(csvHeader)
            records.forEach { r ->
                val manifestClean = r.manifest.replace("\"", "\"\"")
                val vehicleClean = r.vehicleDetails.replace("\"", "\"\"")
                val vinClean = r.vin.replace("\"", "\"\"")
                val originClean = r.origin.replace("\"", "\"\"")
                val destClean = r.destination.replace("\"", "\"\"")
                val notesClean = r.handWritten.replace("\"", "\"\"")
                val drivetrainClean = r.drivetrain.replace("\"", "\"\"")
                val epbClean = r.epb.replace("\"", "\"\"")
                val priceClean = r.price.toString()
                val dateHauledClean = r.dateHauled.replace("\"", "\"\"")
                val forClientClean = r.forClient.replace("\"", "\"\"")
                
                append("\"$manifestClean\",\"$vehicleClean\",\"$vinClean\",\"$originClean\",\"$destClean\",\"$notesClean\",\"$drivetrainClean\",\"$epbClean\",\"$priceClean\",\"$dateHauledClean\",\"$forClientClean\"\n")
            }
        }.toString()

        val fullRelativePath = Environment.DIRECTORY_DOCUMENTS + "/DailyLoads"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, fullRelativePath)
            }
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(csvBody)
                        }
                    }
                    return Pair(filename, uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return Pair(filename, null)
        } else {
            try {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DailyLoads")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvBody)
                    }
                }
                return Pair(filename, Uri.fromFile(file))
            } catch (e: Exception) {
                e.printStackTrace()
                return Pair(filename, null)
            }
        }
    }
}
