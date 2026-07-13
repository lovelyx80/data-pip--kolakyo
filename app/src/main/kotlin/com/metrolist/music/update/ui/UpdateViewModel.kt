package com.metrolist.music.update.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.BuildConfig
import com.metrolist.music.update.PopupConfig
import com.metrolist.music.update.UpdateChecker
import com.metrolist.music.update.UpdateConfig
import com.metrolist.music.update.UpdateConfigRepository
import com.metrolist.music.update.UpdateState
import com.metrolist.music.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds the currently-visible popup config plus the actions the UI can trigger. */
data class PopupState(
    val config: PopupConfig,
    val onDismiss: () -> Unit,
    val onDontShowAgain: () -> Unit,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateConfigRepository,
    private val connectivityObserver: NetworkConnectivityObserver,
) : ViewModel(), DefaultLifecycleObserver {

    companion object {
        // Re-check cadence while everything is fine (requirement #1: 30-60s).
        private const val NORMAL_POLL_INTERVAL_MS = 30_000L

        // Faster re-check while the user is blocked, so recovery is snappy
        // (requirement #10: 15-30s while maintenance screen is showing).
        private const val BLOCKING_POLL_INTERVAL_MS = 15_000L
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Loading)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _popupState = MutableStateFlow<PopupState?>(null)
    val popupState: StateFlow<PopupState?> = _popupState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow(0L)
    val lastCheckedTime: StateFlow<Long> = _lastCheckedTime.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var pollingJob: Job? = null
    private var lastPopupVersionShown: String? = null

    init {
        // Requirement #1: check immediately on app start.
        performCheck()
    }

    // Requirement #1: re-check automatically whenever the app returns to foreground.
    override fun onStart(owner: LifecycleOwner) {
        performCheck()
        startPolling()
    }

    // Stop polling in background to save battery/network; resumes on next onStart.
    override fun onStop(owner: LifecycleOwner) {
        stopPolling()
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    /** Manual "check now" entry point (also used internally by the poll loop). */
    fun checkForUpdate() = performCheck()

    private fun performCheck() {
        // Duplicate-request guard: skip if a check is already in flight.
        if (_isChecking.value) return

        viewModelScope.launch {
            _isChecking.value = true

            val hasInternet = try {
                connectivityObserver.isCurrentlyConnected()
            } catch (_: Exception) {
                false
            }
            _isNetworkAvailable.value = hasInternet

            val config = if (hasInternet) {
                repository.fetchConfig()
            } else {
                repository.fetchCachedOnly()
            }

            val newState = UpdateChecker.determineState(
                config = config,
                currentVersion = BuildConfig.VERSION_NAME,
            )

            _updateState.value = newState
            _lastCheckedTime.value = System.currentTimeMillis()
            _isChecking.value = false

            evaluatePopup(config, newState)
            restartPollingIfIntervalChanged(newState)
        }
    }

    private var currentPollInterval: Long = NORMAL_POLL_INTERVAL_MS

    private fun restartPollingIfIntervalChanged(state: UpdateState) {
        val neededInterval = intervalFor(state)
        if (neededInterval != currentPollInterval) {
            currentPollInterval = neededInterval
            startPolling(force = true)
        }
    }

    private fun intervalFor(state: UpdateState): Long =
        if (state is UpdateState.Maintenance || state is UpdateState.ForceUpdate) {
            BLOCKING_POLL_INTERVAL_MS
        } else {
            NORMAL_POLL_INTERVAL_MS
        }

    private fun startPolling(force: Boolean = false) {
        if (pollingJob?.isActive == true && !force) return
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(currentPollInterval)
                performCheck()
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Popup only appears when the app is actually usable (Normal / OptionalUpdate) -
     * never stacked on top of a full-screen Maintenance or ForceUpdate block.
     */
    private suspend fun evaluatePopup(config: UpdateConfig?, state: UpdateState) {
        val popup = config?.popup

        val blocked = state is UpdateState.Maintenance || state is UpdateState.ForceUpdate
        if (popup == null || !popup.enabled || blocked) {
            _popupState.value = null
            return
        }

        // Requirement #4: when popup.version changes, show it again even if a
        // previous version was dismissed / "don't show again"-ed.
        val dontShowAgainVersion = repository.getPopupDontShowAgain()
        if (!dontShowAgainVersion.isNullOrEmpty() && dontShowAgainVersion == popup.version) {
            _popupState.value = null
            return
        }

        if (popup.showOnce) {
            val dismissedVersion = repository.getPopupDismissedVersion()
            if (dismissedVersion == popup.version) {
                _popupState.value = null
                return
            }
        }

        // Avoid re-triggering the same popup version on every poll tick once shown.
        if (lastPopupVersionShown == popup.version && _popupState.value != null) return
        lastPopupVersionShown = popup.version

        _popupState.value = PopupState(
            config = popup,
            onDismiss = {
                viewModelScope.launch {
                    if (popup.showOnce) repository.savePopupDismissedVersion(popup.version)
                }
                _popupState.value = null
            },
            onDontShowAgain = {
                viewModelScope.launch {
                    repository.savePopupDontShowAgain(popup.version, true)
                }
                _popupState.value = null
            },
        )
    }

    fun dismissOptionalUpdate() {
        _updateState.value = UpdateState.Normal
    }
}
