package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 歌单默认播放模式：0=顺序 1=列表循环 2=单曲循环 3=随机 */
object PlayMode {
    const val SEQUENTIAL = 0
    const val LIST_LOOP = 1
    const val SINGLE_LOOP = 2
    const val SHUFFLE = 3
}

/** 歌单内歌曲排序：0=自定义(添加顺序) 1=标题 2=艺术家 3=专辑 4=时长 */
object PlaylistSortType {
    const val CUSTOM = 0
    const val TITLE = 1
    const val ARTIST = 2
    const val ALBUM = 3
    const val DURATION = 4
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverPath: String? = null,
    val coverSource: Int = 0,          // 0=自动 1=手动选歌曲 2=自选图片
    val defaultPlayMode: Int = PlayMode.SEQUENTIAL,
    val tags: String? = null,          // 标签 JSON 数组（暂未开放 UI）
    val sortType: Int = 0,             // 排序方式（暂按添加顺序）
    val isSystem: Boolean = false,
    val playCount: Int = 0,            // 播放过多少次
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
