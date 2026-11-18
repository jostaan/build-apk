package vn.xin.alexreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Chạy khi alarm nổ: đặt alarm kế tiếp trước, rồi mới hiện thông báo. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.enabled(context)) return

        Scheduler.schedule(context)

        val seq = Prefs.nextSeq(context)
        val every = Prefs.englishEvery(context).coerceAtLeast(1)
        val englishDue = seq % every == 0

        Notifier.show(context, englishDue, seq)

        if (englishDue && Prefs.autoLaunch(context)) {
            try {
                context.startActivity(
                    Intent(context, LaunchActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                // Android chặn mở Activity từ nền: người dùng chạm thông báo thay thế.
            }
        }
    }
}

/** Đặt lại alarm sau khi khởi động lại, cập nhật app, đổi giờ hoặc múi giờ. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.enabled(context)) Scheduler.schedule(context)
    }
}
