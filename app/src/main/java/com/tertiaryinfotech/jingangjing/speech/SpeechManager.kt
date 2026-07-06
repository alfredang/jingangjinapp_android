package com.tertiaryinfotech.jingangjing.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Reads the sutra aloud with Android's on-device speech synthesizer (Mandarin,
 * zh-CN), publishing playback state and the currently-spoken character range
 * for live highlighting — mirrors the iOS SpeechManager.
 *
 * Android's TextToSpeech has no native pause, so pause is emulated: playback
 * stops but the current character offset is kept, and resume re-speaks the
 * remainder of the text from that offset (range highlighting stays correct
 * via [baseOffset]).
 */
class SpeechManager(context: Context) {

    /** Identifier of the passage currently loaded (e.g. "ch-1", "full"). */
    var activeId by mutableStateOf<String?>(null)
        private set
    var isSpeaking by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set

    /** Character range of [currentText] being spoken right now, for highlighting. */
    var spokenRange by mutableStateOf<IntRange?>(null)
        private set

    /** Speech rate (Android engine scale, 1.0 = default). Set from AppStore. */
    var rate: Float = 0.85f

    private var currentText: String = ""
    private var baseOffset: Int = 0        // offset of the current utterance within currentText
    private var pauseOffset: Int = 0       // where to resume from after a pause
    private var utteranceSeq = 0           // guards against stale utterance callbacks
    private var ready = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            applyMandarinVoice()
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val seq = utteranceId?.substringAfterLast('#')?.toIntOrNull()
                mainHandler.post {
                    if (seq == utteranceSeq && activeId != null && !isPaused) {
                        spokenRange = (baseOffset + start) until (baseOffset + end)
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                val seq = utteranceId?.substringAfterLast('#')?.toIntOrNull()
                mainHandler.post { if (seq == utteranceSeq) clearState() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                val seq = utteranceId?.substringAfterLast('#')?.toIntOrNull()
                mainHandler.post { if (seq == utteranceSeq) clearState() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val seq = utteranceId?.substringAfterLast('#')?.toIntOrNull()
                mainHandler.post { if (seq == utteranceSeq) clearState() }
            }
        })
    }

    fun isActive(id: String): Boolean = activeId == id && isSpeaking

    /** Start reading [text], tagging the playback with [id]. */
    fun speak(text: String, id: String) {
        if (!ready) return
        stop()
        currentText = text
        activeId = id
        isSpeaking = true
        isPaused = false
        spokenRange = null
        baseOffset = 0
        pauseOffset = 0
        speakFrom(0)
    }

    /** Toggle play / pause / resume for a given passage (mirrors iOS). */
    fun toggle(text: String, id: String) {
        if (activeId == id && isSpeaking) {
            if (isPaused) resume() else pause()
        } else {
            speak(text, id)
        }
    }

    fun pause() {
        if (activeId == null || isPaused) return
        pauseOffset = spokenRange?.first ?: baseOffset
        isPaused = true
        utteranceSeq++          // invalidate callbacks from the flushed utterance
        tts.stop()
    }

    fun resume() {
        if (activeId == null || !isPaused) return
        isPaused = false
        speakFrom(pauseOffset)
    }

    fun stop() {
        utteranceSeq++
        tts.stop()
        clearState()
    }

    fun shutdown() {
        utteranceSeq++
        tts.stop()
        tts.shutdown()
    }

    private fun speakFrom(offset: Int) {
        val remainder = currentText.substring(offset.coerceIn(0, currentText.length))
        if (remainder.isBlank()) {
            clearState()
            return
        }
        baseOffset = offset
        utteranceSeq++
        tts.setSpeechRate(rate)
        tts.setPitch(1.0f)
        tts.speak(remainder, TextToSpeech.QUEUE_FLUSH, null, "sutra#$utteranceSeq")
    }

    private fun clearState() {
        isSpeaking = false
        isPaused = false
        spokenRange = null
        activeId = null
        baseOffset = 0
        pauseOffset = 0
    }

    /** Prefer an offline Simplified-Chinese Mandarin voice. */
    private fun applyMandarinVoice() {
        tts.language = Locale.SIMPLIFIED_CHINESE
        val zhVoice = runCatching {
            tts.voices?.filter { v ->
                v.locale.language == "zh" && !v.isNetworkConnectionRequired
            }?.minByOrNull { it.latency }
        }.getOrNull()
        if (zhVoice != null) tts.voice = zhVoice
    }
}
