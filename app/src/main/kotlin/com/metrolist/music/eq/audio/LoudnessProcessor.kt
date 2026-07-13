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
class LoudnessProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var loudnessPercent = 0

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private var lowShelf: BiquadFilter? = null
    private var highShelf: BiquadFilter? = null

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    fun setLoudness(percent: Int) {
        loudnessPercent = percent.coerceIn(0, 100)
        if (sampleRate > 0 && loudnessPercent > 0) {
            rebuildFilters()
        } else {
            lowShelf = null
            highShelf = null
        }
    }

    fun getLoudness(): Int = loudnessPercent

    private fun rebuildFilters() {
        val strength = loudnessPercent / 100.0
        val lowGainDb = (strength * 8.0).coerceIn(0.0, 8.0)
        val highGainDb = (strength * 6.0).coerceIn(0.0, 6.0)

        lowShelf = BiquadFilter(sampleRate, 150.0, lowGainDb, 0.7, FilterType.LSC)
        highShelf = BiquadFilter(sampleRate, 6000.0, highGainDb, 0.7, FilterType.HSC)
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        if (loudnessPercent > 0) {
            rebuildFilters()
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val lf = lowShelf
        val hf = highShelf
        if (lf == null || hf == null) {
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
                            var s = inputBuffer.getShort().toDouble() / 32768.0
                            s = lf.processSample(s)
                            s = hf.processSample(s)
                            outputBuffer.putShort((s * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                        }
                        2 -> {
                            var l = inputBuffer.getShort().toDouble() / 32768.0
                            var r = inputBuffer.getShort().toDouble() / 32768.0
                            var (pl, pr) = lf.processStereo(l, r)
                            val (phl, phr) = hf.processStereo(pl, pr)
                            outputBuffer.putShort((phl * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                            outputBuffer.putShort((phr * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
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
        lowShelf?.reset()
        highShelf?.reset()
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        lowShelf = null
        highShelf = null
    }

    override fun queueEndOfStream() { inputEnded = true }
}
