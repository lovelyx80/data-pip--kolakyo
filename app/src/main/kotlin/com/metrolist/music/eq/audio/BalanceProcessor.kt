package com.metrolist.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
@Suppress("DEPRECATION")
class BalanceProcessor : AudioProcessor {

    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var balance = 0f

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    fun setBalance(balance: Float) {
        this.balance = balance.coerceIn(-1f, 1f)
    }

    fun getBalance(): Float = balance

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (balance == 0f || channelCount != 2) {
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

        val leftGain = if (balance < 0f) 1f else (1f - balance).coerceIn(0f, 1f)
        val rightGain = if (balance > 0f) 1f else (1f + balance).coerceIn(0f, 1f)

        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val sampleCount = inputSize / 2
                repeat(sampleCount / 2) {
                    val l = inputBuffer.getShort().toDouble() / 32768.0
                    val r = inputBuffer.getShort().toDouble() / 32768.0
                    outputBuffer.putShort((l * leftGain * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                    outputBuffer.putShort((r * rightGain * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
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
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
    }

    override fun queueEndOfStream() { inputEnded = true }
}
