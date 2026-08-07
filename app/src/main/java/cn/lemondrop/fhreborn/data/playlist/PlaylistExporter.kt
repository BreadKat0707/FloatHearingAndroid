package cn.lemondrop.fhreborn.data.playlist

import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.Song
import org.json.JSONArray
import org.json.JSONObject

/** M3U/JSON 歌单导出与导入解析（纯函数，不涉及存储） */
object PlaylistExporter {

    /**
     * 导出 M3U：每行一个绝对路径，附 #EXTINF 时长信息。
     * M3U 用于同设备/同路径恢复；跨设备建议用 JSON。
     */
    fun exportM3U(songs: List<Song>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        songs.forEach { song ->
            sb.appendLine("#EXTINF:${song.duration},${song.artist} - ${song.title}")
            sb.appendLine(song.path)
        }
        return sb.toString()
    }

    /** 导出 JSON：歌单信息 + 每首歌完整元数据（跨设备智能匹配用） */
    fun exportJson(playlist: Playlist, songs: List<Song>): String {
        val root = JSONObject().apply {
            put("format", "fh_playlist")
            put("version", 1)
            put("name", playlist.name)
            put("description", playlist.description ?: "")
            put("defaultPlayMode", playlist.defaultPlayMode)
            put("createdAt", playlist.createdAt)
            put("updatedAt", playlist.updatedAt)
            put(
                "songs",
                JSONArray().apply {
                    songs.forEach { song ->
                        put(
                            JSONObject().apply {
                                put("title", song.title)
                                put("artist", song.artist)
                                put("album", song.album)
                                put("duration", song.duration)
                                put("path", song.path)
                            }
                        )
                    }
                }
            )
        }
        return root.toString()
    }

    /**
     * 解析 JSON 导入：路径精确匹配 → 失败按元数据（标题+艺术家+专辑+时长）模糊匹配。
     * @return 匹配到的歌曲列表
     */
    fun parseJson(content: String, librarySongs: List<Song>): Pair<String?, List<Song>> {
        return try {
            val root = JSONObject(content)
            val name = root.optString("name").takeIf { it.isNotBlank() }
            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val matched = mutableListOf<Song>()
            for (i in 0 until songsArray.length()) {
                val item = songsArray.getJSONObject(i)
                val path = item.optString("path")
                val title = item.optString("title")
                val artist = item.optString("artist")
                val album = item.optString("album")
                val duration = item.optLong("duration")

                val byPath = librarySongs.firstOrNull { it.path == path }
                val song = byPath ?: librarySongs.firstOrNull {
                    it.title == title && it.artist == artist &&
                        (album.isBlank() || it.album == album) &&
                        (duration <= 0 || kotlin.math.abs(it.duration - duration) < 3000)
                }
                if (song != null) matched.add(song)
            }
            name to matched
        } catch (_: Exception) {
            null to emptyList()
        }
    }

    /**
     * 解析 M3U 导入：按路径匹配，忽略 # 注释行。
     */
    fun parseM3U(content: String, librarySongs: List<Song>): List<Song> {
        val byPath = librarySongs.associateBy { it.path }
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { byPath[it] }
            .toList()
    }
}
