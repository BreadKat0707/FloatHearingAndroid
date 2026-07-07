package cn.lemondrop.fhreborn.ui.screens.settings

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverSizes
import dev.chrisbanes.haze.HazeState
import io.github.composefluent.component.Text

/**
 * 查看本机支持的音视频编解码器内容（纯内容组件，不带外壳）。
 * 列出系统所有 MediaCodecInfo，按是否硬件加速、是否编码器分组展示。
 */
@Composable
fun CodecCapabilitiesContent(
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp,
    hazeState: HazeState
) {
    val codecs = remember { loadCodecList() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = CloverSizes.listOuterHorizontalPadding,
                end = CloverSizes.listOuterHorizontalPadding
            ),
        contentPadding = PaddingValues(bottom = bottomOverlayHeight + 16.dp)
    ) {
        item {
            Text(
                text = "共 ${codecs.size} 个编解码器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        items(codecs, key = { it.name }) { codec ->
            CodecItem(codec = codec)
            HorizontalDivider()
        }
    }
}

private data class CodecInfo(
    val name: String,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val supportedTypes: List<String>,
    val supportedMimeTypesDescription: String
)

private fun loadCodecList(): List<CodecInfo> {
    return MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        .map { codec ->
            val types = codec.supportedTypes.toList()
            CodecInfo(
                name = codec.name,
                isEncoder = codec.isEncoder,
                isHardwareAccelerated = runCatching { codec.isHardwareAccelerated }.getOrDefault(false),
                supportedTypes = types,
                supportedMimeTypesDescription = if (types.isEmpty()) "未知格式" else types.joinToString("\n")
            )
        }
        .sortedWith(
            compareByDescending<CodecInfo> { it.isHardwareAccelerated }
                .thenBy { it.isEncoder }
                .thenBy { it.name }
        )
}

@Composable
private fun CodecItem(codec: CodecInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = codec.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = buildString {
                append(if (codec.isEncoder) "编码器" else "解码器")
                append(" · ")
                append(if (codec.isHardwareAccelerated) "硬件加速" else "软件/非硬解")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = codec.supportedMimeTypesDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
