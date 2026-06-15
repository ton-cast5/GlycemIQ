package com.glycemiq.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.ui.theme.GlucoseCritical
import com.glycemiq.ui.theme.GlucoseCriticalContainer
import com.glycemiq.ui.theme.GlucoseModerate
import com.glycemiq.ui.theme.GlucoseModerateContainer
import com.glycemiq.ui.theme.GlucoseNormal
import com.glycemiq.ui.theme.GlucoseNormalContainer

fun glucoseLevelColor(level: GlucoseLevel): Color = when (level) {
    GlucoseLevel.NORMAL -> GlucoseNormal
    GlucoseLevel.HIGH -> GlucoseModerate
    GlucoseLevel.LOW -> GlucoseCritical
}

fun glucoseLevelContainerColor(level: GlucoseLevel): Color = when (level) {
    GlucoseLevel.NORMAL -> GlucoseNormalContainer
    GlucoseLevel.HIGH -> GlucoseModerateContainer
    GlucoseLevel.LOW -> GlucoseCriticalContainer
}

@Composable
fun GlycemCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = modifier.fillMaxWidth()
    val innerPadding = Modifier.padding(contentPadding)

    if (onClick != null) {
        Card(
            modifier = cardModifier,
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = innerPadding) { content() }
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = innerPadding) { content() }
        }
    }
}

@Composable
fun GlucoseLevelBadge(level: GlucoseLevel, value: Int, compact: Boolean = false) {
    Surface(
        color = glucoseLevelContainerColor(level),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(if (compact) 8.dp else 10.dp),
                color = glucoseLevelColor(level),
                shape = RoundedCornerShape(5.dp)
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${value} mg/dL — ${level.label}",
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = glucoseLevelColor(level),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlycemPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 48.dp else 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlycemOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun GlycemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun GlycemNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    GlycemTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardType = KeyboardType.Number
    )
}

@Composable
fun SectionTitle(text: String, compact: Boolean = false) {
    Text(
        text = text,
        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
}

@Composable
fun HorizontalChipSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.heightIn(min = 40.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun ChipSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    HorizontalChipSelector(options, selectedIndex, onSelected)
}

@Composable
fun EmptyStateMessage(message: String) {
    GlycemCard(contentPadding = 14.dp) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MessageBanner(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) GlucoseCriticalContainer else GlucoseNormalContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) GlucoseCritical else GlucoseNormal
        )
    }
}
