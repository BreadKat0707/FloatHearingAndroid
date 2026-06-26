package cn.lemondrop.fhreborn.ui.screens.player

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.X
import io.github.composefluent.component.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun CoverViewer(
    bitmap: ImageBitmap,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scale = remember { Animatable(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        fun clampOffset(newOffset: Offset, newScale: Float): Offset {
            if (newScale <= 1f) return Offset.Zero
            val maxX = (containerW * (newScale - 1f) / 2f).coerceAtLeast(0f)
            val maxY = (containerH * (newScale - 1f) / 2f).coerceAtLeast(0f)
            return Offset(
                x = newOffset.x.coerceIn(-maxX, maxX),
                y = newOffset.y.coerceIn(-maxY, maxY)
            )
        }

        Image(
            bitmap = bitmap,
            contentDescription = "封面大图",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.x / density.density
                    translationY = offset.y / density.density
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale.targetValue * zoom).coerceIn(1f, 4f)
                        val newOffset = offset + pan
                        scope.launch { scale.snapTo(newScale) }
                        offset = clampOffset(newOffset, newScale)
                    }
                },
            contentScale = ContentScale.Fit
        )

        // 关闭按钮
        FluentIconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "关闭",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 分享 / 保存
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            FluentIconButton(
                onClick = {
                    scope.launch {
                        shareCoverBitmap(context, bitmap.asAndroidBitmap())
                    }
                }
            ) {
                Icon(
                    imageVector = Lucide.Share2,
                    contentDescription = "分享",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            FluentIconButton(
                onClick = {
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            saveCoverBitmap(context, bitmap.asAndroidBitmap(), title)
                        }
                        Toast.makeText(
                            context,
                            if (success) "已保存到相册" else "保存失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Lucide.Download,
                    contentDescription = "保存",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun shareCoverBitmap(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val file = File(context.cacheDir, "cover_share.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享封面"))
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun saveCoverBitmap(context: Context, bitmap: Bitmap, title: String): Boolean {
    return try {
        val fileName = "${title}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FloatHearing")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        uri != null
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
