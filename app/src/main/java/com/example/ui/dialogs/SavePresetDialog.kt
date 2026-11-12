package com.example.ui.dialogs

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.*

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(NothingDarkSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.save_preset_header),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(R.string.save_preset_name_label),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input"),
                    placeholder = {
                        Text(
                            "e.g. Neon Cyberpunk Trap...",
                            color = NothingGrey,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite,
                        cursorColor = NothingRed
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NothingPillButton(
                        text = stringResource(R.string.save_preset_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        testTag = "cancel_save_preset"
                    )
                    NothingPillButton(
                        text = stringResource(R.string.save_preset_save),
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        isPrimary = true,
                        enabled = name.isNotBlank(),
                        testTag = "confirm_save_preset"
                    )
                }
            }
        }
    }
}
