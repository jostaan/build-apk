package vn.xin.alexreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifier {
    const val CHANNEL_ID = "reminder_v1"
    const val NOTIF_ID = 1001

    private val MOVES = listOf(
        "Đứng dậy, xoay vai 10 lần mỗi chiều.",
        "Đi bộ 1–2 phút quanh phòng.",
        "Vươn thở: hai tay lên cao, hít sâu 5 nhịp.",
        "Gập duỗi cổ chân và xoay cổ tay 30 giây.",
        "Squat 10 lần, giữ lưng thẳng.",
        "Đứng áp lưng vào tường 30 giây, mở ngực.",
        "Kéo giãn cổ nhẹ nhàng, mỗi bên 15 giây.",
        "Đi lên xuống cầu thang 1 lượt."
    )

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Nhắc uống nước và vận động",
                NotificationManager.IMPORTANCE_HIGH
            )
            ch.description = "Nhắc định kỳ trong khung giờ đã đặt"
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    fun show(ctx: Context, englishDue: Boolean, seq: Int) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val move = MOVES[seq.coerceAtLeast(0) % MOVES.size]
        val title = if (englishDue) "Uống nước · Vận động · English 5 phút" else "Uống nước · Vận động"
        val text = "Uống vài ngụm nước. $move"
        val big = if (englishDue) "$text\nChạm để mở Gemini và luyện nói." else text

        val target = Intent(
            ctx,
            if (englishDue) LaunchActivity::class.java else MainActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pi = PendingIntent.getActivity(
            ctx,
            10,
            target,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(big))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()

        nm.notify(NOTIF_ID, n)
    }
}
