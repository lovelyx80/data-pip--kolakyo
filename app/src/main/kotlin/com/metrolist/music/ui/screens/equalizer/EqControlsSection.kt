package com.metrolist.music.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EqControlsSection(
    modifier: Modifier = Modifier,
    bassBoost: Int,
    onBassBoostChange: (Int) -> Unit,
    virtualizer: Int,
    onVirtualizerChange: (Int) -> Unit,
    loudness: Int,
    onLoudnessChange: (Int) -> Unit,
    balance: Float,
    onBalanceChange: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
            )
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            text = "Effects",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EffectSlider(
                label = "Bass Boost",
                value = bassBoost / 100f,
                onValueChange = { onBassBoostChange((it * 100).toInt()) },
                valueText = "$bassBoost%",
            )

            EffectSlider(
                label = "Virtualizer",
                value = virtualizer / 100f,
                onValueChange = { onVirtualizerChange((it * 100).toInt()) },
                valueText = "$virtualizer%",
            )

            EffectSlider(
                label = "Loudness",
                value = loudness / 100f,
                onValueChange = { onLoudnessChange((it * 100).toInt()) },
                valueText = "$loudness%",
            )

            val balanceText = when {
                balance < -0.05f -> "L ${(balance * -100).toInt()}%"
                balance > 0.05f -> "R ${(balance * 100).toInt()}%"
                else -> "Center"
            }
            EffectSlider(
                label = "Balance",
                value = (balance + 1f) / 2f,
                onValueChange = { onBalanceChange(it * 2f - 1f) },
                valueText = balanceText,
            )
        }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}
