package com.tertiaryinfotech.jingangjing.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.jingangjing.AppStore
import com.tertiaryinfotech.jingangjing.data.SutraData
import com.tertiaryinfotech.jingangjing.speech.SpeechManager

/**
 * Reading view for a single 分 — large, comfortable typography with adjustable
 * text size, read-aloud with live highlighting of the spoken range, bookmark,
 * share, and previous/next chapter navigation. Mirrors the iOS ChapterDetailView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    chapterId: Int,
    store: AppStore,
    speech: SpeechManager,
    onBack: () -> Unit,
    onOpenChapter: (Int) -> Unit,
) {
    val chapter = remember(chapterId) { chapterById(chapterId) }
    val context = LocalContext.current
    val speechId = "ch-${chapter.id}"
    val fontScale = store.fontScale
    val bodyFontSize = (19 * fontScale).sp
    val bookmarked = store.isBookmarked(chapter.id)

    LaunchedEffect(chapter.id) { store.markRead(chapter.id) }

    // Body with the currently-spoken range highlighted (accent + bold).
    val attributedBody = buildAnnotatedString {
        val range = if (speech.activeId == speechId) speech.spokenRange else null
        if (range == null || range.first >= chapter.body.length) {
            append(chapter.body)
        } else {
            val start = range.first.coerceIn(0, chapter.body.length)
            val end = (range.last + 1).coerceIn(start, chapter.body.length)
            append(chapter.body.substring(0, start))
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
            ) { append(chapter.body.substring(start, end)) }
            append(chapter.body.substring(end))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.fullTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { store.toggleBookmark(chapter.id) }) {
                        Icon(
                            if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (bookmarked) "取消收藏" else "收藏",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "《金刚经》${chapter.fullTitle}\n\n${chapter.body}",
                            )
                        }
                        context.startActivity(Intent.createChooser(share, "分享"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    chapter.ordinal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    chapter.name,
                    fontSize = (26 * minOf(fontScale, 1.3f)).sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            PlaybackBar(text = chapter.body, speechId = speechId, store = store, speech = speech)

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))

            SelectionContainer {
                Text(
                    attributedBody,
                    fontSize = bodyFontSize,
                    lineHeight = bodyFontSize * 1.75f,
                )
            }

            // Previous / next chapter
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val prev = SutraData.chapters.firstOrNull { it.id == chapter.id - 1 }
                val next = SutraData.chapters.firstOrNull { it.id == chapter.id + 1 }
                if (prev != null) {
                    TextButton(onClick = { onOpenChapter(prev.id) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        Text(prev.name)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (next != null) {
                    TextButton(onClick = { onOpenChapter(next.id) }) {
                        Text(next.name)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

/** Play / pause + stop + speed controls for read-aloud (shared with FullTextScreen). */
@Composable
fun PlaybackBar(text: String, speechId: String, store: AppStore, speech: SpeechManager) {
    val active = speech.activeId == speechId && speech.isSpeaking
    val playing = active && !speech.isPaused
    var speedMenu by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { speech.toggle(text, speechId) }) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (playing) "暂停" else "诵读", style = MaterialTheme.typography.titleSmall)
        }
        if (active) {
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = { speech.stop() }) {
                Icon(Icons.Filled.Stop, contentDescription = "停止")
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { speedMenu = true }) {
            Icon(Icons.Filled.Speed, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("语速")
        }
        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
            listOf("慢" to AppStore.RATE_SLOW, "正常" to AppStore.RATE_NORMAL, "快" to AppStore.RATE_FAST)
                .forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        leadingIcon = {
                            RadioButton(
                                selected = store.speechRate == value,
                                onClick = null,
                            )
                        },
                        onClick = {
                            store.setSpeechRateValue(value)
                            speech.rate = value
                            speedMenu = false
                        },
                    )
                }
        }
    }
}
