package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun LyricsDialog(
    title: String,
    genre: String,
    lyrics: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("lyrics_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = genre.uppercase(),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    NothingButton(
                        text = "",
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_lyrics_dialog"),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Close
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Lyrics content with parsed section tags
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        lyrics.lines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = trimmed.uppercase(),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingRed,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            } else if (trimmed.isBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Text(
                                    text = trimmed,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = NothingOffWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action row 1: Copy & Share Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingButton(
                        text = "Copy",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Lyrics // $title", "$title\n\n$lyrics")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_lyrics_button"),
                        style = NothingButtonStyle.Secondary
                    )

                    NothingButton(
                        text = "Share Text",
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TITLE, title)
                                putExtra(Intent.EXTRA_TEXT, "🎵 $title ($genre)\n\n$lyrics\n\nGenerated via Melodia Studio (Treblo AI)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share lyrics to social media..."))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_lyrics_button"),
                        style = NothingButtonStyle.Secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action row 2: Export LRC and Word DOC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingButton(
                        text = "LRC (.lrc)",
                        onClick = {
                            try {
                                val song = com.example.data.model.Song(
                                    title = title,
                                    genre = genre,
                                    mood = "Melodic",
                                    language = "Bilingual",
                                    promptText = title,
                                    tags = genre,
                                    lyrics = lyrics,
                                    coverSeed = title
                                )
                                val intent = com.example.domain.LyricsExportManager.createLrcExportIntent(context, song)
                                context.startActivity(Intent.createChooser(intent, "Export synced lyrics (.lrc)..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_lrc_button"),
                        style = NothingButtonStyle.Primary
                    )

                    NothingButton(
                        text = "Word (.doc)",
                        onClick = {
                            try {
                                val song = com.example.data.model.Song(
                                    title = title,
                                    genre = genre,
                                    mood = "Melodic",
                                    language = "Bilingual",
                                    promptText = title,
                                    tags = genre,
                                    lyrics = lyrics,
                                    coverSeed = title
                                )
                                val intent = com.example.domain.LyricsExportManager.createWordDocExportIntent(context, song)
                                context.startActivity(Intent.createChooser(intent, "Export Word document (.doc)..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_doc_button"),
                        style = NothingButtonStyle.Primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action row 3: Export TXT and JSON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingButton(
                        text = "TEXT (.txt)",
                        onClick = {
                            try {
                                val song = com.example.data.model.Song(
                                    title = title,
                                    genre = genre,
                                    mood = "Melodic",
                                    language = "Bilingual",
                                    promptText = title,
                                    tags = genre,
                                    lyrics = lyrics,
                                    coverSeed = title
                                )
                                val intent = com.example.domain.LyricsExportManager.createTextExportIntent(context, song)
                                context.startActivity(Intent.createChooser(intent, "Export text file (.txt)..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_txt_button"),
                        style = NothingButtonStyle.Secondary
                    )

                    NothingButton(
                        text = "JSON (.json)",
                        onClick = {
                            try {
                                val song = com.example.data.model.Song(
                                    title = title,
                                    genre = genre,
                                    mood = "Melodic",
                                    language = "Bilingual",
                                    promptText = title,
                                    tags = genre,
                                    lyrics = lyrics,
                                    coverSeed = title
                                )
                                val intent = com.example.domain.LyricsExportManager.createJsonExportIntent(context, song)
                                context.startActivity(Intent.createChooser(intent, "Export JSON payload..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_button"),
                        style = NothingButtonStyle.Secondary
                    )
                }
            }
        }
    }
}
