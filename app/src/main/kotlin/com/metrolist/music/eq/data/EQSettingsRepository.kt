package com.metrolist.music.eq.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SerializablePreset(
    val name: String,
    val bands: List<Float>,
    val preamp: Float,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudness: Int,
    val balance: Float,
)

@Singleton
class EQSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "EQSettingsRepository"

        private val EQ_ENABLED = booleanPreferencesKey("eq_enabled_v2")
        private val EQ_PRESET = stringPreferencesKey("eq_preset")
        private val EQ_BANDS = stringPreferencesKey("eq_bands")
        private val EQ_PREAMP = floatPreferencesKey("eq_preamp")
        private val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        private val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        private val EQ_LOUDNESS = intPreferencesKey("eq_loudness")
        private val EQ_BALANCE = floatPreferencesKey("eq_balance")
        private val EQ_CUSTOM_PRESETS = stringPreferencesKey("eq_custom_presets")
    }

    val enabled: Flow<Boolean> = context.dataStore.data.map { it[EQ_ENABLED] ?: true }
    val preset: Flow<String> = context.dataStore.data.map { it[EQ_PRESET] ?: "Normal" }
    val bands: Flow<List<Float>> = context.dataStore.data.map { prefs ->
        prefs[EQ_BANDS]?.let { str ->
            try {
                str.split(",").map { it.toFloat() }
            } catch (e: Exception) {
                List(10) { 0f }
            }
        } ?: List(10) { 0f }
    }
    val preamp: Flow<Float> = context.dataStore.data.map { it[EQ_PREAMP] ?: 0f }
    val bassBoost: Flow<Int> = context.dataStore.data.map { it[EQ_BASS_BOOST] ?: 0 }
    val virtualizer: Flow<Int> = context.dataStore.data.map { it[EQ_VIRTUALIZER] ?: 0 }
    val loudness: Flow<Int> = context.dataStore.data.map { it[EQ_LOUDNESS] ?: 0 }
    val balance: Flow<Float> = context.dataStore.data.map { it[EQ_BALANCE] ?: 0f }

    val config: Flow<EQConfig> = context.dataStore.data.map { prefs ->
        EQConfig(
            enabled = prefs[EQ_ENABLED] ?: true,
            presetName = prefs[EQ_PRESET] ?: "Normal",
            bands = prefs[EQ_BANDS]?.let { str ->
                try { str.split(",").map { it.toFloat() } } catch (e: Exception) { List(10) { 0f } }
            } ?: List(10) { 0f },
            preamp = prefs[EQ_PREAMP] ?: 0f,
            bassBoost = prefs[EQ_BASS_BOOST] ?: 0,
            virtualizer = prefs[EQ_VIRTUALIZER] ?: 0,
            loudness = prefs[EQ_LOUDNESS] ?: 0,
            balance = prefs[EQ_BALANCE] ?: 0f,
        )
    }

    val customPresets: Flow<List<SavedCustomPreset>> = context.dataStore.data.map { prefs ->
        prefs[EQ_CUSTOM_PRESETS]?.let { str ->
            try {
                json.decodeFromString<List<SerializablePreset>>(str).map { sp ->
                    SavedCustomPreset(sp.name, sp.bands, sp.preamp, sp.bassBoost, sp.virtualizer, sp.loudness, sp.balance)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to decode custom presets")
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun saveConfig(config: EQConfig) {
        context.dataStore.edit { prefs ->
            prefs[EQ_ENABLED] = config.enabled
            prefs[EQ_PRESET] = config.presetName
            prefs[EQ_BANDS] = config.bands.joinToString(",")
            prefs[EQ_PREAMP] = config.preamp
            prefs[EQ_BASS_BOOST] = config.bassBoost
            prefs[EQ_VIRTUALIZER] = config.virtualizer
            prefs[EQ_LOUDNESS] = config.loudness
            prefs[EQ_BALANCE] = config.balance
        }
    }

    suspend fun saveCustomPreset(preset: SavedCustomPreset) {
        context.dataStore.edit { prefs ->
            val current = prefs[EQ_CUSTOM_PRESETS]?.let { str ->
                try {
                    json.decodeFromString<List<SerializablePreset>>(str).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: mutableListOf()

            val existing = current.indexOfFirst { it.name == preset.name }
            val serializable = SerializablePreset(
                preset.name, preset.bands, preset.preamp,
                preset.bassBoost, preset.virtualizer, preset.loudness, preset.balance
            )
            if (existing >= 0) {
                current[existing] = serializable
            } else {
                current.add(serializable)
            }

            prefs[EQ_CUSTOM_PRESETS] = json.encodeToString(current.toList())
        }
    }

    suspend fun deleteCustomPreset(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[EQ_CUSTOM_PRESETS]?.let { str ->
                try {
                    json.decodeFromString<List<SerializablePreset>>(str).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: return@edit

            current.removeAll { it.name == name }
            prefs[EQ_CUSTOM_PRESETS] = json.encodeToString(current.toList())
        }
    }
}
