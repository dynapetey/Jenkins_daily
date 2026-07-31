package com.jenkinstowing.dailyloads

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class GoogleSheetsRepository {
    private val aliases = mapOf(
        "vehicle" to listOf("vehicle details", "vehicle", "vehicle description", "year make model"),
        "vin" to listOf("vin", "vehicle identification number"),
        "origin" to listOf("origin", "pickup", "pickup location"),
        "destination" to listOf("destination", "delivery", "delivery location"),
        "notes" to listOf("notes", "note", "hand written", "handwritten", "hand written notes", "comments"),
        "drivetrain" to listOf("drivetrain", "drive train", "drive type"),
        "epb" to listOf("epb", "electronic parking brake", "electric parking brake"),
        "completed" to listOf("completed"),
    )

    fun getTodaySheet(token: String, date: String): DailySheet {
        val fileName = "${date}_LOAD Sheet"
        val escapedName = fileName.replace("'", "\\'")
        val query = "name='$escapedName' and mimeType='application/vnd.google-apps.spreadsheet' and trashed=false"
        val driveUrl = "https://www.googleapis.com/drive/v3/files?q=${encode(query)}&fields=files(id,name)&pageSize=10"
        val files = request(driveUrl, token).optJSONArray("files") ?: JSONArray()
        val matches = (0 until files.length())
            .map { files.getJSONObject(it) }
            .filter { it.optString("name") == fileName }
        if (matches.isEmpty()) error("Couldn’t find “$fileName” in Google Drive.")
        if (matches.size > 1) error("Found more than one “$fileName”. Keep one daily sheet or move duplicates to trash.")

        val spreadsheetId = matches.single().getString("id")
        val valuesUrl = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/${encode("'Sheet1'")}"
        val values = request(valuesUrl, token).optJSONArray("values") ?: JSONArray()
        if (values.length() == 0) error("Sheet1 is empty and has no header row.")

        val rows = (0 until values.length()).map { index ->
            val row = values.getJSONArray(index)
            (0 until row.length()).map { row.optString(it) }
        }
        val headers = rows.first()
        val columns = aliases.mapValues { (_, names) -> findColumn(headers, names) }
        var completedColumn = columns.getValue("completed")
        if (completedColumn < 0) {
            completedColumn = rows.maxOf { it.size }
            val headerRange = "'Sheet1'!${columnLetter(completedColumn)}1"
            updateCell(token, spreadsheetId, headerRange, "Completed", "RAW")
        }

        val dataColumns = listOf("vehicle", "vin", "origin", "destination", "notes", "drivetrain", "epb")
        val loads = rows.drop(1).mapIndexedNotNull { index, row ->
            if (dataColumns.none { cell(row, columns.getValue(it)).isNotBlank() }) return@mapIndexedNotNull null
            Load(
                rowNumber = index + 2,
                vehicleDetails = cell(row, columns.getValue("vehicle")),
                vin = cell(row, columns.getValue("vin")),
                origin = cell(row, columns.getValue("origin")),
                destination = cell(row, columns.getValue("destination")),
                notes = cell(row, columns.getValue("notes")),
                drivetrain = cell(row, columns.getValue("drivetrain")),
                epb = cell(row, columns.getValue("epb")),
                completed = isCompleted(cell(row, completedColumn)),
            )
        }
        return DailySheet(spreadsheetId, completedColumn, loads)
    }

    fun updateCompleted(token: String, sheet: DailySheet, rowNumber: Int, completed: Boolean) {
        updateCell(
            token,
            sheet.spreadsheetId,
            "'Sheet1'!${columnLetter(sheet.completedColumn)}$rowNumber",
            completed,
            "USER_ENTERED",
        )
    }

    private fun updateCell(
        token: String,
        spreadsheetId: String,
        range: String,
        value: Any,
        inputOption: String,
    ) {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/${encode(range)}?valueInputOption=$inputOption"
        val body = JSONObject().put("values", JSONArray().put(JSONArray().put(value))).toString()
        request(url, token, "PUT", body)
    }

    private fun request(url: String, token: String, method: String = "GET", body: String? = null): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(response).getJSONObject("error").getString("message")
                }.getOrNull()
                if (status == 401) error("Your Google session expired. Sign out and sign in again.")
                error(message ?: "Google request failed ($status).")
            }
            if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(value: String) = value.trim().lowercase()
        .replace(Regex("[_-]+"), " ")
        .replace(Regex("\\s+"), " ")

    private fun findColumn(headers: List<String>, names: List<String>): Int =
        headers.indexOfFirst { normalize(it) in names }

    private fun cell(row: List<String>, index: Int): String =
        if (index >= 0) row.getOrNull(index).orEmpty().trim() else ""

    private fun isCompleted(value: String): Boolean =
        normalize(value) in setOf("true", "yes", "y", "1", "completed", "done")

    private fun columnLetter(index: Int): String {
        var value = index + 1
        var result = ""
        while (value > 0) {
            value -= 1
            result = ('A'.code + value % 26).toChar() + result
            value /= 26
        }
        return result
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}
