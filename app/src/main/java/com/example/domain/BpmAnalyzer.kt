package com.example.domain

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object BpmAnalyzer {

    @SuppressLint("MissingPermission")
    suspend fun analyzeBpm(): Int = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
        
        val buffer = ShortArray(minBufferSize)
        recorder.startRecording()
        
        // Analyze for 3 seconds
        val startTime = System.currentTimeMillis()
        val peaks = mutableListOf<Long>()
        var lastPeakTime = 0L
        
        while (System.currentTimeMillis() - startTime < 3000) {
            val read = recorder.read(buffer, 0, minBufferSize)
            if (read > 0) {
                // Simple peak detection (Energy)
                var energy = 0L
                for (s in buffer) {
                    energy += abs(s.toLong())
                }
                energy /= read
                
                if (energy > 500) { // Threshold
                    val now = System.currentTimeMillis()
                    if (now - lastPeakTime > 300) { // Min interval
                        peaks.add(now)
                        lastPeakTime = now
                    }
                }
            }
        }
        
        recorder.stop()
        recorder.release()
        
        if (peaks.size < 2) return@withContext 120 // Fallback
        
        var totalInterval = 0L
        for (i in 1 until peaks.size) {
            totalInterval += (peaks[i] - peaks[i - 1])
        }
        val avgInterval = totalInterval / (peaks.size - 1)
        
        return@withContext (60000 / avgInterval.toInt()).coerceIn(60, 180)
    }
}
