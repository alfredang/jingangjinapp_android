package com.tertiaryinfotech.jingangjing.model

/** One 分 (section/chapter) of the 金刚经. */
data class SutraChapter(
    val id: Int,          // 1...32
    val name: String,     // e.g. "法会因由分"
    val ordinal: String,  // e.g. "第一"
    val body: String,     // full text of the section
) {
    val fullTitle: String get() = name + ordinal
}

/** A single search hit — the chapter plus the matched line of text. */
data class SearchHit(
    val chapter: SutraChapter,
    val line: String,
)

/** A famous line used for 每日一偈 (verse of the day) and the daily reminder. */
data class Verse(
    val id: Int,
    val text: String,
    val chapterId: Int,   // source 分, for "read in context"
)
