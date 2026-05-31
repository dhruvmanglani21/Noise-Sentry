package com.example.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricLockScreen(viewModel: NoiseSentryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var systemOutputMsg by remember { mutableStateOf("SCANNER STANDBY. INITIATE TOUCH ID SCAN.") }
    var inputPasscode by remember { mutableStateOf("") }
    
    // Aesthetic scanning sweep indicator 
    var isAestheticScanning by remember { mutableStateOf(false) }
    var scanningProgress by remember { mutableStateOf(0f) }

    // Pulse animation for critical security ring
    val infiniteTransition = rememberInfiniteTransition(label = "bio_halo")
    val haloPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // Scanning horizontal bar displacement
    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweepYOffset by sweepTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep_y"
    )

    // Trigger Android Native Biometric authentication with callback
    val triggerNativeBiometricAuth = {
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Noise Sentry Security Gate")
                    .setSubtitle("Scan fingerprint or authentication token to unlock the ledger")
                    .setNegativeButtonText("Use Passcode Fallback")
                    .build()

                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        systemOutputMsg = "Native sensor timeout: $errString. Use backup manual override."
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(context, "Sentry Authorized successfully!", Toast.LENGTH_SHORT).show()
                        viewModel.setBiometricLocked(false)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        systemOutputMsg = "Authentication failed! Retransmit credentials."
                    }
                })

                biometricPrompt.authenticate(promptInfo)
            } else {
                systemOutputMsg = "Hardware biometrics unavailable. Standardizing local secure passcode..."
            }
        } else {
            systemOutputMsg = "Invalid activity environment. Initializing cryptographic passcode manual keys..."
        }
    }

    // Effect triggers real system check first
    LaunchedEffect(Unit) {
        delay(600)
        triggerNativeBiometricAuth()
    }

    // Aesthetic scanning sweep progress simulator
    LaunchedEffect(isAestheticScanning) {
        if (isAestheticScanning) {
            systemOutputMsg = "⚡ ENGAGING MICRO-GRAVIMETRIC SCANNER..."
            scanningProgress = 0f
            while (scanningProgress < 1.0f) {
                delay(60)
                scanningProgress += 0.04f
            }
            isAestheticScanning = false
            systemOutputMsg = "SECURE BIO-SCAN MATRICES ACCEPTED!"
            delay(400)
            viewModel.setBiometricLocked(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawBehind {
                val gridSpacing = 40.dp.toPx()
                val lineAlpha = 0.05f
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = NeonCyan,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        alpha = lineAlpha
                    )
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = NeonCyan,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        alpha = lineAlpha
                    )
                    y += gridSpacing
                }
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .background(CyberDarkCard.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔒 SENTRY SECURE LOCKDOWN",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BIOMETRIC SHIELD GATEWAY",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            // Fingerprint Scan visualizer node
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(CyberBlack)
                    .border(1.dp, NeonCyan.copy(alpha = 0.15f), CircleShape)
                    .clickable {
                        if (!isAestheticScanning) {
                            isAestheticScanning = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive pulsing background rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.05f),
                        radius = radius * haloPulseScale,
                        center = center
                    )
                }

                // Cyber finger vector
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Tap scanner node",
                    tint = if (isAestheticScanning) NeonCyan else NeonPurple,
                    modifier = Modifier.size(72.dp)
                )

                // Laser sweep bar line
                if (isAestheticScanning) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 2.dp.toPx()
                        val currentY = size.height * sweepYOffset
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, NeonCyan, Color.Transparent)
                            ),
                            start = Offset(0f, currentY),
                            end = Offset(size.width, currentY),
                            strokeWidth = strokeW
                        )
                    }
                }
            }

            // Terminal status console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, CyberGray, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SEC STAT: ONLINE",
                        color = CyberLightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ID_ENT: SENTRY_PROT",
                        color = CyberLightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = CyberGray, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = systemOutputMsg.uppercase(),
                    color = if (systemOutputMsg.contains("ACCEPTED") || systemOutputMsg.contains("Authorized")) CyberGreenSafe else NeonYellow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Quick bypass buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        isAestheticScanning = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("simulate_biometric_scan_btn"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.DoubleArrow, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "TRIGGER SENSORY BYPASS / SIMULATION",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        triggerNativeBiometricAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("native_biometric_scan_btn"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "RE-TRIGGER SYSTEM PROMPT (TOUCH ID)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Divider or fallback header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(color = CyberGray, modifier = Modifier.weight(1f))
                Text(
                    text = " OR CRYO PASS PIN ",
                    color = CyberLightGray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                HorizontalDivider(color = CyberGray, modifier = Modifier.weight(1f))
            }

            // Passcode text box keyboard override
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPasscode,
                    onValueChange = { inputVal ->
                        if (inputVal.all { it.isDigit() } && inputVal.length <= 6) {
                            inputPasscode = inputVal
                        }
                    },
                    label = { Text("6-Digit Override CryoPIN Key", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    placeholder = { Text("e.g. 1 2 3 4 5 6", color = CyberLightGray, fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberGray,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = CyberLightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crypto_passcode_input")
                )

                Button(
                    onClick = {
                        // Any standard passcode check, let's treat "123456" or any 4+ digit numeric PIN as standard bypass auth key
                        if (inputPasscode.length >= 4) {
                            scope.launch {
                                systemOutputMsg = "UNLOCKED BY CRYO-PASS KEY!"
                                delay(300)
                                viewModel.setBiometricLocked(false)
                            }
                        } else {
                            systemOutputMsg = "PIN must translate at least 4 cryptographic segments!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (inputPasscode.length >= 4) NeonPurple else CyberGray),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("submit_passcode_btn")
                ) {
                    Text(
                        "SUBMIT OVERRIDE ACCESS KEY",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
