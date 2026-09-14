package com.oopnv70.uilab

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Button

class CrashActivity : Activity() {
    companion object { const val EXTRA_REPORT_PATH = "report_path" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = CrashReportStore.read(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        val title = TextView(this).apply { text = "ui-lab 发生异常"; textSize = 24f; setTextColor(Color.BLACK) }
        val hint = TextView(this).apply { text = "应用已安全停止。请复制或分享下面的日志。"; textSize = 15f; setTextColor(Color.DKGRAY) }
        val log = TextView(this).apply { text = report; textSize = 12f; setTextColor(Color.DKGRAY); setPadding(0, 16, 0, 16) }
        val scroll = ScrollView(this).apply { addView(log) }
        val buttons = LinearLayout(this).apply { gravity = Gravity.END }
        fun button(text: String, action: () -> Unit) = Button(this).apply { setText(text); setOnClickListener { action() } }
        buttons.addView(button("复制日志") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("ui-lab 崩溃日志", report))
            Toast.makeText(this, "日志已复制", Toast.LENGTH_SHORT).show()
        })
        buttons.addView(button("分享") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report) }, "分享崩溃日志"))
        })
        buttons.addView(button("重新打开") {
            packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
            finish()
        })
        root.addView(title)
        root.addView(hint)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buttons)
        setContentView(root)
    }
}