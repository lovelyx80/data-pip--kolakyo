package com.metrolist.music.ui.screens.equalizer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.eq.data.BAND_FREQUENCIES
import com.metrolist.music.eq.data.BUILT_IN_PRESETS
import com.metrolist.music.eq.data.BuiltInPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicEqScreen(
    viewModel: GraphicEqViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPresetMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var showDeletePreset by remember { mutableStateOf<String?>(null) }

    val presetNames = BUILT_IN_PRESETS.map { it.name }
    val currentPreset = BUILT_IN_PRESETS.find { it.name == state.presetName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = if (state.enabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = { viewModel.toggleEnabled() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current
                    .asPaddingValues().calculateBottomPadding() + 80.dp
            ),
        ) {
            // Frequency response graph
            item {
                if (state.enabled) {
                    val bandsForGraph = state.bands.mapIndexed { index, gainDb ->
                        com.metrolist.music.eq.data.ParametricEQBand(
                            frequency = com.metrolist.music.eq.data.BAND_HZ[index],
                            gain = gainDb.toDouble(),
                            q = 1.41,
                            filterType = com.metrolist.music.eq.data.FilterType.PK,
                            enabled = true,
                        )
                    }
                    EqFrequencyResponseGraph(
                        bands = bandsForGraph,
                        preamp = state.preamp.toDouble(),
                    )
                }
            }

            // 10-band sliders
            item {
                if (state.enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            items(10) { index ->
                                val (freqText, suffix) = BAND_FREQUENCIES[index]
                                VerticalEqSlider(
                                    value = state.bands[index],
                                    onValueChange = { viewModel.updateBand(index, it) },
                                    label = freqText,
                                    sublabel = suffix,
                                )
                            }
                        }
                    }
                }
            }

            // Preamp slider
            item {
                if (state.enabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Preamp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%+.1f dB", state.preamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Slider(
                            value = state.preamp,
                            onValueChange = { viewModel.updatePreamp(it) },
                            valueRange = -12f..12f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                            steps = 24,
                        )
                    }
                }
            }

            // Preset selector
            item {
                if (state.enabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Preset",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Box {
                            FilledTonalButton(
                                onClick = { showPresetMenu = true },
                            ) {
                                Text(
                                    text = state.presetName,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = showPresetMenu,
                                onDismissRequest = { showPresetMenu = false },
                            ) {
                                BUILT_IN_PRESETS.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = preset.name,
                                                fontWeight = if (preset.name == state.presetName) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            showPresetMenu = false
                                            viewModel.selectPreset(preset)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Effects section
            item {
                if (state.enabled) {
                    EqControlsSection(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        bassBoost = state.bassBoost,
                        onBassBoostChange = { viewModel.updateBassBoost(it) },
                        virtualizer = state.virtualizer,
                        onVirtualizerChange = { viewModel.updateVirtualizer(it) },
                        loudness = state.loudness,
                        onLoudnessChange = { viewModel.updateLoudness(it) },
                        balance = state.balance,
                        onBalanceChange = { viewModel.updateBalance(it) },
                    )
                }
            }

            // Action buttons
            item {
                if (state.enabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.selectPreset(BuiltInPreset("Normal", List(10) { 0f }, 0f))
                                viewModel.updateBassBoost(0)
                                viewModel.updateVirtualizer(0)
                                viewModel.updateLoudness(0)
                                viewModel.updateBalance(0f)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Reset")
                        }

                        Button(
                            onClick = {
                                if (state.presetName != "Custom") {
                                    presetNameInput = state.presetName + " (Custom)"
                                } else {
                                    presetNameInput = ""
                                }
                                showSaveDialog = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Save Preset")
                        }
                    }
                }
            }

            // Custom presets header
            if (state.customPresets.isNotEmpty() && state.enabled) {
                item {
                    Text(
                        text = "My Presets",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Custom presets list
            items(state.customPresets.size) { index ->
                val preset = state.customPresets[index]
                if (state.enabled) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = preset.name,
                                fontWeight = if (preset.name == state.presetName) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Custom preset",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.equalizer),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { viewModel.loadCustomPreset(preset) }
                            .padding(horizontal = 8.dp),
                    )
                }
            }

            // Disabled state
            if (!state.enabled) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Equalizer is disabled",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Save preset dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = presetNameInput.trim()
                        if (name.isNotEmpty()) {
                            viewModel.saveCustomPreset(name)
                            showSaveDialog = false
                            Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
