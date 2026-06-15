package cn.lemondrop.fhreborn.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

object PathUtils {

    /**
     * 尝试从 SAF DocumentTree URI 解析出真实文件系统路径。
     * 例如 content://.../tree/primary%3AMusic → /storage/emulated/0/Music
     */
    fun getRealPathFromTreeUri(uri: Uri): String? {
        if (!DocumentsContract.isTreeUri(uri)) return null

        val treeId = DocumentsContract.getTreeDocumentId(uri)
        val split = treeId.split(":")

        return when {
            split.size >= 2 -> {
                val type = split[0]
                val path = split[1]
                when {
                    type.equals("primary", ignoreCase = true) ->
                        "/storage/emulated/0/$path"
                    else ->
                        "/storage/$type/$path"
                }
            }
            treeId == "primary" -> "/storage/emulated/0"
            else -> "/storage/$treeId"
        }
    }

    /**
     * 从 URI 获取显示名称（最后一段路径）。
     */
    fun getDisplayNameFromUri(uri: Uri): String {
        val path = uri.toString()
        val decoded = java.net.URLDecoder.decode(path, "UTF-8")
        return decoded.substringAfterLast("/", "Music")
    }
}
