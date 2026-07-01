package cn.lemondrop.fhreborn.util

/**
 * 艺术家字段拆分工具。
 *
 * 支持用户通过 SettingsRepository 配置分隔符；未配置或全部为空时使用内置默认值。
 */
object ArtistSplitter {

    /** 内置默认分隔符（当用户设置为空时使用）。 */
    val DEFAULT_SEPARATORS = setOf(" / ", " feat.", " ft.")

    /**
     * 将 [artistField] 按 [separators] 拆分为独立艺术家名称。
     *
     * 分隔符会按顺序依次应用，并去除首尾空白、过滤空值、去重。
     */
    fun split(artistField: String?, separators: Set<String>): List<String> {
        if (artistField.isNullOrBlank()) return emptyList()
        val effective = separators.ifEmpty { DEFAULT_SEPARATORS }
        val parts = mutableListOf(artistField)
        for (sep in effective) {
            if (sep.isBlank()) continue
            val newParts = mutableListOf<String>()
            for (part in parts) {
                newParts.addAll(part.split(sep).map { it.trim() })
            }
            parts.clear()
            parts.addAll(newParts)
        }
        return parts.filter { it.isNotBlank() }.distinct()
    }

    /**
     * 判断 [artistField] 拆分后是否包含 [target]（忽略大小写）。
     */
    fun containsArtist(artistField: String?, target: String, separators: Set<String>): Boolean {
        return split(artistField, separators).any { it.equals(target, ignoreCase = true) }
    }
}
