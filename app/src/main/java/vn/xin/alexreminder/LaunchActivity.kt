package vn.xin.alexreminder

import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Activity trong suốt: nhận cú chạm vào thông báo, mở Gemini rồi tự đóng.
 * Chạy từ một cú chạm của người dùng nên không bị chặn mở Activity từ nền.
 */
class LaunchActivity : Activity() {
    private var done = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java).cancel(Notifier.NOTIF_ID)
        // Dự phòng nếu cửa sổ không nhận được focus (ví dụ màn hình khóa).
        handler.postDelayed({ startGemini() }, 700)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) startGemini()
    }

    private fun startGemini() {
        if (done) return
        done = true
        handler.removeCallbacksAndMessages(null)

        if (Prefs.copyPhrase(this)) {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("alex", Prefs.phrase(this)))
        }
        if (!GeminiLauncher.launch(this)) {
            Toast.makeText(
                this,
                "Không mở được Gemini. Đổi cách mở trong Alex Reminder.",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }
}
