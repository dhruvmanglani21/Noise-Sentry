package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(viewModel: NoiseSentryViewModel) {
    val reports by viewModel.allReports.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLat by viewModel.currentLat.collectAsState()
    val currentLng by viewModel.currentLng.collectAsState()

    val safeUser = currentUser ?: return
    val userReports = reports.filter { it.userId == safeUser.username }

    // Core form states
    var addressInput by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Construction") }
    var commentInput by remember { mutableStateOf("") }
    var estDecibels by remember { mutableStateOf(75f) }

    // Custom Date & Time values (defaulting to current local time)
    val formatterDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val formatterTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var incidentDateStr by remember { mutableStateOf(formatterDate.format(Date())) }
    var incidentTimeStr by remember { mutableStateOf(formatterTime.format(Date())) }

    // Map Coordinates picker (synchronized initially with the user's live coordinates)
    var selectedLat by remember { mutableStateOf(currentLat) }
    var selectedLng by remember { mutableStateOf(currentLng) }

    // Launch effect to sync initially when GPS anchors change or on screen entry
    LaunchedEffect(currentLat, currentLng) {
        selectedLat = currentLat
        selectedLng = currentLng
    }

    // Evidence simulation states
    var audioEvidencePath by remember { mutableStateOf<String?>(null) }
    var photoEvidencePath by remember { mutableStateOf<String?>(null) }

    // Activity simulation triggers
    var isRecordingEvidence by remember { mutableStateOf(false) }
    var evidenceRecordProgress by remember { mutableStateOf(0f) }
    var isSimulatingCamera by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Pulse animation for coordinates map node
    val infiniteTransition = rememberInfiniteTransition(label = "micro_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    // Simulating audio recording timer countdown
    LaunchedEffect(isRecordingEvidence) {
        if (isRecordingEvidence) {
            evidenceRecordProgress = 0f
            while (evidenceRecordProgress < 1.0f) {
                delay(100)
                evidenceRecordProgress += 0.04f // Updates over ~2.5 seconds grab duration
            }
            isRecordingEvidence = false
            audioEvidencePath = "/cache/SENTRY_AUDIO_GRAB_${(1000..9999).random()}.wav"
        }
    }

    // Simulating physical shutter snapper
    LaunchedEffect(isSimulatingCamera) {
        if (isSimulatingCamera) {
            delay(1200)
            isSimulatingCamera = false
            photoEvidencePath = "/cache/SENTRY_SNAP_${(1000..9999).random()}.jpg"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 16.dp)
            .testTag("reporting_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Form Title Banner
        item {
            Text(
                text = "📜 SENTRY DISTURBANCE REPORTING TERMINAL",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Main Complaining sheet
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "NEW COMPLAINT LEDGER SHEET",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Address Form Input Text Field
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Incident Area Details / Address", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberGray,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = CyberLightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        placeholder = { Text("e.g. Commercial Block C, East Alleyway", color = CyberLightGray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("address_input_field")
                    )

                    // MAP INTEGRATION: Clickable Pin Location Selection
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LOCATION GEOMETRICAL MATRIX (MAP INTEGRATION):",
                            color = CyberLightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Visual coordinates summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .border(1.dp, CyberGray, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("COMPLAINT COORD RETICLE:", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "LAT: ${String.format("%.5f", selectedLat)} | LNG: ${String.format("%.5f", selectedLng)}",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedLat = currentLat
                                    selectedLng = currentLng
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Sync GPS Location",
                                    tint = NeonYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Compact interactive radar canvas map
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBlack)
                                .border(1.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val width = size.width
                                            val height = size.height
                                            val centerX = width / 2
                                            val centerY = height / 2

                                            // Map pixel offset to local coordinate delta representation
                                            val deltaLat = -((offset.y - centerY) / height) * 0.015
                                            val deltaLng = ((offset.x - centerX) / width) * 0.015

                                            selectedLat = 25.1193 + deltaLat
                                            selectedLng = 55.3773 + deltaLng
                                        }
                                    }
                            ) {
                                val width = size.width
                                val height = size.height
                                val centerX = width / 2
                                val centerY = height / 2

                                // Draw circular radar matrix rings
                                val gridRadii = listOf(width * 0.2f, width * 0.45f)
                                for (rad in gridRadii) {
                                    drawCircle(
                                        color = NeonCyan,
                                        radius = rad,
                                        center = Offset(centerX, centerY),
                                        style = Stroke(width = 0.5.dp.toPx()),
                                        alpha = 0.1f
                                    )
                                }

                                // Interactive target grid lines
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.05f),
                                    start = Offset(0f, centerY),
                                    end = Offset(width, centerY),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.05f),
                                    start = Offset(centerX, 0f),
                                    end = Offset(centerX, height),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // Draw pinpoint selected location crosshair
                                val pinOffsetLat = selectedLat - 25.1193
                                val pinOffsetLng = selectedLng - 55.3773
                                val pX = centerX + (pinOffsetLng / 0.015) * width
                                val pY = centerY - (pinOffsetLat / 0.015) * height

                                if (pX in 0f..width && pY in 0f..height) {
                                    // Pulsing outer halo circle
                                    drawCircle(
                                        color = NeonCyan,
                                        radius = 16.dp.toPx() * pulseScale,
                                        center = Offset(pX.toFloat(), pY.toFloat()),
                                        style = Stroke(width = 1.dp.toPx()),
                                        alpha = 0.8f * (1f - pulseScale)
                                    )
                                    // Static target center
                                    drawCircle(
                                        color = NeonYellow,
                                        radius = 4.dp.toPx(),
                                        center = Offset(pX.toFloat(), pY.toFloat())
                                    )
                                }
                            }

                            // Guideline caption overlay
                            Text(
                                text = "👆 TAP RADAR AREA TO SHIFT PINPOINT COORDINATES",
                                color = NeonCyan.copy(alpha = 0.6f),
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(6.dp)
                            )
                        }
                    }

                    // Disturbance specific type category row
                    Column {
                        Text("SPECIFIC TYPE OF NOISE:", color = CyberLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val listCategories = listOf("Traffic", "Construction", "Machinery", "Party", "Other")
                            listCategories.forEach { cat ->
                                val selected = categorySelection == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) NeonPurple else CyberBlack)
                                        .border(1.dp, if (selected) NeonPurple else CyberGray, RoundedCornerShape(6.dp))
                                        .clickable { categorySelection = cat }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.uppercase(),
                                        color = if (selected) Color.White else CyberLightGray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // INCIDENT DATE & TIME SELECTION SECTION
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INCIDENT DATETIME SPECIFICATION:",
                            color = CyberLightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date Input
                            OutlinedTextField(
                                value = incidentDateStr,
                                onValueChange = { incidentDateStr = it },
                                label = { Text("Incident Date (YYYY-MM-DD)", fontFamily = FontFamily.Monospace, fontSize = 8.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                singleLine = true,
                                modifier = Modifier.weight(1.1f)
                            )

                            // Time Input
                            OutlinedTextField(
                                value = incidentTimeStr,
                                onValueChange = { incidentTimeStr = it },
                                label = { Text("Incident Time (HH:MM)", fontFamily = FontFamily.Monospace, fontSize = 8.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                singleLine = true,
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        // Quick Select Datetime Presets Line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUICK SECURE:",
                                color = CyberLightGray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(2.dp))

                            // Preset Just Now
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberBlack)
                                    .border(1.dp, CyberGray, RoundedCornerShape(4.dp))
                                    .clickable {
                                        incidentDateStr = formatterDate.format(Date())
                                        incidentTimeStr = formatterTime.format(Date())
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("JUST NOW", color = NeonCyan, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            // Preset 1-Hr Ago
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberBlack)
                                    .border(1.dp, CyberGray, RoundedCornerShape(4.dp))
                                    .clickable {
                                        val oneHrAgo = System.currentTimeMillis() - 3600000L
                                        incidentDateStr = formatterDate.format(Date(oneHrAgo))
                                        incidentTimeStr = formatterTime.format(Date(oneHrAgo))
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("-1 HOUR", color = NeonYellow, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            }

                            // Preset Yesterday
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberBlack)
                                    .border(1.dp, CyberGray, RoundedCornerShape(4.dp))
                                    .clickable {
                                        val yesterday = System.currentTimeMillis() - 86400000L
                                        incidentDateStr = formatterDate.format(Date(yesterday))
                                        incidentTimeStr = formatterTime.format(Date(yesterday))
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("YESTERDAY", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Decibel intensity estimated range
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ESTIMATED INTENSITY VOLUME:", color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("${estDecibels.toInt()} dB", color = NeonYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = estDecibels,
                            onValueChange = { estDecibels = it },
                            valueRange = 35f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonYellow,
                                activeTrackColor = NeonYellow,
                                inactiveTrackColor = CyberGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("est_db_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("35 dB (Quiet Room)", color = CyberLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("120 dB (Airport Extreme)", color = NeonRed, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // AUDIO AND PHOTO EVIDENCE INCIDENT UPLOAD
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INCIDENT EVIDENCE CAPTURE (AUDIO / PHOTO):",
                            color = CyberLightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audio Recording Evidence Btn
                            Button(
                                onClick = { isRecordingEvidence = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (audioEvidencePath != null) CyberGray else NeonCyan),
                                enabled = !isRecordingEvidence,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (audioEvidencePath != null) Icons.Default.CheckCircle else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (audioEvidencePath != null) CyberGreenSafe else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRecordingEvidence) "RECORDING..." else if (audioEvidencePath != null) "AURA GRABBED" else "🎤 GRAB AUDIO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Photo Clapper capturing snap Evidence Btn
                            Button(
                                onClick = { isSimulatingCamera = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (photoEvidencePath != null) CyberGray else NeonCyan),
                                enabled = !isSimulatingCamera,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (photoEvidencePath != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = if (photoEvidencePath != null) CyberGreenSafe else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSimulatingCamera) "SNAPPING..." else if (photoEvidencePath != null) "SNAP SECURED" else "📷 SNAP PHOTO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Wave animation if recording is actively running
                        AnimatedVisibility(visible = isRecordingEvidence) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberBlack, RoundedCornerShape(8.dp))
                                    .border(1.dp, NeonYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎤 MAPPED MIC SAMPLING REAL TIME FREQUENCY... 5 SECS GRAB", color = NeonYellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { evidenceRecordProgress },
                                    color = NeonYellow,
                                    trackColor = CyberGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                )
                            }
                        }

                        // Shutter Camera Snap Flash trigger visibility
                        AnimatedVisibility(visible = isSimulatingCamera) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.9f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📷 APERTURE SENSOR SECURING GRID HIGH RES SHUTTER...", color = CyberBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Visualized Secure Files Indicator Cards
                        if (audioEvidencePath != null || photoEvidencePath != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberBlack.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .border(1.dp, CyberGray, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("ATTACHED DIGITAL INTEGRITY EVIDENCE PACKAGE:", color = CyberLightGray, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                                if (audioEvidencePath != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SENTRY_MIC_${audioEvidencePath?.takeLast(8)}.wav", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyberGreenSafe, modifier = Modifier.size(12.dp))
                                    }
                                }

                                if (photoEvidencePath != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhotoSizeSelectActual, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SENTRY_SNAP_${photoEvidencePath?.takeLast(8)}.jpeg", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyberGreenSafe, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Comments or notes field
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("Incident Description / Issue Notes", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberGray,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = CyberLightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        placeholder = { Text("List continuously recurring vibration sources, horn honking peaks, or other elements...", color = CyberLightGray, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("comments_input_field")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Final Complaint submission terminal button
                    Button(
                        onClick = {
                            isSubmitting = true
                            viewModel.submitNoiseReport(
                                category = categorySelection,
                                address = addressInput,
                                comment = commentInput,
                                estDecibel = estDecibels.toInt(),
                                incidentDate = incidentDateStr,
                                incidentTime = incidentTimeStr,
                                audioEvidencePath = audioEvidencePath,
                                photoEvidencePath = photoEvidencePath,
                                customLat = selectedLat,
                                customLng = selectedLng,
                                onComplete = {
                                    addressInput = ""
                                    commentInput = ""
                                    estDecibels = 75f
                                    audioEvidencePath = null
                                    photoEvidencePath = null
                                    isSubmitting = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_report_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "COMMIT COMPLAINT LOG Entry [GAIN +150 XP]",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Subtitle Municipal ledger
        item {
            Text(
                text = "> SECURED LOCAL MUNICIPAL LEDGER",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // Reports Empty State
        if (userReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded noise complaints lodged under your agent keys yet. All committed files route directly to municipal planning authorities.",
                            color = CyberLightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(userReports) { rep ->
                ReportListItem(report = rep)
            }
        }
    }
}
