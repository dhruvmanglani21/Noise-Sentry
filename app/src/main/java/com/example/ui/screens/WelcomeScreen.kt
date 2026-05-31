package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: NoiseSentryViewModel,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()
    
    // Grid aesthetic background animation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawBehind {
                val gridSpacing = 40.dp.toPx()
                val lineAlpha = 0.07f
                // Vertical grid lines
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
                // Horizontal grid lines
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
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .background(CyberDarkCard.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sentry logo graphic icon
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Status indicator",
                tint = NeonCyan,
                modifier = Modifier
                    .size(72.dp)
                    .drawBehind {
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.15f),
                            radius = size.width,
                        )
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NOISE SENTRY",
                fontSize = 28.sp,
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Community Sound Shield",
                fontSize = 12.sp,
                color = CyberLightGray,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            if (authError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "error", tint = NeonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authError ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Agent ID / Email", fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberGray,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = CyberLightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Secured Passcode", fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberGray,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = CyberLightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.loginUser(username, password, onLoginSuccess)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Login Key",
                        tint = CyberBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INITIALIZE BIOMETRIC LOG",
                        color = CyberBlack,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "* Typing a new Agent passcode automatically creates your local user files on our secure decentral cluster.",
                fontSize = 11.sp,
                color = CyberLightGray,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
