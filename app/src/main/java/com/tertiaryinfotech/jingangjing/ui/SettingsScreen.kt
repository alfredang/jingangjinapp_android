package com.tertiaryinfotech.jingangjing.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.jingangjing.AppStore
import com.tertiaryinfotech.jingangjing.data.SutraData
import com.tertiaryinfotech.jingangjing.notify.ReminderScheduler
import com.tertiaryinfotech.jingangjing.speech.SpeechManager
import java.util.Locale

/**
 * 设置 — reading text size, recitation speed, the daily verse reminder (local
 * notifications), and reading-progress management. Mirrors the iOS SettingsView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: AppStore, speech: SpeechManager, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var notifDenied by remember { mutableStateOf(false) }
    var showResetProgress by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            notifDenied = false
            store.setReminderEnabled(true)
            ReminderScheduler.scheduleDaily(context, store.reminderHour, store.reminderMinute)
        } else {
            notifDenied = true
            store.setReminderEnabled(false)
        }
    }

    fun toggleReminder(on: Boolean) {
        if (!on) {
            store.setReminderEnabled(false)
            ReminderScheduler.cancel(context)
            notifDenied = false
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            store.setReminderEnabled(true)
            ReminderScheduler.scheduleDaily(context, store.reminderHour, store.reminderMinute)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("阅读")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("字体大小", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${(store.fontScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = store.fontScale,
                        onValueChange = { store.setFontScaleValue(it) },
                        valueRange = AppStore.FONT_MIN..AppStore.FONT_MAX,
                        steps = ((AppStore.FONT_MAX - AppStore.FONT_MIN) / AppStore.FONT_STEP).toInt() - 1,
                    )
                    HorizontalDivider()
                    Text("诵读语速", style = MaterialTheme.typography.bodyLarge)
                    val rates = listOf("慢" to AppStore.RATE_SLOW, "正常" to AppStore.RATE_NORMAL, "快" to AppStore.RATE_FAST)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        rates.forEachIndexed { index, (label, value) ->
                            SegmentedButton(
                                selected = store.speechRate == value,
                                onClick = {
                                    store.setSpeechRateValue(value)
                                    speech.rate = value
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = rates.size),
                            ) { Text(label) }
                        }
                    }
                }
            }

            SectionLabel("提醒")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每日一偈提醒", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = store.dailyReminderEnabled,
                            onCheckedChange = { toggleReminder(it) },
                        )
                    }
                    if (store.dailyReminderEnabled) {
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().clickable { showTimePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("提醒时间", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Text(
                                String.format(Locale.US, "%02d:%02d", store.reminderHour, store.reminderMinute),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        if (notifDenied) {
                            "通知权限被关闭，请在「设置 › 应用 › 金刚经 › 通知」中开启。"
                        } else {
                            "每天定时以本地通知推送一句金刚经名句，全程离线，不收集任何数据。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (notifDenied) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionLabel("进度")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row {
                        Text("已读", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${store.readChapters.size} / ${SutraData.chapters.size} 分",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "清空阅读进度",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetProgress = true },
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = store.reminderHour,
            initialMinute = store.reminderMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("提醒时间") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    store.setReminderTime(timeState.hour, timeState.minute)
                    if (store.dailyReminderEnabled) {
                        ReminderScheduler.scheduleDaily(context, timeState.hour, timeState.minute)
                    }
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }

    if (showResetProgress) {
        AlertDialog(
            onDismissRequest = { showResetProgress = false },
            title = { Text("确定清空全部阅读进度？") },
            confirmButton = {
                TextButton(onClick = { store.resetProgress(); showResetProgress = false }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgress = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}
