package com.jenkinstowing.dailyloads.loadsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenkinstowing.dailyloads.loadsheet.data.ExtractedRecord
import java.util.Locale

@Composable
fun MetricsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historicalRecords by viewModel.historicalRecords.collectAsState()
    val scrollState = rememberScrollState()

    // Aggregate values
    val totalLoads = historicalRecords.size
    val totalRevenue = historicalRecords.sumOf { it.price }
    val averageLoadValue = if (totalLoads > 0) totalRevenue / totalLoads else 0.0
    val uniqueClients = historicalRecords.map { it.forClient.trim().lowercase() }.distinct().filter { it.isNotBlank() }.size

    // Calculate Top Clients
    val topClients = remember(historicalRecords) {
        historicalRecords.groupBy { it.forClient.trim() }
            .mapValues { entry ->
                val count = entry.value.size
                val rev = entry.value.sumOf { it.price }
                Pair(count, rev)
            }
            .toList()
            .sortedByDescending { it.second.first } // sort by load count
            .take(5)
    }

    // Calculate Top Routes
    val topRoutes = remember(historicalRecords) {
        historicalRecords.groupBy { "${it.origin.trim()} ➔ ${it.destination.trim()}" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    // Calculate EPB distribution
    val epbYesCount = historicalRecords.count { it.epb.trim().equals("yes", ignoreCase = true) }
    val epbNoCount = totalLoads - epbYesCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title
        Column {
            Text(
                text = "Admin Metrics Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Real-time reports, fleet metrics, and transport analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (historicalRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Empty metrics",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Metrics Data Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregates and stats will generate automatically once loads are saved.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Loads",
                    value = "$totalLoads",
                    icon = Icons.Default.LocalShipping,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Earnings",
                    value = String.format(Locale.US, "$%.2f", totalRevenue),
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1.2f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Avg Load Pay",
                    value = String.format(Locale.US, "$%.2f", averageLoadValue),
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1.1f)
                )
                MetricCard(
                    title = "Active Clients",
                    value = "$uniqueClients",
                    icon = Icons.Default.BusinessCenter,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Top Clients Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Top Clients (by Load Count)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    topClients.forEach { (clientName, stats) ->
                        val (loadCount, rev) = stats
                        val pct = if (totalLoads > 0) loadCount.toFloat() / totalLoads else 0f
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = clientName.ifBlank { "Unspecified" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$loadCount load(s) | " + String.format(Locale.US, "$%.2f", rev),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // EPB Distribution & Vehicle Safety section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Electronic Parking Brake (EPB) Fleet Share",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val yesPct = if (totalLoads > 0) (epbYesCount.toFloat() / totalLoads) else 0f
                    val yesPctFormatted = String.format(Locale.US, "%.1f%%", yesPct * 100)
                    val noPctFormatted = String.format(Locale.US, "%.1f%%", (1f - yesPct) * 100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "EPB Installed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(text = "$epbYesCount vehicles ($yesPctFormatted)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Manual Parking Brake", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(text = "$epbNoCount vehicles ($noPctFormatted)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }

                    LinearProgressIndicator(
                        progress = { yesPct },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFF2E7D32),
                        trackColor = Color(0xFFD32F2F)
                    )
                }
            }

            // Top Routes Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Top Transport Routes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    topRoutes.forEachIndexed { idx, (routeName, routeCount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Text(
                                text = routeName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$routeCount load(s)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
