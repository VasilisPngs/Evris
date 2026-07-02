package com.evris.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import com.evris.android.settings.ReleaseChannelSettings
import com.evris.android.settings.SettingsStore

@Composable
fun SettingsScreen(
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    settings: ReleaseChannelSettings,
    playEnabled: Boolean,
    onPlayChanged: (Boolean) -> Unit,
    onDevChanged: (Boolean) -> Unit,
    onAlphaChanged: (Boolean) -> Unit,
    onBetaChanged: (Boolean) -> Unit,
    onRcChanged: (Boolean) -> Unit,
    onPrereleaseChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ThemeOption("System", themeMode == SettingsStore.THEME_SYSTEM) { onThemeChange(SettingsStore.THEME_SYSTEM) }
                ThemeOption("Light", themeMode == SettingsStore.THEME_LIGHT) { onThemeChange(SettingsStore.THEME_LIGHT) }
                ThemeOption("Dark", themeMode == SettingsStore.THEME_DARK) { onThemeChange(SettingsStore.THEME_DARK) }
            }
        }

        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Sources",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                SettingsSwitchRow(
                    title = "Google Play",
                    subtitle = "Anonymous, via Aurora. May be unstable.",
                    checked = playEnabled,
                    onCheckedChange = onPlayChanged
                )
            }
        }

    SettingsCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Release channels",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            SettingsSwitchRow(
                title = "Dev",
                subtitle = "Nightly, canary, snapshot, internal",
                checked = settings.includeDev,
                onCheckedChange = onDevChanged
            )

            SettingsSwitchRow(
                title = "Alpha",
                subtitle = null,
                checked = settings.includeAlpha,
                onCheckedChange = onAlphaChanged
            )

            SettingsSwitchRow(
                title = "Beta",
                subtitle = null,
                checked = settings.includeBeta,
                onCheckedChange = onBetaChanged
            )

            SettingsSwitchRow(
                title = "RC",
                subtitle = "Release candidate",
                checked = settings.includeRc,
                onCheckedChange = onRcChanged
            )

            SettingsSwitchRow(
                title = "Prerelease",
                subtitle = "Preview, pre-release, early access",
                checked = settings.includePrerelease,
                onCheckedChange = onPrereleaseChanged
            )
        }
    }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
