package com.metrolist.music.update

import kotlinx.serialization.Serializable

/**
 * Remote config fetched from update.json.
 * Every field has a safe default so malformed / partial JSON never crashes parsing.
 */
@Serializable
data class UpdateConfig(
    val latestVersion: String = "",
    val minimumVersion: String = "",
    val server: String = "on",
    val maintenance: Boolean = false,
    val update: String = "on",
    val forceUpdate: Boolean = false,
    val updateUrl: String = "",
    val message: String = "",
    val maintenanceMessage: String? = null,
    val popup: PopupConfig? = null,
)

@Serializable
data class PopupConfig(
    val enabled: Boolean = false,
    val version: String = "",
    val showOnce: Boolean = true,
    val title: String = "",
    val description: String = "",
    val primaryButton: ButtonConfig? = null,
    val secondaryButton: ButtonConfig? = null,
    val cancelButton: ButtonConfig? = null,
    val dontShowAgain: Boolean = true,
)

@Serializable
data class ButtonConfig(
    val text: String = "",
    val url: String? = null,
)

/**
 * Screen-level state driven by [UpdateConfig]. Only one of these is ever
 * shown at a time (see UpdateScreenOverlay).
 */
sealed class UpdateState {
    data object Loading : UpdateState()
    data object Normal : UpdateState()
    data class Maintenance(val message: String) : UpdateState()
    data class ForceUpdate(val message: String, val updateUrl: String) : UpdateState()
    data class OptionalUpdate(
        val latestVersion: String,
        val message: String,
        val updateUrl: String,
    ) : UpdateState()
}

/**
 * Normalizes every line-break variant we might get from JSON (real \n, real \r\n,
 * or literal two-character "\n"/"\r\n" sequences that show up if the source text
 * was double-escaped) into a single real '\n' so Compose Text() wraps correctly.
 */
fun String.normalizeLineBreaks(): String =
    this
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .replace("\\r", "\n")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
