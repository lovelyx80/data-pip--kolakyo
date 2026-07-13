package com.metrolist.music.update

/**
 * Pure decision logic: given a remote config + the app's current version,
 * decide which screen state should be shown. No I/O, no state, fully unit-testable.
 */
object UpdateChecker {

    private const val DEFAULT_MAINTENANCE_MESSAGE =
        "Server is currently under maintenance. Please try again later."
    private const val DEFAULT_FORCE_UPDATE_MESSAGE =
        "This version is no longer supported. Please update to continue."
    private const val DEFAULT_OPTIONAL_UPDATE_MESSAGE =
        "A new version is available. Please update to continue."
    private const val DEFAULT_UPDATE_URL =
        "https://github.com/lovelyx80/Metrolist/releases/latest"

    fun determineState(
        config: UpdateConfig?,
        currentVersion: String,
    ): UpdateState {
        if (config == null) return UpdateState.Normal

        // Requirement #2: server == "off" OR maintenance == true -> Maintenance screen.
        if (config.server.equals("off", ignoreCase = true) || config.maintenance) {
            val msg = (config.maintenanceMessage?.takeIf { it.isNotBlank() } ?: config.message)
                .ifBlank { DEFAULT_MAINTENANCE_MESSAGE }
                .normalizeLineBreaks()
            return UpdateState.Maintenance(msg)
        }

        if (!config.update.equals("on", ignoreCase = true)) return UpdateState.Normal

        val currentParts = parseVersion(currentVersion)
        val latestParts = parseVersion(config.latestVersion)
        val minParts = parseVersion(config.minimumVersion)

        // Can't compare safely -> don't block the user on bad data.
        if (currentParts.isEmpty() || latestParts.isEmpty()) return UpdateState.Normal

        if (minParts.isNotEmpty() && compareVersions(currentParts, minParts) < 0) {
            val msg = config.message.ifBlank { DEFAULT_FORCE_UPDATE_MESSAGE }.normalizeLineBreaks()
            val url = config.updateUrl.ifBlank { DEFAULT_UPDATE_URL }
            return UpdateState.ForceUpdate(msg, url)
        }

        // forceUpdate flag forces the update screen even above the minimum version.
        if (config.forceUpdate && compareVersions(currentParts, latestParts) < 0) {
            val msg = config.message.ifBlank { DEFAULT_FORCE_UPDATE_MESSAGE }.normalizeLineBreaks()
            val url = config.updateUrl.ifBlank { DEFAULT_UPDATE_URL }
            return UpdateState.ForceUpdate(msg, url)
        }

        if (compareVersions(currentParts, latestParts) < 0) {
            val msg = config.message.ifBlank { DEFAULT_OPTIONAL_UPDATE_MESSAGE }.normalizeLineBreaks()
            val url = config.updateUrl.ifBlank { DEFAULT_UPDATE_URL }
            return UpdateState.OptionalUpdate(config.latestVersion, msg, url)
        }

        // current >= latest -> continue normally.
        return UpdateState.Normal
    }

    fun parseVersion(version: String): List<Int> {
        if (version.isBlank()) return emptyList()
        return try {
            version.trim().split(".").map { it.trim().toIntOrNull() ?: 0 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun compareVersions(a: List<Int>, b: List<Int>): Int {
        val maxLength = maxOf(a.size, b.size)
        val aPadded = a + List(maxLength - a.size) { 0 }
        val bPadded = b + List(maxLength - b.size) { 0 }

        for (i in 0 until maxLength) {
            val cmp = aPadded[i].compareTo(bPadded[i])
            if (cmp != 0) return cmp
        }
        return 0
    }
}
