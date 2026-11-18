package vn.xin.alexreminder

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Mở Gemini bằng intent công khai, không cần quyền đặc biệt.
 * Không có intent công khai nào chọn sẵn skill hoặc bật thẳng Gemini Live;
 * các cách dưới đây khác nhau ở chỗ Gemini hiện ra như thế nào, cần thử trên máy.
 */
object GeminiLauncher {
    const val GEMINI_PKG = "com.google.android.apps.bard"

    val METHOD_LABELS = listOf(
        "A. Trợ lý giọng nói (VOICE_ASSIST)",
        "B. Trợ lý mặc định (ASSIST)",
        "C. Mở ứng dụng Gemini",
        "D. Intent tùy chỉnh (nhập bên dưới)"
    )

    private fun custom(ctx: Context): Intent? {
        val action = Prefs.customAction(ctx).trim()
        val comp = Prefs.customComponent(ctx).trim()
        val data = Prefs.customData(ctx).trim()
        if (action.isEmpty() && comp.isEmpty() && data.isEmpty()) return null

        val i = Intent()
        if (action.isNotEmpty()) i.action = action
        if (data.isNotEmpty()) i.data = Uri.parse(data)
        if (comp.contains("/")) {
            val pkg = comp.substringBefore("/")
            var cls = comp.substringAfter("/")
            if (cls.startsWith(".")) cls = pkg + cls
            i.setClassName(pkg, cls)
        } else if (comp.isNotEmpty()) {
            i.setPackage(comp)
        }
        return i
    }

    fun launch(ctx: Context, method: Int = Prefs.method(ctx)): Boolean {
        val intent: Intent? = when (method) {
            0 -> Intent("android.intent.action.VOICE_ASSIST")
            1 -> Intent(Intent.ACTION_ASSIST)
            2 -> ctx.packageManager.getLaunchIntentForPackage(GEMINI_PKG)
            else -> custom(ctx)
        }
        if (intent == null) return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
