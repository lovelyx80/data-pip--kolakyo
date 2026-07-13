package com.metrolist.music.eq.data

data class BuiltInPreset(
    val name: String,
    val bands: List<Float>,
    val preamp: Float = 0f,
)

val BUILT_IN_PRESETS = listOf(
    BuiltInPreset("Normal", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
    BuiltInPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
    BuiltInPreset("Bass Boost", listOf(6f, 5f, 3f, 1f, 0f, -1f, -2f, -1f, 0f, 0f), -2f),
    BuiltInPreset("Treble Boost", listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 6f, 5f), -2f),
    BuiltInPreset("Vocal", listOf(-1f, -1f, 0f, 3f, 4f, 3f, 2f, 1f, 1f, 0f), -1f),
    BuiltInPreset("Rock", listOf(4f, 3f, 1f, -1f, 0f, 1f, 2f, 3f, 4f, 3f), -2f),
    BuiltInPreset("Pop", listOf(3f, 2f, 0f, -1f, 2f, 3f, 2f, 1f, 2f, 2f), -1f),
    BuiltInPreset("Jazz", listOf(3f, 2f, 1f, 1f, 2f, 2f, 3f, 2f, 1f, 0f), -1f),
    BuiltInPreset("Classical", listOf(4f, 2f, 0f, -1f, -1f, 0f, 1f, 2f, 3f, 4f), -1f),
    BuiltInPreset("Dance", listOf(5f, 4f, 2f, 0f, -1f, -1f, 0f, 2f, 4f, 5f), -2f),
    BuiltInPreset("Electronic", listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 4f), -2f),
    BuiltInPreset("Hip-Hop", listOf(6f, 5f, 3f, 1f, 0f, -1f, -1f, 0f, 1f, 2f), -2f),
    BuiltInPreset("Acoustic", listOf(3f, 2f, 0f, 2f, 3f, 2f, 1f, 1f, 2f, 3f), -1f),
    BuiltInPreset("Custom", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
)
