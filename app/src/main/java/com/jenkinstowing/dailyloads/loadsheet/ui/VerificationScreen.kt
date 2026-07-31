package com.jenkinstowing.dailyloads.loadsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onProcessingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBatch by viewModel.currentBatch.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val scope = rememberCoroutineScope()

    if (currentBatch.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FactCheck,
                    contentDescription = "Empty batch",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = "No Loads for Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Go to the 'Dashboard' tab to upload and scan PDF load sheets.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNavigateToDashboard) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to Dashboard")
                }
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.applyAndSyncBatch(onSuccess = onProcessingComplete)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("apply_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isProcessing
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Apply to Spreadsheets")
                        }
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "Edit & Verify Extracted Loads",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Verify values before applying them to Jenkins Paysheet and Daily Loads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                itemsIndexed(currentBatch) { index, item ->
                    var isNhtsaLoading by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Text(
                                        text = "Load Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteRecordFromBatch(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete record",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            // 1. Manifest
                            OutlinedTextField(
                                value = item.manifest,
                                onValueChange = { viewModel.updateRecordInBatch(index, item.copy(manifest = it)) },
                                label = { Text("Manifest #") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Date Hauled & Client (Row)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.dateHauled,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(dateHauled = it)) },
                                    label = { Text("Date Hauled") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.forClient,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(forClient = it)) },
                                    label = { Text("For / Client") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Vehicle Details
                            OutlinedTextField(
                                value = item.vehicleDetails,
                                onValueChange = { viewModel.updateRecordInBatch(index, item.copy(vehicleDetails = it)) },
                                label = { Text("Vehicle Details") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 4. VIN Number (With NHTSA action!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.vin,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(vin = it)) },
                                    label = { Text("VIN (17 characters)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    supportingText = {
                                        if (item.vin.length != 17) {
                                            Text(
                                                text = "${item.vin.length}/17 characters",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isNhtsaLoading = true
                                            val (drv, park) = com.jenkinstowing.dailyloads.loadsheet.network.NhtsaClient.fetchVinDetails(item.vin)
                                            viewModel.updateRecordInBatch(index, item.copy(drivetrain = drv, epb = park))
                                            isNhtsaLoading = false
                                        }
                                    },
                                    enabled = item.vin.length == 17 && !isNhtsaLoading,
                                    modifier = Modifier.padding(top = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    if (isNhtsaLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Lookup", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("NHTSA", fontSize = 11.sp)
                                    }
                                }
                            }

                            // 5. Origin & Destination (Row)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.origin,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(origin = it)) },
                                    label = { Text("Origin") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.destination,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(destination = it)) },
                                    label = { Text("Destination") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 6. Price (Double)
                            OutlinedTextField(
                                value = if (item.price == 0.0) "" else item.price.toString(),
                                onValueChange = {
                                    val parsedPrice = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateRecordInBatch(index, item.copy(price = parsedPrice))
                                },
                                label = { Text("Price ($)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                leadingIcon = { Text("$ ", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 7. Hand Written Notes
                            OutlinedTextField(
                                value = item.handWritten,
                                onValueChange = { viewModel.updateRecordInBatch(index, item.copy(handWritten = it)) },
                                label = { Text("Handwritten Notes") },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8. Drivetrain & EPB Status (NHTSA decoded!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drivetrain Input
                                OutlinedTextField(
                                    value = item.drivetrain,
                                    onValueChange = { viewModel.updateRecordInBatch(index, item.copy(drivetrain = it)) },
                                    label = { Text("Drivetrain") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )

                                // EPB Yes/No Switch Selector
                                Column(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "EPB (Yes/No)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(text = "No", style = MaterialTheme.typography.bodyMedium, fontWeight = if (item.epb == "No") FontWeight.Bold else FontWeight.Normal)
                                        Switch(
                                            checked = item.epb == "Yes",
                                            onCheckedChange = { isChecked ->
                                                viewModel.updateRecordInBatch(index, item.copy(epb = if (isChecked) "Yes" else "No"))
                                            }
                                        )
                                        Text(text = "Yes", style = MaterialTheme.typography.bodyMedium, fontWeight = if (item.epb == "Yes") FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
