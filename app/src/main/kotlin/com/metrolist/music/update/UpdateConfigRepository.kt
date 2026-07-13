package com.metrolist.music.update

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "UpdateConfigRepository"
        private val CACHE_KEY = stringPreferencesKey("update_config_cache")
        private val POPUP_DISMISSED_KEY = stringPreferencesKey("popup_dismissed_version")
        private val POPUP_DONT_SHOW_AGAIN_KEY = stringPreferencesKey("popup_dont_show_again")

        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1_500L

        private const val REMOTE_CONFIG_URL =
            "https://raw.githubusercontent.com/lovelyx80/updows/refs/heads/main/update.json"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Ensures only ONE network fetch is ever in flight at a time, even if
    // the periodic poll and a manual "check now" race each other.
    private val networkMutex = Mutex()

    @Volatile
    private var lastFetchedConfig: UpdateConfig? = null

    /**
     * Fetch config from network (with retry), fall back to cache on total failure.
     * Safe to call frequently - concurrent calls are serialized by [networkMutex].
     */
    suspend fun fetchConfig(): UpdateConfig? = withContext(Dispatchers.IO) {
        networkMutex.withLock {
            val response = fetchFromNetworkWithRetry()
            if (response != null) {
                val config = parseJson(response)
                if (config != null) {
                    writeCache(response)
                    lastFetchedConfig = config
                    return@withContext config
                }
            }

            // Network failed or JSON was malformed -> serve last known-good cache.
            val cached = readCache()
            if (cached != null) {
                val config = parseJson(cached)
                if (config != null) {
                    lastFetchedConfig = config
                    return@withContext config
                }
            }

            null
        }
    }

    /** Fetch only from cache (no network request) - used when offline. */
    suspend fun fetchCachedOnly(): UpdateConfig? = withContext(Dispatchers.IO) {
        try {
            readCache()?.let { parseJson(it) }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cache read failed")
            null
        }
    }

    fun getLastFetchedConfig(): UpdateConfig? = lastFetchedConfig

    suspend fun savePopupDismissedVersion(version: String) {
        context.safeDataStoreEdit { prefs -> prefs[POPUP_DISMISSED_KEY] = version }
    }

    suspend fun getPopupDismissedVersion(): String? = try {
        context.dataStore.data.first()[POPUP_DISMISSED_KEY]
    } catch (_: Exception) {
        null
    }

    suspend fun savePopupDontShowAgain(version: String, dontShowAgain: Boolean) {
        context.safeDataStoreEdit { prefs ->
            prefs[POPUP_DONT_SHOW_AGAIN_KEY] = if (dontShowAgain) version else ""
        }
    }

    suspend fun getPopupDontShowAgain(): String? = try {
        context.dataStore.data.first()[POPUP_DONT_SHOW_AGAIN_KEY]
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchFromNetworkWithRetry(): String? {
        var attempt = 0
        while (attempt <= MAX_RETRIES) {
            val result = fetchFromNetwork()
            if (result != null) return result
            attempt++
            if (attempt <= MAX_RETRIES) delay(RETRY_DELAY_MS)
        }
        return null
    }

    private fun fetchFromNetwork(): String? {
        return try {
            // Cache-bust so GitHub's CDN / ISP proxies never serve a stale copy.
            val urlWithCacheBuster = "$REMOTE_CONFIG_URL?t=${System.currentTimeMillis()}"

            val connection = URL(urlWithCacheBuster).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Metrolist/1.0")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("Expires", "0")
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Timber.tag(TAG).w("Server returned HTTP $responseCode")
                    return null
                }
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Network request failed")
            null
        }
    }

    private suspend fun writeCache(jsonString: String) {
        context.safeDataStoreEdit { prefs -> prefs[CACHE_KEY] = jsonString }
    }

    private suspend fun readCache(): String? = try {
        context.dataStore.data.first()[CACHE_KEY]
    } catch (_: Exception) {
        null
    }

    private fun parseJson(jsonString: String): UpdateConfig? = try {
        json.decodeFromString<UpdateConfig>(jsonString)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to parse update config")
        null
    }
}
