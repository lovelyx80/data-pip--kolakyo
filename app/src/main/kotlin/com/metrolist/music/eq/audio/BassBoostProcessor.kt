package com.metrolist.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.data.FilterType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

@UnstableApi
@Suppress("DEPRECATION")
class BassBoostProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var boostPercent = 0
    private var filter: BiquadFilter? = null

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    fun setBoost(percent: Int) {
        boostPercent = percent.coerceIn(0, 100)
        if (sampleRate > 0 && boostPercent > 0) {
            val gainDb = (boostPercent / 100f) * 12f
            filter = BiquadFilter(
                sampleRate = sampleRate,
                frequency = 100.0,
                gain = gainDb.toDouble(),
                q = 0.7,
                filterType = FilterType.LSC
            )
        } else {
            filter = null
        }
    }

    fun getBoost(): Int = boostPercent

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        if (boostPercent > 0) {
            val gainDb = (boostPercent / 100f) * 12f
            filter = BiquadFilter(sampleRate, 100.0, gainDb.toDouble(), 0.7, FilterType.LSC)
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val currentFilter = filter
        if (currentFilter == null) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return
            if (outputBuffer.capacity() < remaining) {
                outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return

        if (outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val sampleCount = inputSize / 2
                repeat(sampleCount / channelCount) {
                    when (channelCount) {
                        1 -> {
                            val s = inputBuffer.getShort().toDouble() / 32768.0
                            val p = currentFilter.processSample(s)
                            outputBuffer.putShort((p * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                        }
                        2 -> {
                            val l = inputBuffer.getShort().toDouble() / 32768.0
                            val r = inputBuffer.getShort().toDouble() / 32768.0
                            val (pl, pr) = currentFilter.processStereo(l, r)
                            outputBuffer.putShort((pl * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                            outputBuffer.putShort((pr * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                        }
                        else -> repeat(channelCount) { outputBuffer.putShort(inputBuffer.getShort()) }
                    }
                }
            }
            else -> outputBuffer.put(inputBuffer)
        }
        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val buf = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buf
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        filter?.reset()
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        filter = null
    }

    override fun queueEndOfStream() { inputEnded = true }
}
