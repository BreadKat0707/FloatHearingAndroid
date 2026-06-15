package cn.lemondrop.fhreborn.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获处理器
 *
 * 捕获未处理的异常，将崩溃信息持久化到文件，下次启动时展示给用户
 */
object CrashHandler {

    private const val CRASH_LOG_FILE = "crash_log.txt"

    /**
     * 初始化崩溃捕获
     */
    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(context, throwable)
            // 调用系统默认处理器，让应用正常崩溃退出
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 保存崩溃日志到私有文件
     */
    private fun saveCrashLog(context: Context, throwable: Throwable) {
        try {
            val log = buildCrashReport(context, throwable)
            val file = File(context.filesDir, CRASH_LOG_FILE)
            file.writeText(log)
        } catch (_: Exception) {
            // 保存崩溃日志时出错，静默忽略，避免二次崩溃
        }
    }

    /**
     * 构建完整的崩溃报告文本
     */
    private fun buildCrashReport(context: Context, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        return buildString {
            appendLine("===== FloatHearing Crash Report =====")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("App Version: ${packageInfo?.versionName ?: "Unknown"} (${packageInfo?.longVersionCode ?: "Unknown"})")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Board: ${Build.BOARD}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS?.joinToString() ?: "Unknown"}")
            appendLine()
            appendLine("----- Exception -----")
            appendLine("${throwable.javaClass.name}: ${throwable.message}")
            appendLine()
            appendLine("----- Stack Trace -----")
            append(sw.toString())
            appendLine()
            appendLine("===== End of Report =====")
        }
    }

    /**
     * 检查是否存在崩溃日志
     */
    fun hasCrashLog(context: Context): Boolean {
        return File(context.filesDir, CRASH_LOG_FILE).exists()
    }

    /**
     * 读取崩溃日志内容
     */
    fun readCrashLog(context: Context): String {
        return try {
            File(context.filesDir, CRASH_LOG_FILE).readText()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * 清除崩溃日志
     */
    fun clearCrashLog(context: Context) {
        try {
            File(context.filesDir, CRASH_LOG_FILE).delete()
        } catch (_: Exception) {
            // 静默忽略
        }
    }
}
