package com.manoogianmedia.studiorack.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.manoogianmedia.studiorack.R
import java.util.Locale

@Composable
internal fun DictationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = if (singleLine) 1 else 3,
    enabled: Boolean = true,
    appendDictation: Boolean = !singleLine,
) {
    val context = LocalContext.current
    val recognitionIntent = remember(label) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, dictationPrompt(label))
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }
    val recognizerAvailable = remember(recognitionIntent) {
        recognitionIntent.resolveActivity(context.packageManager) != null
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) onValueChange(mergeDictation(value, spoken, appendDictation))
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        modifier = modifier,
        trailingIcon = {
            IconButton(
                enabled = enabled,
                onClick = {
                    if (!recognizerAvailable) {
                        Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                        return@IconButton
                    }
                    try {
                        launcher.launch(recognitionIntent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_microphone),
                    contentDescription = "Dictate $label",
                    tint = if (recognizerAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

internal fun mergeDictation(existing: String, spoken: String, append: Boolean): String {
    val clean = spoken.trim()
    if (clean.isEmpty()) return existing
    if (!append || existing.isBlank()) return clean
    return "${existing.trimEnd()} $clean"
}

internal fun dictationPrompt(label: String): String = when {
    label.startsWith("Find", ignoreCase = true) -> "a search phrase"
    label.startsWith("Filter", ignoreCase = true) -> "a filter phrase"
    label.contains("note", ignoreCase = true) || label.contains("attention", ignoreCase = true) -> "your notes"
    else -> label.lowercase(Locale.getDefault())
}
