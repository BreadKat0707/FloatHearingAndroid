package cn.lemondrop.fhreborn.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * 主页面背景图片的持久化工具。
 *
 * 选图后把图片拷贝进应用内部存储（而非持有 PhotoPicker 的 URI），避免 URI 权限失效。
 */
object BackgroundImageUtils {

    private const val DIR_NAME = "backgrounds"
    private const val FILE_PREFIX = "bg_"

    /**
     * 把 [uri] 指向的图片拷贝到 `filesDir/backgrounds/bg_<timestamp>.jpg`，
     * 拷贝前删除目录内的旧文件，成功返回新文件绝对路径，失败返回 null。
     */
    fun copyImageToInternal(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            // 删除旧背景文件，只保留最新一张
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "$FILE_PREFIX${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            target.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 [path] 解码图片为 [ImageBitmap]，路径为空或解码失败返回 null。
     */
    fun loadBitmapFromPath(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return try {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
