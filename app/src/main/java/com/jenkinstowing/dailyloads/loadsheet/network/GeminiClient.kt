package com.jenkinstowing.dailyloads.loadsheet.network

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class ExtractedBatch(
    @Json(name = "records") val records: List<GeminiExtractedRecord>
)

@JsonClass(generateAdapter = true)
data class GeminiExtractedRecord(
    @Json(name = "manifest") val manifest: String?,
    @Json(name = "date_hauled") val dateHauled: String?,
    @Json(name = "vehicle_details") val vehicleDetails: String?,
    @Json(name = "vin") val vin: String?,
    @Json(name = "origin") val origin: String?,
    @Json(name = "destination") val destination: String?,
    @Json(name = "for_client") val forClient: String?,
    @Json(name = "price") val price: Double?,
    @Json(name = "hand_written") val handWritten: String?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun extractFromPages(bitmaps: List<Bitmap>, apiKey: String): List<GeminiExtractedRecord> {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API Key is missing. Configure it in the Secrets panel in AI Studio.")
        }

        val parts = mutableListOf<GeminiPart>()
        
        // Build the extraction prompt
        val prompt = """
            You are an expert OCR parser for hauling and shipping sheets. 
            Analyze the attached images of load sheets/paysheets and extract structured information for each individual load.
            
            Extract the following fields for each record:
            1. manifest (This represents the load id or manifest number)
            2. date_hauled (Date the hauling was performed, formatted as YYYY-MM-DD if possible)
            3. vehicle_details (Details of the truck, trailer, make/model, fleet number, etc.)
            4. vin (The 17-character VIN number, clean it of spaces)
            5. origin (Starting location or yard name)
            6. destination (Dropoff location or customer yard)
            7. for_client (Who the load is for or client name)
            8. price (The numerical price of the load. Look for rates, total, net pay)
            9. hand_written (Any handwritten notes, remarks, instructions, or scribbles on the sheet)
            
            CRITICAL HANDWRITTEN PRICE RULE:
            If the hand_written notes have any numbers with a '$' symbol (e.g. '$150' or '$225.00'), extract that amount and assign it directly as the 'price' for this record.
            
            Respond strictly in valid JSON format. Your output must match this schema:
            {
              "records": [
                {
                  "manifest": "manifest number",
                  "date_hauled": "YYYY-MM-DD",
                  "vehicle_details": "vehicle details",
                  "vin": "17-character VIN",
                  "origin": "origin location",
                  "destination": "destination location",
                  "for_client": "client name",
                  "price": 150.00,
                  "hand_written": "handwritten notes or none"
                }
              ]
            }
        """.trimIndent()

        parts.add(GeminiPart(text = prompt))

        // Attach each page bitmap as inlineData
        bitmaps.forEach { bitmap ->
            parts.add(GeminiPart(inlineData = GeminiInlineData(
                mimeType = "image/jpeg",
                data = bitmap.toBase64()
            )))
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = parts)),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        val response = service.generateContent(apiKey, request)
        val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned empty candidate content")

        // Parse extracted JSON string
        val adapter = moshi.adapter(ExtractedBatch::class.java)
        val cleanedJson = cleanJsonString(textResult)
        val batch = adapter.fromJson(cleanedJson) ?: throw IllegalStateException("Failed to parse JSON response: $cleanedJson")
        
        return batch.records
    }

    private fun cleanJsonString(json: String): String {
        var cleaned = json.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }
}
