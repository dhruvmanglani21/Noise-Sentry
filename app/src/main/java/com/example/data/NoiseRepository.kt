package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class NoiseRepository(private val db: AppDatabase) {
    val userDao = db.userDao()
    val noiseReportDao = db.noiseReportDao()
    val noiseReadingDao = db.noiseReadingDao()

    // Users
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    val leaderboard: Flow<List<UserEntity>> = userDao.getLeaderboard()

    // Reports
    val allReports: Flow<List<NoiseReportEntity>> = noiseReportDao.getAllReports()
    fun getReportsByUser(userId: String): Flow<List<NoiseReportEntity>> = noiseReportDao.getReportsByUser(userId)
    suspend fun insertReport(report: NoiseReportEntity) = noiseReportDao.insertReport(report)
    suspend fun updateReportStatus(id: Int, status: String) = noiseReportDao.updateReportStatus(id, status)
    suspend fun deleteReportById(id: Int) = noiseReportDao.deleteReportById(id)

    // Readings
    val recentReadings: Flow<List<NoiseReadingEntity>> = noiseReadingDao.getRecentReadings()
    fun getReadingsByUser(userId: String): Flow<List<NoiseReadingEntity>> = noiseReadingDao.getReadingsByUser(userId)
    suspend fun insertReading(reading: NoiseReadingEntity) = noiseReadingDao.insertReading(reading)
    fun getAverageDb(sinceTimestamp: Long): Flow<Double?> = noiseReadingDao.getAverageDbFlow(sinceTimestamp)
    suspend fun clearAllReadings() = noiseReadingDao.clearAllReadings()

    // Generate starter reports/readings for the area (Dubai/Silicon Valley/City) if none exist.
    suspend fun seedMockDataIfEmpty() {
        // Seed default accounts if needed
        val testUser = userDao.getUserByUsername("admin")
        if (testUser == null) {
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    passwordHash = "admin123",
                    fullName = "City Captain",
                    points = 2450,
                    level = 15,
                    avatar = "cyber_sentry"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "dhruv",
                    passwordHash = "secure123",
                    fullName = "Dhruv Manglani",
                    points = 950,
                    level = 6,
                    avatar = "sonic_scout"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "sentry_bot",
                    passwordHash = "botpass",
                    fullName = "Core Sentry Sentinel",
                    points = 1800,
                    level = 11,
                    avatar = "decibel_ranger"
                )
            )
        }

        // Seeds for noise reports if db is empty
        val reports = noiseReportDao.getAllReports().firstOrNull()
        if (reports.isNullOrEmpty()) {
            val seedReports = listOf(
                NoiseReportEntity(
                    userId = "admin",
                    userFullName = "City Captain",
                    lat = 25.1193, // Center area coords (Dubai mock)
                    lng = 55.3773,
                    address = "Downtown Industrial Zone - Junction 4",
                    dbLevel = 95,
                    category = "Machinery",
                    comment = "Persistent high-decibel hum from cooling exhaust vent 24/7.",
                    severity = "High",
                    status = "Reviewing",
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                NoiseReportEntity(
                    userId = "admin",
                    userFullName = "City Captain",
                    lat = 25.1278,
                    lng = 55.3900,
                    address = "Metro Interchange - Near North Gate",
                    dbLevel = 88,
                    category = "Traffic",
                    comment = "Screeching brakes during peak transition hours.",
                    severity = "High",
                    status = "Logged",
                    timestamp = System.currentTimeMillis() - 14400000
                ),
                NoiseReportEntity(
                    userId = "sentry_bot",
                    userFullName = "Core Sentry Sentinel",
                    lat = 25.1011,
                    lng = 55.3620,
                    address = "Silicon Oasis Boulevard - Lot B Construction",
                    dbLevel = 105,
                    category = "Construction",
                    comment = "Excavator operating outside permitted noise exemption windows.",
                    severity = "Critical",
                    status = "Logged",
                    timestamp = System.currentTimeMillis() - 3600000
                ),
                NoiseReportEntity(
                    userId = "dhruv",
                    userFullName = "Dhruv Manglani",
                    lat = 25.1320,
                    lng = 55.3850,
                    address = "Commercial District - Night Lounge Alley",
                    dbLevel = 79,
                    category = "Party",
                    comment = "Exceeded sub-bass limit parameters after local curfew regulations.",
                    severity = "Medium",
                    status = "Resolved",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
            for (r in seedReports) {
                noiseReportDao.insertReport(r)
            }
        }

        // Seeds for noise readings (for heatmap / stats)
        val readings = noiseReadingDao.getRecentReadings().firstOrNull()
        if (readings.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            // Generate some sound log coordinates
            val baseLat = 25.115
            val baseLng = 55.375
            val seedReadings = listOf(
                NoiseReadingEntity(userId = "admin", lat = baseLat + 0.005, lng = baseLng + 0.002, dbLevel = 74.3, timestamp = now - 600000),
                NoiseReadingEntity(userId = "admin", lat = baseLat - 0.004, lng = baseLng + 0.008, dbLevel = 55.2, timestamp = now - 1200000),
                NoiseReadingEntity(userId = "dhruv", lat = baseLat + 0.012, lng = baseLng - 0.005, dbLevel = 92.1, timestamp = now - 1800000),
                NoiseReadingEntity(userId = "dhruv", lat = baseLat + 0.010, lng = baseLng - 0.003, dbLevel = 88.5, timestamp = now - 2400000),
                NoiseReadingEntity(userId = "sentry_bot", lat = baseLat - 0.010, lng = baseLng - 0.010, dbLevel = 61.4, timestamp = now - 3000000),
                NoiseReadingEntity(userId = "sentry_bot", lat = baseLat - 0.008, lng = baseLng - 0.008, dbLevel = 104.7, timestamp = now - 3600000),
                NoiseReadingEntity(userId = "admin", lat = baseLat + 0.001, lng = baseLng - 0.001, dbLevel = 45.0, timestamp = now - 4200000)
            )
            for (read in seedReadings) {
                noiseReadingDao.insertReading(read)
            }
        }
    }
}
