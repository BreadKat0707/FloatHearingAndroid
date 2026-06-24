package cn.lemondrop.fhreborn.ui.screens.demo

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun HazeDemoScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember(imageUri) {
        mutableStateOf<ImageBitmap?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imageUri = uri
        bitmap = uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 底层图片：作为 Haze 源
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择一张图片查看 Haze 效果",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 亚克力材质层：全屏半透明 + 模糊
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(state = hazeState) {
                        blurRadius = 40.dp
                        backgroundColor = Color.White.copy(alpha = 0.15f)
                        tints = listOf(HazeTint(color = Color.White.copy(alpha = 0.25f)))
                    }
            )
        }

        // 顶部渐进模糊条
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState) {
                        blurRadius = 60.dp
                        backgroundColor = Color.White.copy(alpha = 0.1f)
                        tints = listOf(HazeTint(color = Color.White.copy(alpha = 0.2f)))
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 1f,
                            endIntensity = 0f
                        )
                    }
            )
        }

        // 底部渐进模糊条
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .hazeEffect(state = hazeState) {
                        blurRadius = 60.dp
                        backgroundColor = Color.White.copy(alpha = 0.1f)
                        tints = listOf(HazeTint(color = Color.White.copy(alpha = 0.2f)))
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 0f,
                            endIntensity = 1f
                        )
                    }
            )
        }

        // 控制按钮
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text("选择图片")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) {
                Text("返回")
            }
        }

        // 居中的亚克力卡片示例
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 30.dp
                        backgroundColor = Color.White.copy(alpha = 0.2f)
                        tints = listOf(HazeTint(color = Color.White.copy(alpha = 0.3f)))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Acrylic / 亚克力",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black.copy(alpha = 0.8f)
                )
            }
        }
    }
}
