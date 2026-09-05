package com.cyberlog69.passgen.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberlog69.passgen.domain.GeneratorMode

@Composable
fun ModeTabs(
    currentMode: GeneratorMode,
    onModeSelected: (GeneratorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = GeneratorMode.values()
    val selectedIndex = modes.indexOf(currentMode)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            modes.forEachIndexed { index, mode ->
                val selected = index == selectedIndex
                val icon = when (mode) {
                    GeneratorMode.PASSWORD -> Icons.Default.Key
                    GeneratorMode.PASSPHRASE -> Icons.Default.MenuBook
                    GeneratorMode.PIN -> Icons.Default.Pin
                }

                Tab(
                    selected = selected,
                    onClick = { onModeSelected(mode) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.title,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    text = {
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
