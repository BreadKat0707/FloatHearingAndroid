package cn.lemondrop.fhreborn.data.lyrics

/**
 * 单行歌词数据
 *
 * @param startTime 开始时间戳（毫秒）
 * @param endTime 结束时间戳（毫秒），由解析器根据下一行起始时间推算
 * @param content 歌词正文
 * @param translation 翻译歌词（同时间戳的下一行合并而来）
 * @param wordTimestamps 逐字时间戳列表（字符结束索引, 开始时间, 结束时间）
 * @param isBackground 是否为背景人声
 */
data class LyricLine(
    val startTime: Long,
    var endTime: Long = Long.MAX_VALUE,
    val content: String,
    var translation: String = "",
    val wordTimestamps: List<WordTimestamp> = emptyList(),
    val isBackground: Boolean = false
)

/**
 * 逐字时间戳
 *
 * @param startTime 该字/词开始时间（毫秒）
 * @param endTime 该字/词结束时间（毫秒）
 * @param charEndIndex 该字/词在整句中的字符结束位置（exclusive）
 */
data class WordTimestamp(
    val startTime: Long,
    val endTime: Long,
    val charEndIndex: Int
)
