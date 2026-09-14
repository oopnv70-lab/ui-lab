package com.oopnv70.uilab

import android.app.Application
import android.content.Intent
import android.os.Process
class UiLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.processName.endsWith(":crash")) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // CrashActivity 自己崩溃时不再递归启动界面。
            if (!isCrashActivityProcess()) {
                val report = CrashReportStore.write(this, thread, throwable)
                runCatching {
                    startActivity(Intent(this, CrashActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(CrashActivity.EXTRA_REPORT_PATH, report?.absolutePath)
                    })
                }
                Process.killProcess(Process.myPid())
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun isCrashActivityProcess(): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) == null
}