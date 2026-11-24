
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
import kotlin.math.sin

object AlertTone {
    private const val SAMPLE_RATE = 44100
    private const val DURATION_MS = 2000
    private const val FREQUENCY_HZ = 2100.0
    private const val AMPLITUDE = 0.92

    fun play(context: Context) {
        playLoudTone()
        vibrate(context)
    }

    private fun playLoudTone() {
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleCount = SAMPLE_RATE * DURATION_MS / 1000
                val samples = ShortArray(sampleCount) { index ->
                    val time = index.toDouble() / SAMPLE_RATE
                    val sample = sin(2.0 * PI * FREQUENCY_HZ * time) * AMPLITUDE
                    (sample * Short.MAX_VALUE).toInt().toShort()
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

                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                track.setVolume(1.0f)
                track.play()
                Thread.sleep(DURATION_MS.toLong())
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                } catch (_: Exception) {
                }
                track?.release()
            }
        }.start()
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}
