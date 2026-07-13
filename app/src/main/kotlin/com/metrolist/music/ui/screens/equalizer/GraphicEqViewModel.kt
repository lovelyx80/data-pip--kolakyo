package com.metrolist.music.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.eq.EqualizerService
import com.metrolist.music.eq.data.BUILT_IN_PRESETS
import com.metrolist.music.eq.data.BuiltInPreset
import com.metrolist.music.eq.data.EQConfig
import com.metrolist.music.eq.data.EQSettingsRepository
import com.metrolist.music.eq.data.SavedCustomPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GraphicEqState(
    val enabled: Boolean = true,
    val presetName: String = "Normal",
    val bands: List<Float> = List(10) { 0f },
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val balance: Float = 0f,
    val customPresets: List<SavedCustomPreset> = emptyList(),
)

@HiltViewModel
class GraphicEqViewModel @Inject constructor(
    private val eqSettingsRepository: EQSettingsRepository,
    private val equalizerService: EqualizerService,
) : ViewModel() {

    private val _state = MutableStateFlow(GraphicEqState())
    val state: StateFlow<GraphicEqState> = _state.asStateFlow()

    private var lastBands: FloatArray = FloatArray(10) { 0f }
    private var lastPreamp: Float = 0f

    init {
        viewModelScope.launch {
            eqSettingsRepository.config.collect { config ->
                _state.update {
                    it.copy(
                        enabled = config.enabled,
                        presetName = config.presetName,
                        bands = config.bands,
                        preamp = config.preamp,
                        bassBoost = config.bassBoost,
                        virtualizer = config.virtualizer,
                        loudness = config.loudness,
                        balance = config.balance,
                    )
                }
                // Apply to audio processors
                applyToService(config)
            }
        }
        viewModelScope.launch {
            eqSettingsRepository.customPresets.collect { presets ->
                _state.update { it.copy(customPresets = presets) }
            }
        }
    }

    fun toggleEnabled() {
        val newState = !_state.value.enabled
        _state.update { it.copy(enabled = newState) }
        equalizerService.setEnabled(newState)
        viewModelScope.launch {
            eqSettingsRepository.saveConfig(
                currentConfig().copy(enabled = newState)
            )
        }
    }

    fun updateBand(index: Int, gainDb: Float) {
        val newBands = _state.value.bands.toMutableList()
        newBands[index] = gainDb
        val newPreset = if (isMatchingBuiltIn(newBands, _state.value.preamp)) {
            findMatchingPreset(newBands, _state.value.preamp)?.name ?: "Custom"
        } else {
            "Custom"
        }
        _state.update { it.copy(bands = newBands, presetName = newPreset) }
        val bandsArray = newBands.toFloatArray()
        lastBands = bandsArray
        lastPreamp = _state.value.preamp
        equalizerService.updateBands(bandsArray, _state.value.preamp)
        debounceSave()
    }

    fun updatePreamp(preampDb: Float) {
        val newPreset = if (isMatchingBuiltIn(_state.value.bands, preampDb)) {
            findMatchingPreset(_state.value.bands, preampDb)?.name ?: "Custom"
        } else {
            "Custom"
        }
        _state.update { it.copy(preamp = preampDb, presetName = newPreset) }
        val bandsArray = _state.value.bands.toFloatArray()
        lastBands = bandsArray
        lastPreamp = preampDb
        equalizerService.updateBands(bandsArray, preampDb)
        debounceSave()
    }

    fun selectPreset(preset: BuiltInPreset) {
        val newPresetName = if (preset.name == "Custom") {
            val saved = findMatchingSavedPreset(preset)
            if (saved != null) saved.name else "Custom"
        } else preset.name
        _state.update {
            it.copy(
                presetName = newPresetName,
                bands = preset.bands,
                preamp = preset.preamp,
            )
        }
        val bandsArray = preset.bands.toFloatArray()
        lastBands = bandsArray
        lastPreamp = preset.preamp
        equalizerService.updateBands(bandsArray, preset.preamp)
        debounceSave()
    }

    fun updateBassBoost(percent: Int) {
        _state.update { it.copy(bassBoost = percent) }
        equalizerService.setBassBoost(percent)
        debounceSave()
    }

    fun updateVirtualizer(percent: Int) {
        _state.update { it.copy(virtualizer = percent) }
        equalizerService.setVirtualizer(percent)
        debounceSave()
    }

    fun updateLoudness(percent: Int) {
        _state.update { it.copy(loudness = percent) }
        equalizerService.setLoudness(percent)
        debounceSave()
    }

    fun updateBalance(balance: Float) {
        _state.update { it.copy(balance = balance) }
        equalizerService.setBalance(balance)
        debounceSave()
    }

    fun saveCustomPreset(name: String) {
        val s = _state.value
        val preset = SavedCustomPreset(
            name = name,
            bands = s.bands,
            preamp = s.preamp,
            bassBoost = s.bassBoost,
            virtualizer = s.virtualizer,
            loudness = s.loudness,
            balance = s.balance,
        )
        viewModelScope.launch {
            eqSettingsRepository.saveCustomPreset(preset)
        }
    }

    fun loadCustomPreset(preset: SavedCustomPreset) {
        _state.update {
            it.copy(
                presetName = preset.name,
                bands = preset.bands,
                preamp = preset.preamp,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                loudness = preset.loudness,
                balance = preset.balance,
            )
        }
        applyToService(
            EQConfig(
                enabled = _state.value.enabled,
                presetName = preset.name,
                bands = preset.bands,
                preamp = preset.preamp,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                loudness = preset.loudness,
                balance = preset.balance,
            )
        )
    }

    fun deleteCustomPreset(name: String) {
        viewModelScope.launch {
            eqSettingsRepository.deleteCustomPreset(name)
        }
    }

    private fun applyToService(config: EQConfig) {
        equalizerService.setEnabled(config.enabled)
        equalizerService.updateBands(config.bands.toFloatArray(), config.preamp)
        equalizerService.setBassBoost(config.bassBoost)
        equalizerService.setVirtualizer(config.virtualizer)
        equalizerService.setLoudness(config.loudness)
        equalizerService.setBalance(config.balance)
        lastBands = config.bands.toFloatArray()
        lastPreamp = config.preamp
    }

    private fun currentConfig() = _state.value.let {
        EQConfig(
            enabled = it.enabled,
            presetName = it.presetName,
            bands = it.bands,
            preamp = it.preamp,
            bassBoost = it.bassBoost,
            virtualizer = it.virtualizer,
            loudness = it.loudness,
            balance = it.balance,
        )
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    private fun debounceSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            eqSettingsRepository.saveConfig(currentConfig())
        }
    }

    private fun isMatchingBuiltIn(bands: List<Float>, preamp: Float): Boolean {
        return BUILT_IN_PRESETS.any { it.bands == bands && it.preamp == preamp }
    }

    private fun findMatchingPreset(bands: List<Float>, preamp: Float): BuiltInPreset? {
        return BUILT_IN_PRESETS.find { it.bands == bands && it.preamp == preamp }
    }

    private fun findMatchingSavedPreset(preset: BuiltInPreset): SavedCustomPreset? {
        return _state.value.customPresets.find { sp ->
            sp.bands == preset.bands && sp.preamp == preset.preamp
        }
    }
}
