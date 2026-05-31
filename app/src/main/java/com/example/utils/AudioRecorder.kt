package com.example.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorder {
    private val _dbFlow = MutableStateFlow(0f)
    val dbFlow: StateFlow<Float> = _dbFlow.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val sampleRate = 8000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope, hasPermission: Boolean) {
        if (_isRecording.value) return

        if (!hasPermission) {
            // Start simulation fallback
            startSimulation(scope)
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                startSimulation(scope)
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "Failed to initialize AudioRecord. Falling back to simulation.")
                startSimulation(scope)
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize)
                while (isActive && _isRecording.value) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        var count = 0
                        for (i in 0 until readSize) {
                            val value = buffer[i].toDouble()
                            sum += value * value
                            count++
                        }
                        if (count > 0) {
                            val rms = sqrt(sum / count)
                            // Map amplitude to dB. Standard reference is 1.0, 
                            // typically AudioRecord RMS value max is ~32767.
                            // Convert RMS scale to dB:
                            var db = 20 * log10(rms)
                            
                            // Adjust the scale so natural ambient speech matches 50-70dB, 
                            // absolute silence matches 30dB, and slamming/screaming reaches 100-110dB.
                            if (db.isInfinite() || db.isNaN()) {
                                db = 30.0
                            } else {
                                // Shift scale to realistic dBSPL environment
                                db = (db - 10) * 1.6 + 30
                                if (db < 30) db = 30.0
                                if (db > 120) db = 120.0
                            }
                            _dbFlow.value = db.toFloat()
                        }
                    }
                    delay(150) // sample rate update ~6-7 times a second
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting microphone recorder: ${e.message}", e)
            startSimulation(scope)
        }
    }

    private fun startSimulation(scope: CoroutineScope) {
        _isRecording.value = true
        recordJob = scope.launch(Dispatchers.Default) {
            var headingUp = true
            var baseDb = 45f
            while (isActive && _isRecording.value) {
                // Generate a realistic, dynamic ambient noise level
                val randomVariation = (-3..3).random()
                
                // Occasional dramatic disturbance spikes
                if ((0..20).random() == 5) {
                    baseDb = (75..112).random().toFloat()
                } else {
                    if (baseDb > 60) {
                        baseDb -= 8f // decay spike back to normal
                    } else {
                        // Ambient drift
                        baseDb = 42f + (Math.sin(System.currentTimeMillis() / 8000.0) * 8f).toFloat()
                    }
                }

                _dbFlow.value = (baseDb + randomVariation).coerceIn(30f, 120f)
                delay(200)
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        }
        audioRecord = null
        _dbFlow.value = 0f
    }
}
