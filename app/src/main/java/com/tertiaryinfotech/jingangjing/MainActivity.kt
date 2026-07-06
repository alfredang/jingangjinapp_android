package com.tertiaryinfotech.jingangjing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tertiaryinfotech.jingangjing.notify.ReminderScheduler
import com.tertiaryinfotech.jingangjing.speech.SpeechManager
import com.tertiaryinfotech.jingangjing.ui.JingangJingTheme
import com.tertiaryinfotech.jingangjing.ui.RootScreen

class MainActivity : ComponentActivity() {

    private lateinit var speech: SpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore.get(this)
        speech = SpeechManager(this)
        speech.rate = store.speechRate

        // Keep the daily verse in any scheduled reminder current (mirrors iOS).
        if (store.dailyReminderEnabled) {
            ReminderScheduler.scheduleDaily(this, store.reminderHour, store.reminderMinute)
        }

        setContent {
            JingangJingTheme {
                RootScreen(store = store, speech = speech)
            }
        }
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}
