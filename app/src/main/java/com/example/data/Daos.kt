package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY points DESC LIMIT 10")
    fun getLeaderboard(): Flow<List<UserEntity>>
}

@Dao
interface NoiseReportDao {
    @Query("SELECT * FROM noise_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<NoiseReportEntity>>

    @Query("SELECT * FROM noise_reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReportsByUser(userId: String): Flow<List<NoiseReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: NoiseReportEntity)

    @Query("UPDATE noise_reports SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: Int, status: String)

    @Query("DELETE FROM noise_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)
}

@Dao
interface NoiseReadingDao {
    @Query("SELECT * FROM noise_readings ORDER BY timestamp DESC LIMIT 200")
    fun getRecentReadings(): Flow<List<NoiseReadingEntity>>

    @Query("SELECT * FROM noise_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReadingsByUser(userId: String): Flow<List<NoiseReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: NoiseReadingEntity)

    @Query("SELECT AVG(dbLevel) FROM noise_readings WHERE timestamp >= :sinceTimestamp")
    fun getAverageDbFlow(sinceTimestamp: Long): Flow<Double?>

    @Query("DELETE FROM noise_readings")
    suspend fun clearAllReadings()
}
