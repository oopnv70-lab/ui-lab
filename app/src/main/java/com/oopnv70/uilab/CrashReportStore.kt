package com.oopnv70.uilab

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 崩溃报告的本地持久化。只保存最近一次，避免异常时继续制造 I/O 压力。 */
object CrashReportStore {
    private const val FILE_NAME = "last_crash.txt"

    fun write(context: Context, thread: Thread, throwable: Throwable): File? {
        return runCatching {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(Date())
            val report = buildString {
                appendLine("ui-lab 崩溃报告")
                appendLine("时间: $time")
                appendLine("应用: ${context.packageName}")
                appendLine("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("线程: ${thread.name} (#${thread.id})")
                appendLine()
                appendLine("异常类型: ${throwable.javaClass.name}")
                appendLine("异常消息: ${throwable.message.orEmpty()}")
                appendLine()
                appendLine("堆栈:")
                throwable.printStackTrace(java.io.PrintWriter(this.writer()))
            }
            File(context.filesDir, FILE_NAME).apply { writeText(report, Charsets.UTF_8) }
        }.getOrNull()
    }

    fun read(context: Context): String = runCatching {
        File(context.filesDir, FILE_NAME).readText(Charsets.UTF_8)
    }.getOrDefault("没有找到崩溃报告。")

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}

private fun StringBuilder.writer(): java.io.Writer = object : java.io.Writer() {
    override fun write(cbuf: CharArray, off: Int, len: Int) { append(cbuf, off, len) }
    override fun flush() = Unit
    override fun close() = Unit
}
