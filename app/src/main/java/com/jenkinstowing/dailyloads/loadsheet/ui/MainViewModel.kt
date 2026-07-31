package com.jenkinstowing.dailyloads.loadsheet.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jenkinstowing.dailyloads.BuildConfig
import com.jenkinstowing.dailyloads.loadsheet.data.AppDatabase
import com.jenkinstowing.dailyloads.loadsheet.data.ExtractedRecord
import com.jenkinstowing.dailyloads.loadsheet.network.AppScriptClient
import com.jenkinstowing.dailyloads.loadsheet.network.GeminiClient
import com.jenkinstowing.dailyloads.loadsheet.network.NhtsaClient
import com.jenkinstowing.dailyloads.loadsheet.util.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val recordDao = database.recordDao()

    // Preferences for Google Sheets integration
    private val prefs = context.getSharedPreferences("load_sheet_prefs", Context.MODE_PRIVATE)

    // Flow of historical records
    val historicalRecords: StateFlow<List<ExtractedRecord>> = recordDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow("")
    val processingProgress: StateFlow<String> = _processingProgress.asStateFlow()

    private val _currentBatch = MutableStateFlow<List<ExtractedRecord>>(emptyList())
    val currentBatch: StateFlow<List<ExtractedRecord>> = _currentBatch.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _appsScriptUrl = MutableStateFlow(prefs.getString("apps_script_url", "") ?: "")
    val appsScriptUrl: StateFlow<String> = _appsScriptUrl.asStateFlow()

    private val _dailyLoadsTemplate = MutableStateFlow(prefs.getString("daily_loads_template", "Dailyloads.template") ?: "Dailyloads.template")
    val dailyLoadsTemplate: StateFlow<String> = _dailyLoadsTemplate.asStateFlow()

    fun updateAppsScriptUrl(url: String) {
        _appsScriptUrl.value = url
        prefs.edit().putString("apps_script_url", url).apply()
    }

    fun updateDailyLoadsTemplate(template: String) {
        _dailyLoadsTemplate.value = template
        prefs.edit().putString("daily_loads_template", template).apply()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun updateRecordInBatch(index: Int, updated: ExtractedRecord) {
        val list = _currentBatch.value.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _currentBatch.value = list
        }
    }

    fun deleteRecordFromBatch(index: Int) {
        val list = _currentBatch.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _currentBatch.value = list
        }
    }

    fun clearBatch() {
        _currentBatch.value = emptyList()
    }

    fun processSelectedPdfs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            _currentBatch.value = emptyList()

            try {
                _processingProgress.value = "Extracting and rendering pages from ${uris.size} PDF(s)..."
                val bitmaps = mutableListOf<Bitmap>()
                
                withContext(Dispatchers.IO) {
                    uris.forEachIndexed { pdfIndex, uri ->
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            val pdfRenderer = PdfRenderer(pfd)
                            val pageCount = pdfRenderer.pageCount
                            for (i in 0 until pageCount) {
                                _processingProgress.value = "Rendering PDF ${pdfIndex + 1}/${uris.size}, Page ${i + 1}/$pageCount..."
                                val page = pdfRenderer.openPage(i)
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                
                                // Scale down to max 1200px to maintain high resolution but avoid payload limits
                                val scaledBitmap = resizeBitmap(bitmap)
                                bitmaps.add(scaledBitmap)
                                page.close()
                            }
                            pdfRenderer.close()
                            pfd.close()
                        }
                    }
                }

                if (bitmaps.isEmpty()) {
                    throw IllegalStateException("No pages could be rendered from the selected files.")
                }

                _processingProgress.value = "Running AI OCR extraction via Gemini... This can take a few moments."
                
                // Fetch key from BuildConfig
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalArgumentException("Gemini API Key is placeholder or missing. Please set it in the AI Studio Secrets panel.")
                }

                val geminiRecords = withContext(Dispatchers.IO) {
                    GeminiClient.extractFromPages(bitmaps, apiKey)
                }

                _processingProgress.value = "OCR extraction completed. Querying NHTSA database for ${geminiRecords.size} vehicle(s)..."

                val parsedRecords = mutableListOf<ExtractedRecord>()
                geminiRecords.forEachIndexed { index, geminiRec ->
                    val vin = geminiRec.vin ?: ""
                    _processingProgress.value = "Decoding VIN details with NHTSA (${index + 1}/${geminiRecords.size}): $vin"
                    
                    val (drivetrain, epb) = if (vin.length == 17) {
                        NhtsaClient.fetchVinDetails(vin)
                    } else {
                        Pair("Front wheel drive", "No") // Fallback if VIN is malformed
                    }

                    // Enforce the handwritten price rule: if there are handwritten notes containing a dollar amount
                    var finalPrice = geminiRec.price ?: 0.0
                    val handWrittenNotes = geminiRec.handWritten ?: ""
                    
                    val dollarRegex = """\$(\d+(?:\.\d{2})?)""".toRegex()
                    val matchResult = dollarRegex.find(handWrittenNotes)
                    if (matchResult != null) {
                        val parsedDollarAmount = matchResult.groupValues[1].toDoubleOrNull()
                        if (parsedDollarAmount != null) {
                            finalPrice = parsedDollarAmount
                        }
                    }

                    parsedRecords.add(
                        ExtractedRecord(
                            manifest = geminiRec.manifest ?: "",
                            dateHauled = geminiRec.dateHauled ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            vehicleDetails = geminiRec.vehicleDetails ?: "",
                            vin = vin,
                            origin = geminiRec.origin ?: "",
                            destination = geminiRec.destination ?: "",
                            forClient = geminiRec.forClient ?: "",
                            price = finalPrice,
                            handWritten = handWrittenNotes,
                            drivetrain = drivetrain,
                            epb = epb
                        )
                    )
                }

                _currentBatch.value = parsedRecords
                _processingProgress.value = "Success! Verified data loaded."
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage ?: "An error occurred during extraction."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun applyAndSyncBatch(onSuccess: () -> Unit = {}) {
        val batch = _currentBatch.value
        if (batch.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _syncStatus.value = "Saving loaded sheets to local database and CSV documents..."
            _errorMessage.value = null

            try {
                // 1. Save all to Local Room Database
                withContext(Dispatchers.IO) {
                    recordDao.insertRecords(batch)
                }

                // 2. Export local CSV Copy to Documents/DailyLoads folder
                val (filename, uri) = withContext(Dispatchers.IO) {
                    CsvExporter.saveDailyLoadsToDocuments(context, batch)
                }

                var appScriptResultDetails = ""

                // 3. Sync to Google Sheets via AppScript Web App if URL is provided
                val url = _appsScriptUrl.value
                if (url.isNotBlank() && url.startsWith("https://")) {
                    _syncStatus.value = "Uploading and syncing with Google Sheets backend..."

                    // Sync paysheet rows
                    var paysheetCount = 0
                    batch.forEach { item ->
                        val pResult = AppScriptClient.syncToJenkinsPaysheet(
                            url = url,
                            manifest = item.manifest,
                            dateHauled = item.dateHauled,
                            vehicleDetails = item.vehicleDetails,
                            vin = item.vin,
                            origin = item.origin,
                            destination = item.destination,
                            forClient = item.forClient,
                            price = item.price
                        )
                        if (pResult.isSuccess) paysheetCount++
                    }

                    // Create the Daily Loads Sheet
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val dailyLoadsFilename = "${dateStr}_LOAD Sheet"
                    
                    val dailyLoadsRows = batch.map { item ->
                        mapOf(
                            "manifest" to item.manifest,
                            "vehicleDetails" to item.vehicleDetails,
                            "vin" to item.vin,
                            "origin" to item.origin,
                            "destination" to item.destination,
                            "handWritten" to item.handWritten,
                            "drivetrain" to item.drivetrain,
                            "epb" to item.epb
                        )
                    }

                    val dlResult = AppScriptClient.createDailyLoadsSheet(
                        url = url,
                        filename = dailyLoadsFilename,
                        templateName = _dailyLoadsTemplate.value,
                        rows = dailyLoadsRows
                    )

                    appScriptResultDetails = if (dlResult.isSuccess) {
                        "\n✓ Synchronized $paysheetCount rows to Jenkins_Paysheet\n✓ Created new Google Sheet \"$dailyLoadsFilename\" successfully!"
                    } else {
                        "\n✓ Synchronized $paysheetCount rows to Jenkins_Paysheet\n⚠ Daily Load Creation failed: ${dlResult.exceptionOrNull()?.message}"
                    }
                } else {
                    appScriptResultDetails = "\nℹ No active Apps Script Web App configured. Synced locally only."
                }

                val savedMessage = if (uri != null) {
                    "Successfully processed ${batch.size} load(s)!\nSaved locally as \"$filename\" in your Documents folder.$appScriptResultDetails"
                } else {
                    "Successfully processed ${batch.size} load(s)!\nSaved locally in database logs.$appScriptResultDetails"
                }

                _syncStatus.value = savedMessage
                _currentBatch.value = emptyList() // Batch verified and cleared
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Sync Error: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1200): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                recordDao.clearAll()
            }
        }
    }
}
