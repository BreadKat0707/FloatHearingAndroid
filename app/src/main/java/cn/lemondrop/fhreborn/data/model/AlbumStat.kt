package cn.lemondrop.fhreborn.data.model

/**
 * 专辑聚合信息（用于第二阶段的专辑列表/网格页）。
 *
 * 字段名需与聚合查询的 SQL 列名保持一致，以便 Room 直接映射。
 * 本期仅定义模型，尚未接入查询。
 */
data class AlbumStat(
    val album: String,
    val albumArtist: String?,
    val songCount: Int,
    val totalDuration: Long,
    val year: Int?,
    val coverPath: String?
)
