package com.jenkinstowing.dailyloads.loadsheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jenkinstowing.dailyloads.loadsheet.ui.MainLayout
import com.jenkinstowing.dailyloads.loadsheet.ui.MainViewModel
import com.jenkinstowing.dailyloads.loadsheet.ui.theme.MyApplicationTheme
import com.jenkinstowing.dailyloads.MainActivity
import org.json.JSONArray
import org.json.JSONObject

class LoadSheetActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: MainViewModel = viewModel()
        Surface(modifier = Modifier.fillMaxSize()) {
          MainLayout(
            viewModel = viewModel,
            onProcessingComplete = { records ->
              val payload = JSONArray().apply {
                records.forEach { record ->
                  put(JSONObject().apply {
                    put("vehicleDetails", record.vehicleDetails)
                    put("vin", record.vin)
                    put("origin", record.origin)
                    put("destination", record.destination)
                    put("notes", record.handWritten)
                    put("drivetrain", record.drivetrain)
                    put("epb", record.epb)
                  })
                }
              }.toString()
              startActivity(
                Intent(this@LoadSheetActivity, MainActivity::class.java)
                  .putExtra(MainActivity.EXTRA_PROCESSED_LOADS, payload)
              )
              finish()
            }
          )
        }
      }
    }
  }
}
