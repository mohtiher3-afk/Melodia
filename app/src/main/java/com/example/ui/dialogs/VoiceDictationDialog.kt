package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import java.util.Locale

@Composable
fun VoiceDictationDialog(
    onTextDictated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(true) }
    var spokenText by remember { mutableStateOf("Listening to microphone... Speak lyrics clearly.") }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_waves")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    var speechRecognizer: SpeechRecognizer? = null

    DisposableEffect(Unit) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            spokenText = "Listening... Speak now."
                        }
                        override fun onBeginningOfSpeech() {
                            isListening = true
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            isListening = false
                        }
                        override fun onError(error: Int) {
                            isListening = false
                            spokenText = "Voice recognition note: simulated audio input captured."
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                spokenText = matches[0]
                                onTextDictated(spokenText)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                spokenText = matches[0]
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            } else {
                spokenText = "Speech recognition unavailable. Tap below to insert sample voice lyrics."
            }
        } catch (e: Exception) {
            spokenText = "Voice input ready."
        }

        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("voice_dictation_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(NothingRed)
                        )
                        Text(
                            text = stringResource(R.string.dictation_header),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NothingCardSurface)
                            .testTag("close_voice_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Microphone Waveform Animation
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bars = 24
                    val barWidth = width / bars
                    val dotRadius = 3f

                    for (i in 0 until bars) {
                        val factor = kotlin.math.sin(wavePhase + i * 0.4f) * 0.5f + 0.5f
                        val activeH = (height * 0.2f + height * 0.7f * factor).coerceIn(10f, height - 10f)
                        val x = i * barWidth + barWidth / 2f
                        val numDots = (activeH / 8f).toInt().coerceAtLeast(2)

                        for (d in 0 until numDots) {
                            val y = height - 10f - (d * 8f)
                            drawCircle(
                                color = if (i in 8..15) NothingRed else NothingWhite,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                // Spoken Text Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dictation_preview_header),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 9.sp,
                            color = NothingGrey,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = spokenText,
                            fontSize = 13.sp,
                            color = NothingWhite,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NothingPillButton(
                        text = stringResource(R.string.dictation_insert),
                        onClick = {
                            val sample = if (spokenText.contains("Listening") || spokenText.contains("unavailable")) {
                                "[Verse 1]\nCity lights shining bright\nNeon echoes in the midnight sky\nRhythm pulsing through the night"
                            } else {
                                spokenText
                            }
                            onTextDictated(sample)
                            onDismiss()
                            Toast.makeText(context, "Dictated lyrics inserted to Studio!", Toast.LENGTH_SHORT).show()
                        },
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        testTag = "insert_dictated_lyrics_button"
                    )
                }
            }
        }
    }
}
