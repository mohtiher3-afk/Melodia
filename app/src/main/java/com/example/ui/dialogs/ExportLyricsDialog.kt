package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.domain.LyricsExportManager
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.components.pressable
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

enum class ExportPreviewFormat(val label: String, val extension: String) {
    LRC("LRC (.lrc)", ".lrc"),
    WORD("WORD (.doc)", ".doc"),
    TEXT("TEXT (.txt)", ".txt"),
    JSON("JSON (.json)", ".json")
}

@Composable
fun ExportLyricsDialog(
    song: Song,
    defaultFormat: String = "LRC",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialFormat = remember(defaultFormat) {
        when (defaultFormat.uppercase()) {
            "WORD", "DOC" -> ExportPreviewFormat.WORD
            "TXT", "TEXT" -> ExportPreviewFormat.TEXT
            "JSON" -> ExportPreviewFormat.JSON
            else -> ExportPreviewFormat.LRC
        }
    }
    var previewFormat by remember(defaultFormat) { mutableStateOf(initialFormat) }

    val lrcContent = remember(song) {
        LyricsExportManager.generateLrcContent(song)
    }

    val wordContent = remember(song) {
        LyricsExportManager.generateWordDocContent(song)
    }

    val textContent = remember(song) {
        LyricsExportManager.generatePureLyricsText(song)
    }

    val jsonContent = remember(song) {
        LyricsExportManager.generateLyricsJson(song)
    }

    val displayedContent = when (previewFormat) {
        ExportPreviewFormat.LRC -> lrcContent
        ExportPreviewFormat.WORD -> wordContent
        ExportPreviewFormat.TEXT -> textContent
        ExportPreviewFormat.JSON -> jsonContent
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("export_lyrics_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EXPORT // LYRICS & TIMING FORMATS",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NothingCardSurface)
                            .testTag("close_export_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Format Toggle Tabs (LRC, WORD, TEXT, JSON)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExportPreviewFormat.entries.forEach { format ->
                        val isSelected = previewFormat == format
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) NothingRed else NothingCardSurface)
                                .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(14.dp))
                                .pressable(
                                    pressedScale = 0.94f,
                                    haptic = com.example.ui.components.TapHaptic
                                ) { previewFormat = format }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("export_tab_${format.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = when (format) {
                                        ExportPreviewFormat.LRC -> Icons.Default.MusicNote
                                        ExportPreviewFormat.WORD -> Icons.Default.Description
                                        ExportPreviewFormat.TEXT -> Icons.Default.TextFields
                                        ExportPreviewFormat.JSON -> Icons.Default.Code
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) NothingWhite else NothingGrey,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = format.label,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NothingWhite else NothingGrey
                                )
                            }
                        }
                    }
                }

                // Format Description banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingCardSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val desc = when (previewFormat) {
                        ExportPreviewFormat.LRC -> "Standard synchronized LRC lyrics format for music players and karaoke apps."
                        ExportPreviewFormat.WORD -> "Microsoft Word document (.doc) formatted for desktop editing and printing."
                        ExportPreviewFormat.TEXT -> "Clean plain text (.txt) file with lyrics grouped by section."
                        ExportPreviewFormat.JSON -> "Structured JSON (.json) payload containing line timings and metadata."
                    }
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = NothingOffWhite,
                        lineHeight = 16.sp
                    )
                }

                // Code / Text Preview Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingDarkGrey)
                        .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = displayedContent,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = NothingOffWhite
                        )
                    }
                }

                // Action buttons: Copy & Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingPillButton(
                        text = "COPY CONTENT",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("${song.title} ${previewFormat.name}", displayedContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied ${previewFormat.label} to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        testTag = "copy_format_button"
                    )

                    NothingPillButton(
                        text = "EXPORT ${previewFormat.extension}",
                        onClick = {
                            try {
                                val intent = when (previewFormat) {
                                    ExportPreviewFormat.LRC -> LyricsExportManager.createLrcExportIntent(context, song)
                                    ExportPreviewFormat.WORD -> LyricsExportManager.createWordDocExportIntent(context, song)
                                    ExportPreviewFormat.TEXT -> LyricsExportManager.createTextExportIntent(context, song)
                                    ExportPreviewFormat.JSON -> LyricsExportManager.createJsonExportIntent(context, song)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export lyrics file (${previewFormat.extension})..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        isRedAccent = true,
                        testTag = "export_current_format_button"
                    )
                }
            }
        }
    }
}
