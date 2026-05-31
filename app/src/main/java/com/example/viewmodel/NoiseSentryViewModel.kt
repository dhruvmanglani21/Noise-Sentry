package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.AudioRecorder
import com.example.utils.NotificationHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class NoiseSentryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = NoiseRepository(db)

    // Auth state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Biometric Security States
    private val _isBiometricLocked = MutableStateFlow(true)
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(true)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow("dashboard") // "dashboard", "meter", "map", "reports", "gamify", "export"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Sound Meter
    private val audioRecorder = AudioRecorder()
    val liveDb: StateFlow<Float> = audioRecorder.dbFlow
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording

    // Reactive DB collections
    val allReports: StateFlow<List<NoiseReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentReadings: StateFlow<List<NoiseReadingEntity>> = repository.recentReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<UserEntity>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live coordinates
    private val _currentLat = MutableStateFlow(25.1193) // Dubai mock center base
    val currentLat: StateFlow<Double> = _currentLat.asStateFlow()

    private val _currentLng = MutableStateFlow(55.3773)
    val currentLng: StateFlow<Double> = _currentLng.asStateFlow()

    // Location tracker client
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Alerts tracking
    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    init {
        // Init notification channel
        NotificationHelper.initNotificationChannel(application)
        
        // Seed mock database records
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
        }

        // Monitor live decibels for trigger alerts (exceeds 85dB triggers local warning)
        viewModelScope.launch {
            var lastAlertTime = 0L
            liveDb.collect { dbVal ->
                if (dbVal >= 88f) {
                    val now = System.currentTimeMillis()
                    // Throttle notification triggers by 15 seconds to avoid spamming alerts
                    if (now - lastAlertTime > 15000L) {
                        lastAlertTime = now
                        val locationName = getApproximateLocationName(currentLat.value, currentLng.value)
                        triggerNoiseAlert(locationName, dbVal.toInt())
                    }
                }
            }
        }
    }

    // --- Authentication Actions ---
    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun loginUser(userNameInput: String, passwordInput: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val trimUser = userNameInput.trim().lowercase()
            if (trimUser.isEmpty() || passwordInput.isEmpty()) {
                _authError.value = "Username and password fields are required!"
                return@launch
            }

            val fetched = repository.getUserByUsername(trimUser)
            if (fetched == null) {
                // If it doesn't exist, let's create a new account for easier testing and quick login
                val newUser = UserEntity(
                    username = trimUser,
                    passwordHash = passwordInput, // simulated MD5/SHA representation
                    fullName = userNameInput.replaceFirstChar { it.uppercase() } + " Scout",
                    points = 100, // Starter bonus
                    level = 1,
                    avatar = getRandomCyberAvatar()
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                onComplete()
            } else {
                if (fetched.passwordHash == passwordInput) {
                    _currentUser.value = fetched
                    onComplete()
                } else {
                    _authError.value = "Invalid credentials. Please attempt again."
                }
            }
        }
    }

    fun logout() {
        stopSampling()
        _currentUser.value = null
        _currentScreen.value = "dashboard"
    }

    // --- Location Actions ---
    fun updateSimulatedLocation(lat: Double, lng: Double) {
        _currentLat.value = lat
        _currentLng.value = lng
    }

    fun fetchActualLocation(hasPermission: Boolean) {
        if (!hasPermission) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    _currentLat.value = loc.latitude
                    _currentLng.value = loc.longitude
                    Log.d("NoiseSentryVM", "Updated coordinates dynamically: ${loc.latitude}, ${loc.longitude}")
                }
            }
        } catch (e: SecurityException) {
            Log.e("NoiseSentryVM", "Error getting physical GPS: ${e.message}")
        }
    }

    // --- Audio Sampling Actions ---
    fun startSampling(hasPermission: Boolean) {
        viewModelScope.launch {
            audioRecorder.startRecording(viewModelScope, hasPermission)
        }
    }

    fun stopSampling() {
        audioRecorder.stopRecording()
    }

    // Capture and save current sound meter decibels to historical map list
    fun logSoundReadingToHistoricalMap() {
        val user = _currentUser.value ?: return
        val dbLevelVal = liveDb.value
        if (dbLevelVal < 31f) return // not running or zero background

        viewModelScope.launch {
            val reading = NoiseReadingEntity(
                userId = user.username,
                lat = currentLat.value,
                lng = currentLng.value,
                dbLevel = dbLevelVal.toDouble()
            )
            repository.insertReading(reading)

            // Reward gamification experience points: +15 XP for logging a real-time coordinate decibel reading!
            rewardXpBonus(15)
            
            _alertMessage.value = "🔊 Logged ${dbLevelVal.toInt()} dB to global coverage grid! You gained +15 XP"
        }
    }

    // --- Report Management Actions ---
    fun submitNoiseReport(
        category: String,
        address: String,
        comment: String,
        estDecibel: Int,
        incidentDate: String,
        incidentTime: String,
        audioEvidencePath: String?,
        photoEvidencePath: String?,
        customLat: Double? = null,
        customLng: Double? = null,
        onComplete: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val sev = when {
                estDecibel < 65 -> "Low"
                estDecibel < 80 -> "Medium"
                estDecibel < 95 -> "High"
                else -> "Critical"
            }

            val report = NoiseReportEntity(
                userId = user.username,
                userFullName = user.fullName,
                lat = customLat ?: currentLat.value,
                lng = customLng ?: currentLng.value,
                address = address.ifEmpty { "Community Perimeter Grid" },
                dbLevel = estDecibel,
                category = category,
                comment = comment.ifEmpty { "Spontaneous sound variance logged by sentry user." },
                severity = sev,
                status = "Logged",
                incidentDate = incidentDate,
                incidentTime = incidentTime,
                audioEvidencePath = audioEvidencePath,
                photoEvidencePath = photoEvidencePath
            )
            repository.insertReport(report)

            // Reward substantial XP for creating reports: +150 Points!
            rewardXpBonus(150)
            
            _alertMessage.value = "📜 Noise Incident Registered! Saved to local authorities table. You gained +150 XP!"
            onComplete()
        }
    }

    fun dismissAlertMessage() {
        _alertMessage.value = null
    }

    // --- Gamification Logic ---
    private suspend fun rewardXpBonus(pts: Int) {
        val user = _currentUser.value ?: return
        val currentPoints = user.points + pts
        // XP level up curve: Level = 1 + (Points / 300)
        val calculatedLevel = 1 + (currentPoints / 300)
        
        val updatedUser = user.copy(
            points = currentPoints,
            level = calculatedLevel.coerceAtLeast(user.level)
        )
        repository.insertUser(updatedUser)
        _currentUser.value = updatedUser
    }

    private fun getRandomCyberAvatar(): String {
        val avs = listOf("cyber_sentry", "sonic_scout", "decibel_ranger", "silence_monk")
        return avs.random()
    }

    // --- Sound Level Descriptive helpers ---
    fun getDbAdvisory(db: Float): String {
        return when {
            db < 45f -> "🟢 Peace Corridor: Background level is safe for sleeping and concentration."
            db < 60f -> "🟡 Active Office: Medium conversational noise levels. Minimal disturbance index."
            db < 75f -> "🟠 Disruptive Area: Mild annoyance index. Disturbance parameters detected."
            db < 90f -> "🔴 Acoustic Exhaustion Hazard: Extended exposure risk. Relocate or isolate noise."
            else -> "💀 EXTREME HAZARD ZONE: Critical exposure. Hearing protection parameters mandatory!"
        }
    }

    private fun getApproximateLocationName(lat: Double, lng: Double): String {
        // Convert coords into friendly localized terms
        val dLat = Math.abs(lat - 25.1193)
        val dLng = Math.abs(lng - 55.3773)
        return when {
            dLat < 0.003 && dLng < 0.003 -> "Silicon Hub Central Core"
            lat > 25.1193 && lng > 55.3773 -> "Commercial North Sector"
            lat < 25.1193 && lng > 55.3773 -> "East Residential Ring"
            lat > 25.1193 && lng < 55.3773 -> "Industrial Transit Terminus"
            else -> "Outer Sentry Sector"
        }
    }

    // Trigger Android notifications and local alerts
    private fun triggerNoiseAlert(location: String, dbValue: Int) {
        viewModelScope.launch {
            // Write alert record to readings list automatically to map the high-decibel spike on the visual chart!
            repository.insertReading(
                NoiseReadingEntity(
                    userId = "SYSTEM_WATCH",
                    lat = currentLat.value,
                    lng = currentLng.value,
                    dbLevel = dbValue.toDouble()
                )
            )
            // Push actual system notification 
            NotificationHelper.sendNoiseAlert(getApplication(), location, dbValue)
            _alertMessage.value = "⚠️ ALARM SENSORS TRIGGERED: Noise disturbance detected in $location (${dbValue}dB)! Notification triggered."
        }
    }

    // --- Export Data for Authorities ---
    // Generates a fully formatted CSV or JSON file in documents/cache directory and returns the absolute file path and data string.
    fun exportDataForAuthorities(context: Context, format: String): File? {
        val reportsList = allReports.value
        val readingsList = recentReadings.value
        
        try {
            val filename = "NOISE_SENTRY_METRICS_${System.currentTimeMillis()}.${format.lowercase()}"
            val file = File(context.cacheDir, filename)
            val outputStream = FileOutputStream(file)

            if (format.uppercase() == "JSON") {
                val rootJson = JSONObject()
                rootJson.put("app_identity", "Noise Sentry Sound Protection Platform")
                rootJson.put("export_timestamp", System.currentTimeMillis())
                rootJson.put("total_reports", reportsList.size)
                
                val repArray = JSONArray()
                for (r in reportsList) {
                    val rObj = JSONObject()
                    rObj.put("report_id", r.id)
                    rObj.put("report_user", r.userFullName)
                    rObj.put("latitude", r.lat)
                    rObj.put("longitude", r.lng)
                    rObj.put("address", r.address)
                    rObj.put("db_level", r.dbLevel)
                    rObj.put("category", r.category)
                    rObj.put("comment", r.comment)
                    rObj.put("severity", r.severity)
                    rObj.put("status", r.status)
                    rObj.put("reported_date_ms", r.timestamp)
                    rObj.put("incident_date", r.incidentDate)
                    rObj.put("incident_time", r.incidentTime)
                    rObj.put("audio_evidence", r.audioEvidencePath ?: "None")
                    rObj.put("photo_evidence", r.photoEvidencePath ?: "None")
                    repArray.put(rObj)
                }
                rootJson.put("community_reports", repArray)

                val readArray = JSONArray()
                for (rd in readingsList) {
                    val rdObj = JSONObject()
                    rdObj.put("log_id", rd.id)
                    rdObj.put("user_id", rd.userId)
                    rdObj.put("latitude", rd.lat)
                    rdObj.put("longitude", rd.lng)
                    rdObj.put("db_level", rd.dbLevel)
                    rdObj.put("logged_date_ms", rd.timestamp)
                    readArray.put(rdObj)
                }
                rootJson.put("realtime_sound_scans", readArray)

                outputStream.write(rootJson.toString(4).toByteArray())
            } else {
                // Export as CSV
                val csvBuilder = StringBuilder()
                csvBuilder.append("RECORD_TYPE,ID,USER,LATITUDE,LONGITUDE,DB_LEVEL,CATEGORY_OR_USER,STATUS_OR_EMPTY,TIMESTAMP,INCIDENT_DATE,INCIDENT_TIME,AUDIO_EVIDENCE,PHOTO_EVIDENCE,COMMENT_OR_INFO\n")
                
                for (r in reportsList) {
                    csvBuilder.append("REPORT,${r.id},\"${r.userFullName}\",${r.lat},${r.lng},${r.dbLevel},\"${r.category}\",\"${r.status}\",${r.timestamp},\"${r.incidentDate}\",\"${r.incidentTime}\",\"${r.audioEvidencePath ?: ""}\",\"${r.photoEvidencePath ?: ""}\",\"${r.comment.replace("\"", "'")}\"\n")
                }
                for (rd in readingsList) {
                    csvBuilder.append("SOUND_SCAN,${rd.id},\"${rd.userId}\",${rd.lat},${rd.lng},${rd.dbLevel},N/A,N/A,${rd.timestamp},\"Dynamic grid mapping record\"\n")
                }
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            outputStream.flush()
            outputStream.close()
            Log.d("NoiseSentryVM", "Successfully exported system statistics to path: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("NoiseSentryVM", "Error compiling file export: ${e.message}", e)
            return null
        }
    }

    fun setBiometricLocked(locked: Boolean) {
        _isBiometricLocked.value = locked
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        if (!enabled) {
            _isBiometricLocked.value = false
        }
    }
}
