package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val fullName: String,
    val points: Int = 0,
    val level: Int = 1,
    val avatar: String = "sonic_scout", // "cyber_sentry", "sonic_scout", "decibel_ranger", "silence_monk"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "noise_reports")
data class NoiseReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userFullName: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val dbLevel: Int,
    val category: String, // "Traffic", "Construction", "Machinery", "Party", "Other"
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: String, // "Low" (< 60dB), "Medium" (60-80dB), "High" (80-100dB), "Critical" (> 100dB)
    val status: String = "Logged", // "Logged", "Reviewing", "Resolved"
    val incidentDate: String = "",
    val incidentTime: String = "",
    val audioEvidencePath: String? = null,
    val photoEvidencePath: String? = null
)

@Entity(tableName = "noise_readings")
data class NoiseReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val dbLevel: Double,
    val timestamp: Long = System.currentTimeMillis()
)
