package com.metrolist.music.eq.data

data class EQConfig(
    val enabled: Boolean = true,
    val presetName: String = "Normal",
    val bands: List<Float> = List(10) { 0f },
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val balance: Float = 0f,
)

data class EQBand(
    val frequency: String,
    val index: Int,
)

data class SavedCustomPreset(
    val name: String,
    val bands: List<Float>,
    val preamp: Float,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudness: Int,
    val balance: Float,
)

val BAND_FREQUENCIES = listOf(
    "31" to "Hz",
    "62" to "Hz",
    "125" to "Hz",
    "250" to "Hz",
    "500" to "Hz",
    "1k" to "",
    "2k" to "",
    "4k" to "",
    "8k" to "",
    "16k" to "",
)

val BAND_HZ = listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
