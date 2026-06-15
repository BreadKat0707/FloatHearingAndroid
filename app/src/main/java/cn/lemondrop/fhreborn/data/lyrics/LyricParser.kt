package cn.lemondrop.fhreborn.data.lyrics

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import kotlin.math.pow

/**
 * LRC / SPL / TTML / 逐字歌词解析器
 *
 * 参考 AccordLegacy (Gramophone) 的实现，支持：
 * - 普通行 [mm:ss.xx] lyrics
 * - 压缩行 [mm:ss.xx][mm:ss.xx] lyrics
 * - 逐字 <mm:ss.xx> word <mm:ss.xx> word
 * - TTML (Apple Music 逐字歌词格式)
 * - 翻译（同时间戳自动合并为 translation）
 * - 标签 [bg: ...] 背景人声
 */
object LyricParser {

    private val ttmlTimeRegex = "^(?:(\\d+):)?(?:(\\d+):)?(\\d+(?:\\.\\d+)?)$".toRegex()

    /**
     * 自动检测格式并解析
     */
    fun parseAuto(content: String): List<LyricLine> {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("<?xml") || trimmed.startsWith("<tt") -> parseTTML(content)
            else -> parse(content)
        }
    }

    private val timeRegex = "\\[(\\d{2}:\\d{2})([.:]\\d+)?]".toRegex()
    private val wordTimeRegex = "<(\\d{2}:\\d{2})([.:]\\d+)?>".toRegex()
    private val bgRegex = "\\[bg:\\s?(.*?)]".toRegex()

    fun parse(lrcContent: String, trim: Boolean = true): List<LyricLine> {
        val list = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { rawLine ->
            val line = if (trim) rawLine.trim() else rawLine
            if (line.isEmpty()) return@forEach

            // 处理 [bg: ...] 背景歌词
            bgRegex.findAll(line).let { results ->
                if (results.count() > 0) {
                    results.forEach { match ->
                        val lyricLine = match.value.substring(4, match.value.length - 1).trim()
                        if (wordTimeRegex.containsMatchIn(lyricLine)) {
                            parseWordLevel(lyricLine, 0L)?.let { (text, words) ->
                                list.add(
                                    LyricLine(
                                        startTime = 0L,
                                        content = text,
                                        wordTimestamps = words,
                                        isBackground = true
                                    )
                                )
                            }
                        } else {
                            list.add(LyricLine(startTime = 0L, content = lyricLine, isBackground = true))
                        }
                    }
                    return@forEach
                }
            }

            // 处理时间戳行
            val timeMatches = timeRegex.findAll(line)
            if (timeMatches.count() == 0) {
                // 没有时间戳的纯文本（全部当成无时间戳歌词）
                return@forEach
            }

            val textPart = line.substring(timeMatches.last().range.last + 1)
                .let { if (trim) it.trim() else it }

            timeMatches.forEach { match ->
                val timeStr = match.groupValues[1] + match.groupValues[2]
                val timestamp = parseTime(timeStr)

                if (wordTimeRegex.containsMatchIn(textPart)) {
                    parseWordLevel(textPart, timestamp)?.let { (text, words) ->
                        list.add(
                            LyricLine(
                                startTime = timestamp,
                                content = text,
                                wordTimestamps = words
                            )
                        )
                    }
                } else {
                    list.add(LyricLine(startTime = timestamp, content = textPart))
                }
            }
        }

        // 排序
        list.sortBy { it.startTime }

        // 合并翻译（相同时间戳的连续两行：第一行是原文，第二行是翻译）
        val translationIndexes = mutableListOf<Int>()
        var prevTs = -1L
        list.forEachIndexed { index, line ->
            if (line.startTime == prevTs) {
                list[index - 1].translation = line.content
                translationIndexes += index
            }
            prevTs = line.startTime
        }
        translationIndexes.reversed().forEach { list.removeAt(it) }

        // 设置 endTime
        list.forEachIndexed { index, line ->
            line.endTime = if (line.wordTimestamps.isNotEmpty()) {
                line.wordTimestamps.last().endTime
            } else {
                list.getOrNull(index + 1)?.startTime ?: Long.MAX_VALUE
            }
        }

        if (trim) {
            list.removeIf { it.content.isEmpty() }
        }

        return list
    }

    private fun parseWordLevel(
        lyricLine: String,
        baseTimestamp: Long
    ): Pair<String, List<WordTimestamp>>? {
        val wordMatches = wordTimeRegex.findAll(lyricLine)
        val words = lyricLine.split(wordTimeRegex)
        var lastWordTime = baseTimestamp
        val wordTimestamps = words.mapIndexedNotNull { index, _ ->
            wordMatches.elementAtOrNull(index)?.let { match ->
                val wordTs = parseTime(match.groupValues[1] + match.groupValues[2])
                WordTimestamp(
                    startTime = lastWordTime,
                    endTime = wordTs,
                    charEndIndex = words.take(index + 1).sumOf { it.length }
                ).also { lastWordTime = wordTs }
            }
        }.toMutableList().apply {
            removeIf { it.charEndIndex == 0 }
        }
        val cleanText = lyricLine.replace(wordTimeRegex, "")
        return Pair(cleanText, wordTimestamps)
    }

    // ===== TTML 解析 =====

    fun parseTTML(ttmlContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(ttmlContent.reader())

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "p") {
                    val line = parseTTMLLine(parser)
                    if (line != null) result.add(line)
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // 解析失败返回空列表
        }
        result.sortBy { it.startTime }
        return result
    }

    private fun parseTTMLLine(parser: XmlPullParser): LyricLine? {
        val begin = parseTTMLTime(parser.getAttributeValue(null, "begin") ?: return null)
        val end = parseTTMLTime(parser.getAttributeValue(null, "end") ?: "")
        val textBuilder = StringBuilder()
        val wordTimestamps = mutableListOf<WordTimestamp>()
        var lastWordEnd = begin

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "p")) {
            when (eventType) {
                XmlPullParser.TEXT -> {
                    val text = parser.text ?: ""
                    textBuilder.append(text)
                }
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "span", "br" -> {
                            val spanBegin = parseTTMLTime(parser.getAttributeValue(null, "begin") ?: "")
                            val spanEnd = parseTTMLTime(parser.getAttributeValue(null, "end") ?: "")
                            val spanText = parseTTMLSpanText(parser)
                            val charStart = textBuilder.length
                            val charEnd = charStart + spanText.length
                            textBuilder.append(spanText)
                            if (spanBegin >= 0 && spanEnd > spanBegin) {
                                wordTimestamps.add(
                                    WordTimestamp(
                                        startTime = spanBegin,
                                        endTime = spanEnd,
                                        charEndIndex = charEnd
                                    )
                                )
                                lastWordEnd = spanEnd
                            } else {
                                // 没有时间戳的 span，使用行级时间
                                wordTimestamps.add(
                                    WordTimestamp(
                                        startTime = lastWordEnd,
                                        endTime = lastWordEnd + 200,
                                        charEndIndex = charEnd
                                    )
                                )
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val content = textBuilder.toString().trim()
        if (content.isEmpty()) return null

        val lineEnd = if (end > begin) end else lastWordEnd
        return LyricLine(
            startTime = begin,
            endTime = lineEnd,
            content = content,
            wordTimestamps = wordTimestamps
        )
    }

    private fun parseTTMLSpanText(parser: XmlPullParser): String {
        val builder = StringBuilder()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && (parser.name == "span" || parser.name == "br"))) {
            if (eventType == XmlPullParser.TEXT) {
                builder.append(parser.text ?: "")
            }
            eventType = parser.next()
        }
        return builder.toString()
    }

    /**
     * 解析 TTML 时间格式：支持 hh:mm:ss.mmm, mm:ss.mmm, ss.mmm, mm:ss 等
     */
    private fun parseTTMLTime(timeStr: String): Long {
        if (timeStr.isBlank()) return -1
        val match = ttmlTimeRegex.find(timeStr.trim()) ?: return -1
        val groups = match.groupValues
        return when {
            // hh:mm:ss.mmm
            groups[1].isNotBlank() && groups[2].isNotBlank() -> {
                val h = groups[1].toLongOrNull() ?: 0
                val m = groups[2].toLongOrNull() ?: 0
                val s = groups[3].toDoubleOrNull() ?: 0.0
                h * 3600000 + m * 60000 + (s * 1000).toLong()
            }
            // mm:ss.mmm
            groups[2].isNotBlank() -> {
                val m = groups[2].toLongOrNull() ?: 0
                val s = groups[3].toDoubleOrNull() ?: 0.0
                m * 60000 + (s * 1000).toLong()
            }
            // ss.mmm
            else -> {
                val s = groups[3].toDoubleOrNull() ?: 0.0
                (s * 1000).toLong()
            }
        }
    }

    private fun parseTime(timeString: String): Long {
        val regex = "(\\d{2}):(\\d{2})[.:](\\d+)".toRegex()
        val match = regex.find(timeString) ?: return 0
        val minutes = match.groupValues[1].toLongOrNull() ?: 0
        val seconds = match.groupValues[2].toLongOrNull() ?: 0
        val msString = match.groupValues[3]
        val ms = (msString.substring(0, msString.length.coerceAtMost(3))
            .toLongOrNull() ?: 0) * 10f.pow(3 - msString.length).toLong()
        return minutes * 60000 + seconds * 1000 + ms
    }
}
