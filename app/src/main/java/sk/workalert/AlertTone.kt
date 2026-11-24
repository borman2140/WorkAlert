
package sk.workalert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

object AlertTone {
    private const val SAMPLE_RATE = 44100
    private const val PULSE_MS = 115
    private const val INTERVAL_MS = 500
    private const val START_FREQ = 1750.0
    private const val END_FREQ = 2200.0
    private const val AMPLITUDE = 0.92

    fun play(context: Context) {
        playFourPulses()
        vibrate(context)
    }

    private fun playFourPulses() {
        Thread {
            for (pulse in 0 until 4) {
                playOnePulse()
                if (pulse < 3) {
                    Thread.sleep((INTERVAL_MS - PULSE_MS).toLong())
                }
            }
        }.start()
    }

    private fun playOnePulse() {
        var track: AudioTrack? = null

        try {
            val count = SAMPLE_RATE * PULSE_MS / 1000
            val samples = ShortArray(count)
            var phase = 0.0
            val duration = PULSE_MS / 1000.0

            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val frequency = START_FREQ *
                    exp(ln(END_FREQ / START_FREQ) * t / duration)

                val envelope = when {
                    t < 0.003 -> t / 0.003
                    t < duration * 0.68 -> 1.0
                    else -> {
                        val fade = (t - duration * 0.68) / (duration * 0.32)
                        1.0 - fade
                    }
                }

                val wave = if (sin(phase) >= 0.0) 1.0 else -1.0
                val value = wave * AMPLITUDE * envelope.coerceIn(0.0, 1.0)
                samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
                phase += 2.0 * PI * frequency / SAMPLE_RATE
            }

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()

            track.write(samples, 0, samples.size)
            track.setVolume(1.0f)
            track.play()

            Thread.sleep(PULSE_MS.toLong() + 25L)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
            } catch (_: Exception) {
            }
            track?.release()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            val timings = longArrayOf(
                0, 115, 385,
                115, 385,
                115, 385,
                115
            )
            val amplitudes = intArrayOf(
                0, 255, 0,
                255, 0,
                255, 0,
                255
            )
            vibrator.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        }
    }
}
