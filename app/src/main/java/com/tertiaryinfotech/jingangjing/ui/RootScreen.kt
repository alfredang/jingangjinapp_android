package com.tertiaryinfotech.jingangjing.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tertiaryinfotech.jingangjing.AppStore
import com.tertiaryinfotech.jingangjing.data.SutraData
import com.tertiaryinfotech.jingangjing.speech.SpeechManager

// In-tab destinations of the 经文 (Read) tab.
// Saveable encoding: 0 = chapter list, -1 = full text, -2 = settings, n = chapter n.
private const val DEST_ROOT = 0
private const val DEST_FULL_TEXT = -1
private const val DEST_SETTINGS = -2

/**
 * Root bottom-tab navigation in the Tertiary Infotech house style:
 * 经文 · 持诵 · 收藏 · 反馈 · 关于, with 设置 reached from the Home top bar.
 * Mirrors the iOS app's feature set.
 */
@Composable
fun RootScreen(store: AppStore, speech: SpeechManager) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var readDest by rememberSaveable { mutableIntStateOf(DEST_ROOT) }
    var bookmarkDest by rememberSaveable { mutableIntStateOf(DEST_ROOT) }

    fun stopSpeechIfAny() {
        if (speech.activeId != null) speech.stop()
    }

    // Hardware/gesture back pops in-tab navigation.
    BackHandler(enabled = (tab == 0 && readDest != DEST_ROOT) || (tab == 2 && bookmarkDest != DEST_ROOT)) {
        if (tab == 0) { stopSpeechIfAny(); readDest = DEST_ROOT }
        else { stopSpeechIfAny(); bookmarkDest = DEST_ROOT }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "经文") },
                    label = { Text("经文") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.TouchApp, contentDescription = "持诵") },
                    label = { Text("持诵") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = "收藏") },
                    label = { Text("收藏") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "反馈") },
                    label = { Text("反馈") },
                )
                NavigationBarItem(
                    selected = tab == 4,
                    onClick = { tab = 4 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "关于") },
                    label = { Text("关于") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                0 -> when {
                    readDest == DEST_FULL_TEXT -> FullTextScreen(
                        store = store,
                        speech = speech,
                        onBack = { stopSpeechIfAny(); readDest = DEST_ROOT },
                    )
                    readDest == DEST_SETTINGS -> SettingsScreen(
                        store = store,
                        speech = speech,
                        onBack = { readDest = DEST_ROOT },
                    )
                    readDest > 0 -> ChapterDetailScreen(
                        chapterId = readDest,
                        store = store,
                        speech = speech,
                        onBack = { stopSpeechIfAny(); readDest = DEST_ROOT },
                        onOpenChapter = { stopSpeechIfAny(); readDest = it },
                    )
                    else -> HomeScreen(
                        store = store,
                        onOpenChapter = { readDest = it },
                        onOpenFullText = { readDest = DEST_FULL_TEXT },
                        onOpenSettings = { readDest = DEST_SETTINGS },
                    )
                }
                1 -> ReciteScreen(store = store)
                2 -> if (bookmarkDest > 0) {
                    ChapterDetailScreen(
                        chapterId = bookmarkDest,
                        store = store,
                        speech = speech,
                        onBack = { stopSpeechIfAny(); bookmarkDest = DEST_ROOT },
                        onOpenChapter = { stopSpeechIfAny(); bookmarkDest = it },
                    )
                } else {
                    BookmarksScreen(store = store, onOpenChapter = { bookmarkDest = it })
                }
                3 -> FeedbackScreen()
                else -> AboutScreen()
            }
        }
    }
}

/** Resolve a chapter id defensively (bad saved state falls back to chapter 1). */
fun chapterById(id: Int) = SutraData.chapters.firstOrNull { it.id == id } ?: SutraData.chapters.first()
