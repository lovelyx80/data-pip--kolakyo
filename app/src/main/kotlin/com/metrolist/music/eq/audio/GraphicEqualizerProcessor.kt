package com.metrolist.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

@UnstableApi
@Suppress("DEPRECATION")
class GraphicEqualizerProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var enabled = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private val bandFrequencies = doubleArrayOf(
        31.0, 62.0, 125.0, 250.0, 500.0,
        1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )
    private var bandGains = FloatArray(10) { 0f }
    private var preampGain = 1.0
    private var filters = emptyList<BiquadFilter>()

    companion object {
        private const val TAG = "GraphicEqualizerProcessor"
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    fun updateBands(gains: FloatArray, preampDb: Float) {
        bandGains = gains
        preampGain = 10.0.pow(preampDb / 20.0)
        if (sampleRate > 0) {
            rebuildFilters()
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            preampGain = 1.0
            filters = emptyList()
        }
    }

    fun isEnabled(): Boolean = enabled

    private fun rebuildFilters() {
        filters = bandGains.mapIndexed { index, gainDb ->
            BiquadFilter(
                sampleRate = sampleRate,
                frequency = bandFrequencies[index],
                gain = gainDb.toDouble(),
                q = 1.41,
                filterType = com.metrolist.music.eq.data.FilterType.PK
            )
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        if (enabled && bandGains.any { it != 0f }) {
            rebuildFilters()
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!enabled || filters.isEmpty()) {
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

        if (outputBuffer === EMPTY_BUFFER || outputBuffer === inputBuffer) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else if (outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        when (encoding) {
            C.ENCODING_PCM_16BIT -> process16Bit(inputBuffer, outputBuffer)
            else -> outputBuffer.put(inputBuffer)
        }

        outputBuffer.flip()
    }

    private fun process16Bit(input: ByteBuffer, output: ByteBuffer) {
        val sampleCount = input.remaining() / 2

        repeat(sampleCount / channelCount) {
            when (channelCount) {
                1 -> {
                    val sample = input.getShort().toDouble() / 32768.0
                    var processed = sample
                    for (filter in filters) {
                        processed = filter.processSample(processed)
                    }
                    processed *= preampGain
                    val outSample = (processed * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outSample)
                }
                2 -> {
                    val left = input.getShort().toDouble() / 32768.0
                    val right = input.getShort().toDouble() / 32768.0
                    var pL = left
                    var pR = right
                    for (filter in filters) {
                        val (l, r) = filter.processStereo(pL, pR)
                        pL = l
                        pR = r
                    }
                    pL *= preampGain
                    pR *= preampGain
                    output.putShort((pL * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                    output.putShort((pR * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                }
                else -> {
                    repeat(channelCount) { output.putShort(input.getShort()) }
                }
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        filters.forEach { it.reset() }
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
