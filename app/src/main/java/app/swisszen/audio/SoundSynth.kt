package app.swisszen.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import app.swisszen.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.random.Random

/**
 * Breath sounds are synthesised (white noise through a band-pass filter whose centre sweeps up
 * on an inhale and down on an exhale); the bell is a pre-rendered chime played via SoundPool.
 */
class SoundSynth(context: Context) {
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
    private val bellId = pool.load(context, R.raw.bell, 1)
    private val cache = ConcurrentHashMap<String, ShortArray>()
    // One thread owns every breath track, so creating, starting and releasing them never race.
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private var current: AudioTrack? = null

    fun bell(volume: Float = 0.8f) {
        pool.play(bellId, volume, volume, 1, 0, 1f)
    }

    fun breath(inhale: Boolean, seconds: Double) {
        worker.execute {
            val key = "$inhale:${"%.2f".format(seconds)}"
            val pcm = cache.getOrPut(key) { render(inhale, seconds) }
            play(pcm)
        }
    }

    // Each track is released on a timer rather than via a playback marker: the marker callback doesn't
    // always fire, and leaked tracks pile up until Android refuses new ones and the breath goes silent.
    private fun play(pcm: ShortArray) {
        current?.let(::release)
        val track = runCatching { build(pcm) }.getOrNull() ?: return
        current = track
        track.play()
        worker.schedule({ release(track) }, pcm.size * 1000L / RATE + 250, TimeUnit.MILLISECONDS)
    }

    private fun release(track: AudioTrack) {
        if (current === track) current = null
        if (track.state == AudioTrack.STATE_UNINITIALIZED) return // already released
        runCatching { track.stop() }
        track.release()
    }

    private fun build(pcm: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        if (track.write(pcm, 0, pcm.size) < 0) { track.release(); error("write failed") }
        return track
    }

    private fun render(inhale: Boolean, seconds: Double): ShortArray {
        val n = (seconds * RATE).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val rnd = Random(42)
        val f0 = if (inhale) 360.0 else 1250.0
        val f1 = if (inhale) 1250.0 else 320.0
        val peakAt = if (inhale) 0.6 else 0.18
        val peak = if (inhale) 0.11 else 0.08
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        var b0 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            if (i % 64 == 0) { // band-pass biquad (RBJ), Q = 0.8, centre swept exponentially
                val f = f0 * exp(ln(f1 / f0) * p)
                val w = 2 * PI * f / RATE
                val alpha = sin(w) / (2 * 0.8)
                val a0 = 1 + alpha
                b0 = alpha / a0; b2 = -alpha / a0
                a1 = -2 * cos(w) / a0; a2 = (1 - alpha) / a0
            }
            val x = rnd.nextDouble() * 2 - 1
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            val env = if (p < peakAt) p / peakAt else 1 - (p - peakAt) / (1 - peakAt)
            out[i] = (y * env * env * peak * 4.0 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    companion object { private const val RATE = 22050 }
}
