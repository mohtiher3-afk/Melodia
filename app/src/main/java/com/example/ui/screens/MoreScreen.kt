package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlyphCard
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun MoreScreen(
    onOpenVisuals: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("more_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "MORE // WORKSPACE",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = NothingRed
            )
            Text(
                text = "Tools & settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = NothingWhite
            )
            Text(
                text = "Creative tools and app preferences",
                fontSize = 14.sp,
                color = NothingGrey
            )
        }

        GlyphCard(headerTitle = "MORE DESTINATIONS") {
            Column {
                MoreDestinationRow(
                    title = "Visuals",
                    description = "Create and review your song cover art",
                    icon = Icons.Default.Image,
                    testTag = "more_visuals",
                    onClick = onOpenVisuals
                )
                MoreDivider()
                MoreDestinationRow(
                    title = "Export",
                    description = "Prepare and export lyrics or video",
                    icon = Icons.Default.Upload,
                    testTag = "more_export",
                    onClick = onOpenExport
                )
                MoreDivider()
                MoreDestinationRow(
                    title = "Settings",
                    description = "AI provider, privacy, and preferences",
                    icon = Icons.Default.Settings,
                    testTag = "more_settings",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun MoreDestinationRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NothingWhite
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = NothingGrey
            )
        }
    }
}

@Composable
private fun MoreDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp)
            .height(1.dp)
            .background(NothingBorder)
    )
}
