package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun GenerationActionFeedback(
    message: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isError: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = NothingRed,
                trackColor = NothingBorder,
                strokeWidth = 2.dp
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isError) NothingRed else NothingRed.copy(alpha = 0.75f))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            fontFamily = SpaceMonoFontFamily,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal,
            color = if (isError) NothingRed else NothingGrey
        )
    }
}
