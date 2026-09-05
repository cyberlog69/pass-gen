package com.cyberlog69.passgen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberlog69.passgen.domain.Separator

@Composable
fun PassphraseControls(
    wordCount: Int,
    onWordCountChange: (Int) -> Unit,
    separator: Separator,
    onSeparatorChange: (Separator) -> Unit,
    capitalize: Boolean,
    onToggleCapitalize: () -> Unit,
    appendNumber: Boolean,
    onToggleAppendNumber: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Word Count Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORD COUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "$wordCount words",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = wordCount.toFloat(),
                onValueChange = { onWordCountChange(it.toInt()) },
                valueRange = 3f..10f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Separator Selection Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "WORD SEPARATOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Separator.values().forEach { sep ->
                    FilterChip(
                        selected = separator == sep,
                        onClick = { onSeparatorChange(sep) },
                        label = {
                            Text(
                                text = when (sep) {
                                    Separator.HYPHEN -> "Hyphen (-)"
                                    Separator.SPACE -> "Space ( )"
                                    Separator.PERIOD -> "Period (.)"
                                    Separator.UNDERSCORE -> "Underscore (_)"
                                },
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Options: Capitalize & Append Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleCard(
                title = "Capitalize",
                checked = capitalize,
                onCheckedChange = onToggleCapitalize,
                modifier = Modifier.weight(1f)
            )
            ToggleCard(
                title = "Append Number",
                checked = appendNumber,
                onCheckedChange = onToggleAppendNumber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
