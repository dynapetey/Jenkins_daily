package com.jenkinstowing.dailyloads.loadsheet.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IntegrationGuide(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val appsScriptCode = """
/**
 * Google Apps Script for Load Sheet Manager Integration
 * 
 * Instructions:
 * 1. Open your target Google Sheet.
 * 2. Click Extensions > Apps Script.
 * 3. Delete any default code and paste this script.
 * 4. Click 'Deploy' > 'New deployment'.
 * 5. Set Type to "Web app".
 * 6. Set Execute as to "Me".
 * 7. Set Who has access to "Anyone" (essential for Android integrations).
 * 8. Authorize permissions, click Deploy, and copy the Web App URL.
 * 9. Paste this URL into the 'Settings' panel on the Dashboard tab in this app!
 */

function doPost(e) {
  try {
    var requestData = JSON.parse(e.postData.contents);
    var action = requestData.action;
    
    if (action === "insert_paysheet_row") {
      return handleInsertPaysheetRow(requestData.data);
    } else if (action === "create_daily_loads_sheet") {
      return handleCreateDailyLoadsSheet(requestData.filename, requestData.templateName, requestData.rows);
    } else {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        message: "Invalid action"
      })).setMimeType(ContentService.MimeType.JSON);
    }
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function handleInsertPaysheetRow(data) {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = spreadsheet.getSheetByName("Jenkins_Paysheet");
  
  if (!sheet) {
    sheet = spreadsheet.insertSheet("Jenkins_Paysheet");
    sheet.appendRow([
      "Manifest #", 
      "Date Hauled (date)", 
      "Vehicle Details (vehicleDetails)", 
      "VIN (vin)", 
      "Origin (origin)", 
      "Destination (destination)", 
      "For / Client (forClient)", 
      "Price (price)"
    ]);
    
    // Style headers
    var headerRange = sheet.getRange(1, 1, 1, 8);
    headerRange.setBackground("#1a237e").setFontColor("#ffffff").setFontWeight("bold");
    sheet.setFrozenRows(1);
    sheet.autoResizeColumns(1, 8);
  }
  
  sheet.appendRow([
    data.manifest,
    data.dateHauled,
    data.vehicleDetails,
    data.vin,
    data.origin,
    data.destination,
    data.forClient,
    data.price
  ]);
  
  return ContentService.createTextOutput(JSON.stringify({
    status: "success",
    message: "Row appended to Jenkins_Paysheet"
  })).setMimeType(ContentService.MimeType.JSON);
}

function handleCreateDailyLoadsSheet(filename, templateName, rows) {
  var activeSpreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var timestamp = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "HHmmss");
  var sheetName = filename + " (" + timestamp + ")";
  
  var templateSheet = activeSpreadsheet.getSheetByName(templateName);
  var newSheet;
  
  if (templateSheet) {
    newSheet = templateSheet.copyTo(activeSpreadsheet);
  } else {
    newSheet = activeSpreadsheet.insertSheet();
  }
  
  newSheet.setName(sheetName);
  newSheet.showSheet();
  
  if (!templateSheet) {
    newSheet.appendRow([
      "Manifest #",
      "Vehicle Details",
      "VIN",
      "Origin",
      "Destination",
      "Hand Written Notes",
      "Drivetrain",
      "EPB"
    ]);
    var headerRange = newSheet.getRange(1, 1, 1, 8);
    headerRange.setBackground("#2e7d32").setFontColor("#ffffff").setFontWeight("bold");
    newSheet.setFrozenRows(1);
  }
  
  rows.forEach(function(row) {
    newSheet.appendRow([
      row.manifest,
      row.vehicleDetails,
      row.vin,
      row.origin,
      row.destination,
      row.handWritten,
      row.drivetrain,
      row.epb
    ]);
  });
  
  newSheet.autoResizeColumns(1, 8);
  
  return ContentService.createTextOutput(JSON.stringify({
    status: "success",
    message: "Created sheet " + sheetName + " with " + rows.length + " rows",
    url: activeSpreadsheet.getUrl()
  })).setMimeType(ContentService.MimeType.JSON);
}
""".trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.IntegrationInstructions,
                    contentDescription = "Integration Icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Google Sheets Backend Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Set up your free Apps Script to sync directly with Google Sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            text = "Follow These Simple Steps:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val steps = listOf(
            "1. Open your active Google Sheet (e.g. Jenkins Paysheet spreadsheet).",
            "2. Navigate to Extensions > Apps Script in the toolbar.",
            "3. Clear any default code inside Code.gs and paste the code below.",
            "4. Make sure your sheet template is named Dailyloads.template (or customize it in settings).",
            "5. Click Deploy > New deployment. Select Web app.",
            "6. Set Execute as to 'Me' and Who has access to 'Anyone'.",
            "7. Copy the generated Web App URL and paste it under the settings card on the Dashboard!"
        )

        steps.forEach { step ->
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Copy Script Code:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(appsScriptCode))
                    Toast.makeText(context, "Apps Script code copied!", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Icon",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Copy Code", fontSize = 12.sp)
            }
        }

        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = appsScriptCode,
                    color = Color(0xFFCE9178),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
