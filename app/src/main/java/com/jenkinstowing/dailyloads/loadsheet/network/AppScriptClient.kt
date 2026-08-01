package com.jenkinstowing.dailyloads.loadsheet.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppScriptClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true) // Crucial for Apps Script redirects!
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun syncToJenkinsPaysheet(
        url: String,
        manifest: String,
        dateHauled: String,
        vehicleDetails: String,
        vin: String,
        origin: String,
        destination: String,
        forClient: String,
        price: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Apps Script Web App URL is empty!"))
        }

        val payload = mapOf(
            "action" to "insert_paysheet_row",
            "data" to mapOf(
                "manifest" to manifest,
                "dateHauled" to dateHauled,
                "vehicleDetails" to vehicleDetails,
                "vin" to vin,
                "origin" to origin,
                "destination" to destination,
                "forClient" to forClient,
                "price" to price
            )
        )

        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(type)
        val jsonString = adapter.toJson(payload)

        val request = Request.Builder()
            .url(url)
            .post(jsonString.toRequestBody(jsonMediaType))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("Apps Script error (${response.code}): $body"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createDailyLoadsSheet(
        url: String,
        filename: String,
        templateName: String,
        rows: List<Map<String, Any>>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Apps Script Web App URL is empty!"))
        }

        val payload = mapOf(
            "action" to "create_daily_loads_sheet",
            "filename" to filename,
            "templateName" to templateName,
            "rows" to rows
        )

        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(type)
        val jsonString = adapter.toJson(payload)

        val request = Request.Builder()
            .url(url)
            .post(jsonString.toRequestBody(jsonMediaType))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("Apps Script error (${response.code}): $body"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
