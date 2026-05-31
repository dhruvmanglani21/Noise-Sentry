package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import java.io.File
import java.io.FileInputStream

@Composable
fun ExportScreen(viewModel: NoiseSentryViewModel) {
    val reports by viewModel.allReports.collectAsState()
    val readings by viewModel.recentReadings.collectAsState()
    val context = LocalContext.current

    var selectedFormat by remember { mutableStateOf("JSON") }
    var exportResultFile by remember { mutableStateOf<File?>(null) }
    var rawTextPreview by remember { mutableStateOf("") }
    
    var isCompiling by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 16.dp)
            .testTag("export_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Module Heading
        item {
            Text(
                text = "📁 MUNICIPAL HEALTH & CIVIL EXPORT TERMINAL",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stats Ledger Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DISTRICT CACHE OVERVIEW",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "These compiled records are packed with verified metrics and logged acoustic incident parameters ready for analysis by municipal engineers to combat community noise pollution.",
                        color = CyberLightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExportStatBox(label = "Logged Incidents", count = reports.size.toString(), color = NeonYellow)
                        ExportStatBox(label = "Sound Level Scans", count = readings.size.toString(), color = NeonCyan)
                    }
                }
            }
        }

        // Export Actions Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonYellow.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "COMPILE SYSTEM RECORDS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Selection Row (JSON vs CSV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButtonSelection(format = "JSON", isSelected = selectedFormat == "JSON") { selectedFormat = "JSON" }
                        IconButtonSelection(format = "CSV", isSelected = selectedFormat == "CSV") { selectedFormat = "CSV" }
                    }

                    // Export Build button
                    Button(
                        onClick = {
                            isCompiling = true
                            val generated = viewModel.exportDataForAuthorities(context, selectedFormat)
                            exportResultFile = generated
                            if (generated != null) {
                                try {
                                    val stream = FileInputStream(generated)
                                    val size = stream.available()
                                    val buffer = ByteArray(size)
                                    stream.read(buffer)
                                    stream.close()
                                    rawTextPreview = String(buffer)
                                } catch (e: Exception) {
                                    rawTextPreview = "Error importing build contents: ${e.message}"
                                }
                            }
                            isCompiling = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("compile_export_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.BuildCircle, contentDescription = null, tint = CyberBlack, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCompiling) "COMPILING SECTOR DATA..." else "BUILD DISTRICT DISPATCH",
                                color = CyberBlack,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Raw terminal output preview
        if (rawTextPreview.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> RAW EXPORT TRANSMISSION PREVIEW",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LOCAL PATH: cache/${exportResultFile?.name}",
                        color = CyberLightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.dp, CyberGray, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = rawTextPreview,
                                color = CyberGreenSafe,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberGreenSafe.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreenSafe)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyberGreenSafe)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "District data Compiled successfully! Municipal systems can consume this schema safely to identify high-decibel disturbances.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ExportStatBox(label: String, count: String, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberBlack)
            .border(1.dp, CyberGray, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = label, color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun RowScope.IconButtonSelection(format: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonYellow.copy(alpha = 0.15f) else CyberBlack)
            .border(1.dp, if (isSelected) NeonYellow else CyberGray, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = if (format == "JSON") Icons.Default.DataObject else Icons.Default.GridOn,
                contentDescription = null,
                tint = if (isSelected) NeonYellow else CyberLightGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$format DOCUMENT",
                color = if (isSelected) Color.White else CyberLightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
