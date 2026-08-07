package cn.lemondrop.fhreborn.ui.screens.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar

@Composable
fun CrashReportScreen(
    crashLog: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutLeft = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr)
    val cutoutRight = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = cutoutLeft + cutoutRight)
            .background(MiuixTheme.colorScheme.windowDimming.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MiuixTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部标题栏
                BlurTopBar(
                    title = "应用发生异常"
                )

                // 副标题说明
                Text(
                    text = "上次使用时应用意外停止，以下是错误详情",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 崩溃日志内容
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        Text(
                            text = crashLog,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                // 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("崩溃日志", crashLog)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Lucide.Copy, contentDescription = "复制")
                        Text("复制")
                    }
                }
            }
        }
    }
}