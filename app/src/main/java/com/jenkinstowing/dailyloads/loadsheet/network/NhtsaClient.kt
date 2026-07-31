package com.jenkinstowing.dailyloads.loadsheet.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NhtsaResponse(
    @Json(name = "Results") val results: List<NhtsaResult>?
)

@JsonClass(generateAdapter = true)
data class NhtsaResult(
    @Json(name = "DriveType") val driveType: String?,
    @Json(name = "ParkBrake") val parkBrake: String?
)

interface NhtsaService {
    @GET("vehicles/DecodeVinValues/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json"
    ): NhtsaResponse
}

object NhtsaClient {
    private const val BASE_URL = "https://vpic.nhtsa.dot.gov/api/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: NhtsaService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NhtsaService::class.java)
    }

    suspend fun fetchVinDetails(vin: String): Pair<String, String> {
        return try {
            val response = service.decodeVin(vin.trim())
            val result = response.results?.firstOrNull()
            
            // Map drivetrain details
            val rawDriveType = result?.driveType ?: ""
            val drivetrain = when {
                rawDriveType.contains("All", ignoreCase = true) || rawDriveType.contains("AWD", ignoreCase = true) || rawDriveType.contains("4WD", ignoreCase = true) || rawDriveType.contains("4x4", ignoreCase = true) -> "All wheel drive"
                rawDriveType.contains("Front", ignoreCase = true) || rawDriveType.contains("FWD", ignoreCase = true) || rawDriveType.contains("4x2", ignoreCase = true) -> "Front wheel drive"
                rawDriveType.contains("Rear", ignoreCase = true) || rawDriveType.contains("RWD", ignoreCase = true) -> "Rear wheel drive"
                rawDriveType.isNotBlank() -> rawDriveType
                else -> "Front wheel drive" // Default or fallback
            }

            // Map EPB details
            val rawParkBrake = result?.parkBrake ?: ""
            val epb = when {
                rawParkBrake.contains("Electric", ignoreCase = true) || rawParkBrake.contains("EPB", ignoreCase = true) || rawParkBrake.contains("Yes", ignoreCase = true) -> "Yes"
                else -> "No"
            }

            Pair(drivetrain, epb)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Front wheel drive", "No") // Safely return fallbacks on error
        }
    }
}
