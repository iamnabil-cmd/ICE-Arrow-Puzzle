package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural crystalline audio synthesizer for Ice Arrow Puzzle.
 * Synthesizes crisp ice clicks, whooshes, fractures, and shatters with zero external latency.
 */
class SoundManager(context: Context, private val isSoundEnabled: () -> Boolean) {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    // Short recorded samples go through SoundPool for low-latency playback
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val arrowTapSoundId = soundPool.load(context, R.raw.arrow_tap, 1)

    /** Crisp phone-style tap click (the tap sound from the reference game). */
    fun playArrowTap() {
        if (!isSoundEnabled()) return
        soundPool.play(arrowTapSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }

    fun playArrowWhoosh() {
        if (!isSoundEnabled()) return
        scope.launch {
            // Gliding airy ice slide: frequency ramp 600Hz -> 1400Hz with smooth curve
            val durationMs = 280
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val envelope = sin(PI * progress) // smooth bell envelope
                val freq = 500.0 + 800.0 * (progress * progress)
                val sample = (sin(2 * PI * freq * t) * 0.7 + sin(2 * PI * (freq * 1.5) * t) * 0.3) * envelope
                buffer[i] = (sample * 16000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playIceCrack() {
        if (!isSoundEnabled()) return
        scope.launch {
            // High crystalline fracture snap with secondary dissonance
            val durationMs = 120
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 45.0)
                val noise = ((Math.random() - 0.5) * 0.4)
                val tone1 = sin(2 * PI * 2200.0 * t)
                val tone2 = sin(2 * PI * 3350.0 * t)
                val sample = (tone1 * 0.4 + tone2 * 0.3 + noise) * decay
                buffer[i] = (sample * 22000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playIceShatter() {
        if (!isSoundEnabled()) return
        scope.launch {
            // Deep resonant ice impact followed by sparkling crystal glass cascade
            val durationMs = 600
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val impactDecay = exp(-t * 20.0)
                val shimmerDecay = exp(-t * 7.0)

                // Resonant body frequencies (deep glacial block + glass shards)
                val lowImpact = sin(2 * PI * 320.0 * t) * impactDecay
                val midResonance = sin(2 * PI * 980.0 * t) * shimmerDecay
                val sparkle1 = sin(2 * PI * 2400.0 * t + sin(2 * PI * 12.0 * t)) * shimmerDecay
                val sparkle2 = sin(2 * PI * 4200.0 * t) * shimmerDecay
                val grain = (Math.random() - 0.5) * 0.3 * exp(-t * 15.0)

                val sample = (lowImpact * 0.4 + midResonance * 0.3 + sparkle1 * 0.2 + sparkle2 * 0.2 + grain)
                buffer[i] = (sample * 25000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playChiselHit() {
        if (!isSoundEnabled()) return
        scope.launch {
            // Metallic/ice pick strike
            val durationMs = 150
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 35.0)
                val tone = sin(2 * PI * 1650.0 * t) * 0.6 + sin(2 * PI * 3100.0 * t) * 0.4
                val chipNoise = (Math.random() - 0.5) * 0.5 * exp(-t * 60.0)
                val sample = (tone + chipNoise) * decay
                buffer[i] = (sample * 20000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playVictoryChord() {
        if (!isSoundEnabled()) return
        scope.launch {
            // Sparkling pentatonic arpeggio: C6 (1046.5), E6 (1318.5), G6 (1567.98), C7 (2093.0)
            val notes = listOf(1046.5, 1318.5, 1567.98, 2093.0)
            val noteDurationMs = 130
            val totalDurationMs = noteDurationMs * notes.size + 400
            val totalSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)

            notes.forEachIndexed { noteIdx, freq ->
                val startSample = (sampleRate * (noteIdx * noteDurationMs / 1000.0)).toInt()
                val noteLen = (sampleRate * 0.6).toInt()
                for (j in 0 until noteLen) {
                    val bufIdx = startSample + j
                    if (bufIdx >= totalSamples) break
                    val t = j.toDouble() / sampleRate
                    val decay = exp(-t * 6.0)
                    val s = (sin(2 * PI * freq * t) * 0.7 + sin(2 * PI * (freq * 2) * t) * 0.3) * decay
                    val cur = buffer[bufIdx].toInt()
                    val added = (cur + (s * 15000).toInt()).coerceIn(-32768, 32767)
                    buffer[bufIdx] = added.toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playBlockedThud() {
        if (!isSoundEnabled()) return
        scope.launch {
            val durationMs = 80
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 50.0)
                val sample = sin(2 * PI * 180.0 * t) * decay
                buffer[i] = (sample * 16000).toInt().coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Release after playback finishes
            scope.launch {
                val durationMs = (buffer.size.toDouble() / sampleRate * 1000).toLong() + 100
                kotlinx.coroutines.delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Fail gracefully if audio system is restricted
        }
    }
}
