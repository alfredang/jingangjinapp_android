package com.tertiaryinfotech.jingangjing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.jingangjing.AppStore
import com.tertiaryinfotech.jingangjing.data.SutraData
import com.tertiaryinfotech.jingangjing.speech.SpeechManager

private const val SPEECH_ID = "full"

/**
 * Continuous full-text reading of all 32 分 in one scroll, with read-aloud of
 * the whole sutra and the same adjustable text size as the per-chapter reader.
 * Mirrors the iOS FullTextView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTextScreen(store: AppStore, speech: SpeechManager, onBack: () -> Unit) {
    val fontScale = store.fontScale
    val bodyFontSize = (19 * fontScale).sp

    // The whole sutra as one speakable string (title + every section).
    val fullSpeech = remember {
        buildString {
            append("金刚般若波罗蜜经。")
            for (c in SutraData.chapters) {
                append('\n').append(c.fullTitle).append('。').append(c.body)
            }
        }
    }

    val active = speech.activeId == SPEECH_ID && speech.isSpeaking
    val playing = active && !speech.isPaused

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全文", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (active) speech.stop() else speech.speak(fullSpeech, SPEECH_ID)
                    }) {
                        Icon(
                            if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "停止诵读" else "诵读全文",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = { store.setFontScaleValue(fontScale - AppStore.FONT_STEP) },
                        enabled = fontScale > AppStore.FONT_MIN + 0.001f,
                    ) {
                        Icon(Icons.Filled.TextDecrease, contentDescription = "缩小字体")
                    }
                    IconButton(
                        onClick = { store.setFontScaleValue(fontScale + AppStore.FONT_STEP) },
                        enabled = fontScale < AppStore.FONT_MAX - 0.001f,
                    ) {
                        Icon(Icons.Filled.TextIncrease, contentDescription = "放大字体")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "《${SutraData.TITLE}》",
                        fontSize = (24 * minOf(fontScale, 1.3f)).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                    )
                    Text(
                        SutraData.TRANSLATOR,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(SutraData.chapters, key = { it.id }) { chapter ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        chapter.fullTitle,
                        fontSize = (20 * minOf(fontScale, 1.3f)).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    SelectionContainer {
                        Text(
                            chapter.body,
                            fontSize = bodyFontSize,
                            lineHeight = bodyFontSize * 1.75f,
                        )
                    }
                }
            }
        }
    }
}
