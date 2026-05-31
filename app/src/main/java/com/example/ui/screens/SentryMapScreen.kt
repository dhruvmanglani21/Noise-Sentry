package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoiseReportEntity
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SentryMapScreen(viewModel: NoiseSentryViewModel) {
    val reports by viewModel.allReports.collectAsState()
    val readings by viewModel.recentReadings.collectAsState()
    val currentLat by viewModel.currentLat.collectAsState()
    val currentLng by viewModel.currentLng.collectAsState()

    var activeFilter by remember { mutableStateOf("All") }

    // Pulse animation for hot spots
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_fraction"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
            .testTag("map_screen_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Map HUD Panel header
        Text(
            text = "🌐 SONIC GEOMETRIC GRID MAP",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        // Coordinates status ticker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGray, RoundedCornerShape(8.dp))
                .background(CyberDarkCard)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CURRENT RADAR ANCHOR:", color = CyberLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LAT: ${String.format("%.5f", currentLat)} | LNG: ${String.format("%.5f", currentLng)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "GPS LOCK SECURED",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Category Filter Buttons Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf("All", "Construction", "Machinery", "Traffic", "Party")
            categories.forEach { cat ->
                val isSelected = activeFilter == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonCyan else CyberDarkCard)
                        .border(1.dp, if (isSelected) NeonCyan else CyberGray, RoundedCornerShape(6.dp))
                        .clickable { activeFilter = cat }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.uppercase(),
                        color = if (isSelected) CyberBlack else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Main custom interactive Canvas map grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .background(CyberDarkCard)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Scale tap position to simulated coordinate window:
                            // We center the canvas on the simulated base: 25.1193, 55.3773
                            val width = size.width
                            val height = size.height
                            val centerX = width / 2
                            val centerY = height / 2

                            // 100px translates roughly to 0.005 latitude/longitude delta representation
                            val deltaLat = -((offset.y - centerY) / height) * 0.02
                            val deltaLng = ((offset.x - centerX) / width) * 0.02

                            viewModel.updateSimulatedLocation(
                                25.1193 + deltaLat,
                                55.3773 + deltaLng
                            )
                        }
                    }
                    .testTag("interactive_canvas_map")
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val centerY = height / 2

                // 1. Draw concentrical cyber radar grids
                val radarRadii = listOf(width * 0.15f, width * 0.32f, width * 0.45f)
                for (radius in radarRadii) {
                    drawCircle(
                        color = NeonCyan,
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 0.7.dp.toPx(), pathEffect = null),
                        alpha = 0.12f
                    )
                }

                // Radar sector division lines
                drawLine(
                    color = NeonCyan.copy(alpha = 0.08f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = NeonCyan.copy(alpha = 0.08f),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, height),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Draw historical mapped noise report hotspots
                val filteredReports = if (activeFilter == "All") reports else reports.filter { it.category.equals(activeFilter, true) }
                filteredReports.forEach { rep ->
                    // Convert report coordinates relative to base (25.1193, 55.3773)
                    val offsetLat = rep.lat - 25.1193
                    val offsetLng = rep.lng - 55.3773

                    // Map coordinate shifts to pixel positions
                    val rX = centerX + (offsetLng / 0.02) * width
                    val rY = centerY - (offsetLat / 0.02) * height

                    // Filter outside boundaries
                    if (rX >= 0 && rX <= width && rY >= 0 && rY <= height) {
                        val color = when (rep.category) {
                            "Construction" -> NeonRed
                            "Machinery" -> NeonRed
                            "Traffic" -> NeonYellow
                            "Party" -> NeonPurple
                            else -> NeonCyan
                        }

                        // Hotspot radiating halo
                        drawCircle(
                            color = color,
                            radius = 24.dp.toPx() * pulseFraction,
                            center = Offset(rX.toFloat(), rY.toFloat()),
                            alpha = 0.35f * (1.0f - pulseFraction)
                        )

                        // Central core node
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = Offset(rX.toFloat(), rY.toFloat())
                        )
                    }
                }

                // 3. Draw active real-time microphone logged coordinates
                readings.forEach { rd ->
                    val offsetLat = rd.lat - 25.1193
                    val offsetLng = rd.lng - 55.3773
                    val rdX = centerX + (offsetLng / 0.02) * width
                    val rdY = centerY - (offsetLat / 0.02) * height

                    if (rdX >= 0 && rdX <= width && rdY >= 0 && rdY <= height) {
                        val color = if (rd.dbLevel > 85.0) NeonRed else CyberGreenSafe
                        drawCircle(
                            color = color,
                            radius = 2.dp.toPx(),
                            center = Offset(rdX.toFloat(), rdY.toFloat()),
                            alpha = 0.6f
                        )
                    }
                }

                // 4. Draw Current Sentry location dot (flashing cyan radar crosshair)
                val userOffsetLat = currentLat - 25.1193
                val userOffsetLng = currentLng - 55.3773
                val uX = centerX + (userOffsetLng / 0.02) * width
                val uY = centerY - (userOffsetLat / 0.02) * height

                if (uX >= 0 && uX <= width && uY >= 0 && uY <= height) {
                    // Pulsing Ring
                    drawCircle(
                        color = NeonCyan,
                        radius = 20.dp.toPx() * pulseFraction,
                        center = Offset(uX.toFloat(), uY.toFloat()),
                        style = Stroke(width = 1.dp.toPx()),
                        alpha = 0.8f * (1.0f - pulseFraction)
                    )
                    // Core point
                    drawCircle(
                        color = NeonCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(uX.toFloat(), uY.toFloat())
                    )
                }
            }

            // Map UI overlays
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .widthIn(max = 180.dp)
                    .background(CyberBlack.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .border(1.dp, CyberGray, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("TACTICAL LEGEND:", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonYellow))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Traffic Zone", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Industrial / Build", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonPurple))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Night Lounges", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Your Location (Crosshair)", color = CyberLightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Helper badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(CyberBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "👇 TAP SCREEN TO SIMULATE LOCATION",
                    color = NeonCyan,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
