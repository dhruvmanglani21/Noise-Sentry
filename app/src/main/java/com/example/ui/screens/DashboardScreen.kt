package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoiseReportEntity
import com.example.data.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: NoiseSentryViewModel,
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val reports by viewModel.allReports.collectAsState()
    val readings by viewModel.recentReadings.collectAsState()

    val safeUser = currentUser ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 16.dp)
            .testTag("dashboard_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Technological User Level HUD Header
        item {
            UserHudCard(user = safeUser)
        }

        // Biometric Security Controller Gate Card
        item {
            com.example.ui.screens.BiometricGateSecurityCard(viewModel = viewModel)
        }

        // Real-time Safety Level Metric Grid
        item {
            RealTimePulseMetrics(reports = reports, averageDb = readings.map { it.dbLevel }.average())
        }

        // Quick Command Console Row
        item {
            Text(
                text = "> SYSTEM NAVIGATION PANEL",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.setScreen("meter") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("nav_meter_btn")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE RADAR", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.setScreen("reports") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("nav_reports_btn")
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOG INCIDENT", color = NeonPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Frequent Disturbances Section
        item {
            FrequentDisturbanceWidget(reports = reports)
        }

        // Unlocked Badges Summary
        item {
            AgentBadgeShowcase(points = safeUser.points)
        }

        // Live Municipal Incidents Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> LIVE LOGGED INCIDENTS FEED",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${reports.size} Reports active",
                    color = CyberLightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Items of Reports list
        if (reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreenSafe, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ALL SECTORS QUIET", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("No high decibel disruptions registered to community grid.", color = CyberLightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        } else {
            items(reports.take(5)) { report ->
                ReportListItem(report = report)
            }
        }
    }
}

@Composable
fun UserHudCard(user: UserEntity) {
    val progress = (user.points % 300) / 300f
    val levelXpMax = 300
    val currentXpInLevel = user.points % 300

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow Avatar Badge Bubble
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .border(2.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarIcon = when (user.avatar) {
                        "cyber_sentry" -> Icons.Default.Security
                        "sonic_scout" -> Icons.Default.Sensors
                        "decibel_ranger" -> Icons.Default.Gavel
                        else -> Icons.Default.SelfImprovement
                    }
                    Icon(avatarIcon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFICIAL COMMUNITY SENTRY",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                    }
                }

                // Level badge indicator
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "LVL",
                        color = CyberLightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = user.level.toString(),
                        color = NeonYellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL EXP: $currentXpInLevel / $levelXpMax XP",
                    color = CyberLightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TOTAL: ${user.points} XP",
                    color = NeonPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = CyberGray
            )
        }
    }
}

@Composable
fun RealTimePulseMetrics(reports: List<NoiseReportEntity>, averageDb: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ambient Avg Card
        Card(
            modifier = Modifier.weight(1.0f).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Default.VolumeDown, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("SCAN AVERAGE", color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                val finalAv = if (averageDb.isNaN() || averageDb == 0.0) 53.5 else averageDb
                Text("${String.format("%.1f", finalAv)} dB", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Optimized Target", color = CyberGreenSafe, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Active Disruptions Index
        Card(
            modifier = Modifier.weight(1.0f).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("DISTURBANCES", color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                val activeReportsCount = reports.filter { it.status != "Resolved" }.size
                Text("$activeReportsCount Pending", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Severity Index: HI-X1", color = NeonYellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Safety Score
        Card(
            modifier = Modifier.weight(1.0f).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = CyberGreenSafe, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("GRID STATUS", color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                val score = (100 - (reports.filter { it.status != "Resolved" }.size * 3.5)).coerceAtLeast(65.0)
                Text("${score.toInt()}% SECURE", color = if (score > 85) CyberGreenSafe else NeonYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Safe Exposure Level", color = CyberLightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun FrequentDisturbanceWidget(reports: List<NoiseReportEntity>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "> FREQUENT COMMERCE DISTURBANCES",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Build categories group
            val categories = listOf("Traffic", "Construction", "Machinery", "Party", "Other")
            for (cat in categories) {
                val catReports = reports.filter { it.category.equals(cat, true) }
                val pct = if (reports.isEmpty()) 0.2f else (catReports.size.toFloat() / reports.size.toFloat()).coerceAtLeast(0.1f)
                val barColor = if (cat == "Construction" || cat == "Machinery") NeonRed else if (cat == "Traffic") NeonYellow else NeonCyan

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(90.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(CyberGray, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct)
                                .background(barColor, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${catReports.size} zone log",
                        color = CyberLightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentBadgeShowcase(points: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "> UNLOCKED SENTRY BADGES",
                color = NeonPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Badge 1: Cadet
                BadgeItem(isUnlocked = points >= 100, label = "Grid Cadet", symbol = "🛡️")
                // Badge 2: Cartographer
                BadgeItem(isUnlocked = points >= 300, label = "Sonic Mapper", symbol = "📡")
                // Badge 3: Sentinel
                BadgeItem(isUnlocked = points >= 600, label = "Shield Captain", symbol = "⚔️")
                // Badge 4: Sound Master
                BadgeItem(isUnlocked = points >= 1200, label = "Silence Gladiator", symbol = "🧘")
            }
        }
    }
}

@Composable
fun BadgeItem(isUnlocked: Boolean, label: String, symbol: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) NeonPurple.copy(alpha = 0.15f) else CyberBlack)
                .border(2.dp, if (isUnlocked) NeonPurple else CyberGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = symbol, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isUnlocked) Color.White else CyberLightGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ReportListItem(report: NoiseReportEntity) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
    
    val sevColor = when (report.severity) {
        "Critical" -> NeonRed
        "High" -> NeonYellow
        "Medium" -> NeonCyan
        else -> CyberGreenSafe
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(sevColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INCIDENT #${report.id} [${report.category.uppercase()}]",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (report.status == "Resolved") CyberGreenSafe.copy(alpha = 0.15f) else NeonYellow.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = report.status.uppercase(),
                        color = if (report.status == "Resolved") CyberGreenSafe else NeonYellow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Geometrical coordinates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.address} (${String.format("%.4f", report.lat)}, ${String.format("%.4f", report.lng)})",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = report.comment,
                color = CyberLightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Timestamps and attachments
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Incident Date",
                        tint = CyberLightGray,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (report.incidentDate.isNotEmpty()) report.incidentDate else dateStr.substringBefore(","),
                        color = CyberLightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Incident Time",
                        tint = CyberLightGray,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (report.incidentTime.isNotEmpty()) report.incidentTime else dateStr.substringAfter(", "),
                        color = CyberLightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (report.audioEvidencePath != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(8.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("AUDIO", color = NeonCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (report.photoEvidencePath != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(8.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("PHOTO", color = NeonCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            HorizontalDivider(color = CyberGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported by: ${report.userFullName}",
                    color = CyberLightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${report.dbLevel} dB Intensity",
                    color = sevColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BiometricGateSecurityCard(viewModel: NoiseSentryViewModel) {
    val enabled by viewModel.biometricEnabled.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.Fingerprint else Icons.Default.LockOpen,
                        contentDescription = "Lock Status",
                        tint = if (enabled) NeonCyan else CyberLightGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BIOMETRIC SENTRY PROTOCOLS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (enabled) "SECURED - SHIELD COVERAGE ENGAGED" else "UNSHIELDED - OPEN TERMINAL ACCESS",
                            color = if (enabled) CyberGreenSafe else NeonYellow,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                        uncheckedThumbColor = CyberGray,
                        uncheckedTrackColor = CyberBlack
                    ),
                    modifier = Modifier.testTag("biometric_lock_toggle")
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, CyberGray, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORCE MANNED LOCKDOWN:",
                        color = CyberLightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = { viewModel.setBiometricLocked(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("force_lock_btn")
                    ) {
                        Text("LOCK SENTRY DEVICE", color = NeonRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

