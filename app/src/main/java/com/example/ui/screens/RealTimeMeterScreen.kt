package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RealTimeMeterScreen(viewModel: NoiseSentryViewModel) {
    val liveDb by viewModel.liveDb.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val scope = rememberCoroutineScope()

    // Oscilloscope animation phase
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Smooth decibel transition to avoid needle jumping erratically
    val smoothDb by animateFloatAsState(
        targetValue = liveDb,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "smoothDb"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
            .testTag("meter_screen_root"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning Banner HUD Overlay
        AnimatedVisibility(
            visible = alertMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alertMessage ?: "",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.dismissAlertMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Module Title Heading
        Text(
            text = "🔊 SONIC SPECTRUM DECIBEL RADAR",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        // Main Hardware Decibel Meter Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Radial concentric background design paths
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = NeonCyan,
                        radius = 65.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
                        ),
                        alpha = 0.08f
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = 110.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
                        ),
                        alpha = 0.05f
                    )
                }

                // Core content
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "CURRENT INTENSITY",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isRecording) String.format("%.0f", smoothDb) else "00",
                                color = Color.White,
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-2).sp
                            )
                            Text(
                                text = " dB",
                                color = NeonCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        // State Status warning pill
                        val zoneLabel = when {
                            !isRecording -> "REC OFFLINE"
                            smoothDb > 85f -> "CRITICAL ZONE"
                            smoothDb > 70f -> "ELEVATED ZONE"
                            else -> "SAFE ZONE"
                        }
                        val zoneColor = when {
                            !isRecording -> CyberLightGray
                            smoothDb > 85f -> NeonRed
                            smoothDb > 70f -> NeonYellow
                            else -> CyberGreenSafe
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(zoneColor.copy(alpha = 0.12f))
                                .border(1.dp, zoneColor.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = zoneLabel,
                                color = zoneColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // High Density visual wave bars matching design
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Seven symmetrical responsive frequency spectrum bars
                        val spectrumDistribution = listOf(0.2f, 0.45f, 0.75f, 1.0f, 0.6f, 0.35f, 0.15f)
                        spectrumDistribution.forEachIndexed { idx, valueFactor ->
                            // Scale height exponentially with decibel intensity if recording, capped at max height (28.dp)
                            val barScaleFactor = if (isRecording) {
                                val dbRatio = ((smoothDb - 30) / 90f).coerceIn(0f, 1f)
                                (0.15f + dbRatio * 0.85f) * valueFactor
                            } else {
                                0.08f
                            }
                            val targetBarHeight = (32.dp * barScaleFactor).coerceIn(4.dp, 32.dp)

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(4.dp)
                                    .height(targetBarHeight)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        if (isRecording) NeonCyan.copy(alpha = (0.2f + 0.8f * valueFactor))
                                        else Color.White.copy(alpha = 0.15f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Oscilloscope dynamic visual waves box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, CyberGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(12.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hz WAVEFORM OSCILLATOR",
                        color = CyberLightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MODE: MIC SAMPLING",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val amplitude = (smoothDb - 30) * 0.8f // amplify scale based on dBSPL
                        val pointsCount = 120
                        val stepX = size.width / pointsCount
                        val midY = size.height / 2

                        val waveBrush = Brush.linearGradient(
                            colors = listOf(NeonCyan, NeonPurple, NeonRed),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f)
                        )

                        var prevX = 0f
                        var prevY = midY

                        for (i in 0..pointsCount) {
                            val x = i * stepX
                            // Simulate multiple waves overlayed
                            val angle1 = (i.toFloat() / pointsCount.toFloat()) * 4f * Math.PI.toFloat() + phaseOffset
                            val angle2 = (i.toFloat() / pointsCount.toFloat()) * 9f * Math.PI.toFloat() - phaseOffset
                            
                            val y = midY + (if (isRecording) (sin(angle1) * amplitude * 0.6f + sin(angle2) * amplitude * 0.3f).toFloat() else 0f)

                            if (i > 0) {
                                drawLine(
                                    brush = waveBrush,
                                    start = Offset(prevX, prevY),
                                    end = Offset(x, y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            prevX = x
                            prevY = y
                        }
                    }
                }

                // Advisory card matching acoustic guidelines
                Text(
                    text = viewModel.getDbAdvisory(smoothDb),
                    color = if (smoothDb > 85f) NeonRed else Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Toggle Audio Radar Control System panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "RADAR POWER SWITCH",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopSampling()
                            } else {
                                // Request permission or utilize simulation fallback
                                scope.launch {
                                    micPermissionState.launchPermissionRequest()
                                    viewModel.startSampling(micPermissionState.status.isGranted)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) NeonRed else NeonCyan
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("power_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CyberBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "OFFLINE TERMINATE" else "ACTIVATE AUDIO RADAR",
                                color = CyberBlack,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Log sound value button
                    Button(
                        onClick = {
                            scope.launch {
                                locationPermissionState.launchPermissionRequest()
                                viewModel.fetchActualLocation(locationPermissionState.status.isGranted)
                                viewModel.logSoundReadingToHistoricalMap()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        enabled = isRecording,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("log_coordinate_button"),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOG GRID",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
