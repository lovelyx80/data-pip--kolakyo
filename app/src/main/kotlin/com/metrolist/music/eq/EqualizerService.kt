package com.metrolist.music.eq

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.audio.BalanceProcessor
import com.metrolist.music.eq.audio.BassBoostProcessor
import com.metrolist.music.eq.audio.GraphicEqualizerProcessor
import com.metrolist.music.eq.audio.LoudnessProcessor
import com.metrolist.music.eq.audio.VirtualizerProcessor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerService @Inject constructor() {

    private val eqProcessors = mutableListOf<GraphicEqualizerProcessor>()
    private val bassBoostProcessors = mutableListOf<BassBoostProcessor>()
    private val virtualizerProcessors = mutableListOf<VirtualizerProcessor>()
    private val loudnessProcessors = mutableListOf<LoudnessProcessor>()
    private val balanceProcessors = mutableListOf<BalanceProcessor>()

    private var pendingBands: FloatArray? = null
    private var pendingPreamp: Float? = null
    private var pendingBassBoost: Int? = null
    private var pendingVirtualizer: Int? = null
    private var pendingLoudness: Int? = null
    private var pendingBalance: Float? = null
    private var pendingEnabled: Boolean? = null

    companion object {
        private const val TAG = "EqualizerService"
    }

    @OptIn(UnstableApi::class)
    fun addAudioProcessors(
        eq: GraphicEqualizerProcessor,
        bassBoost: BassBoostProcessor,
        virtualizer: VirtualizerProcessor,
        loudness: LoudnessProcessor,
        balance: BalanceProcessor,
    ) {
        eqProcessors.add(eq)
        bassBoostProcessors.add(bassBoost)
        virtualizerProcessors.add(virtualizer)
        loudnessProcessors.add(loudness)
        balanceProcessors.add(balance)

        pendingBands?.let { eq.updateBands(it, pendingPreamp ?: 0f) }
        pendingBassBoost?.let { bassBoost.setBoost(it) }
        pendingVirtualizer?.let { virtualizer.setVirtualizer(it) }
        pendingLoudness?.let { loudness.setLoudness(it) }
        pendingBalance?.let { balance.setBalance(it) }
        pendingEnabled?.let { enabled ->
            eq.setEnabled(enabled)
        }
    }

    fun removeAudioProcessors(
        eq: GraphicEqualizerProcessor,
        bassBoost: BassBoostProcessor,
        virtualizer: VirtualizerProcessor,
        loudness: LoudnessProcessor,
        balance: BalanceProcessor,
    ) {
        eqProcessors.remove(eq)
        bassBoostProcessors.remove(bassBoost)
        virtualizerProcessors.remove(virtualizer)
        loudnessProcessors.remove(loudness)
        balanceProcessors.remove(balance)
    }

    fun updateBands(gains: FloatArray, preampDb: Float) {
        if (eqProcessors.isEmpty()) {
            pendingBands = gains
            pendingPreamp = preampDb
            return
        }
        pendingBands = gains
        pendingPreamp = preampDb
        eqProcessors.forEach { it.updateBands(gains, preampDb) }
    }

    fun setBassBoost(percent: Int) {
        if (bassBoostProcessors.isEmpty()) {
            pendingBassBoost = percent
            return
        }
        pendingBassBoost = percent
        bassBoostProcessors.forEach { it.setBoost(percent) }
    }

    fun setVirtualizer(percent: Int) {
        if (virtualizerProcessors.isEmpty()) {
            pendingVirtualizer = percent
            return
        }
        pendingVirtualizer = percent
        virtualizerProcessors.forEach { it.setVirtualizer(percent) }
    }

    fun setLoudness(percent: Int) {
        if (loudnessProcessors.isEmpty()) {
            pendingLoudness = percent
            return
        }
        pendingLoudness = percent
        loudnessProcessors.forEach { it.setLoudness(percent) }
    }

    fun setBalance(balance: Float) {
        if (balanceProcessors.isEmpty()) {
            pendingBalance = balance
            return
        }
        pendingBalance = balance
        balanceProcessors.forEach { it.setBalance(balance) }
    }

    @OptIn(UnstableApi::class)
    fun setEnabled(enabled: Boolean) {
        if (eqProcessors.isEmpty()) {
            pendingEnabled = enabled
            return
        }
        pendingEnabled = enabled
        eqProcessors.forEach { it.setEnabled(enabled) }
    }

    fun isEnabled(): Boolean = eqProcessors.any { it.isEnabled() }

    fun isInitialized(): Boolean = eqProcessors.isNotEmpty()

    fun release() {
        eqProcessors.clear()
        bassBoostProcessors.clear()
        virtualizerProcessors.clear()
        loudnessProcessors.clear()
        balanceProcessors.clear()
        pendingBands = null
        pendingPreamp = null
        pendingBassBoost = null
        pendingVirtualizer = null
        pendingLoudness = null
        pendingBalance = null
        pendingEnabled = null
    }
}
