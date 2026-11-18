package vn.xin.alexreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

object Scheduler {
    private const val REQ = 4242

    private fun pending(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            REQ,
            Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /**
     * Mốc nhắc kế tiếp, neo theo giờ bắt đầu: start, start+I, start+2I... <= end.
     * Hết khung giờ hôm nay thì sang giờ bắt đầu ngày mai.
     */
    fun nextTrigger(nowMs: Long, startMin: Int, endMin: Int, intervalMin: Int): Long {
        val zone = ZoneId.systemDefault()
        val interval = intervalMin.coerceAtLeast(1).toLong()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val dayStart = today.atStartOfDay(zone)
        var t = dayStart.plusMinutes(startMin.toLong())
        val end = dayStart.plusMinutes(maxOf(endMin, startMin).toLong())
        while (!t.isAfter(end)) {
            val ms = t.toInstant().toEpochMilli()
            if (ms > nowMs + 1000L) return ms
            t = t.plusMinutes(interval)
        }
        return today.plusDays(1).atStartOfDay(zone)
            .plusMinutes(startMin.toLong())
            .toInstant().toEpochMilli()
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx)
        am.cancel(pi)
        if (!Prefs.enabled(ctx)) {
            Prefs.setNextAt(ctx, 0L)
            return
        }
        val at = nextTrigger(
            System.currentTimeMillis(),
            Prefs.startMin(ctx),
            Prefs.endMin(ctx),
            Prefs.intervalMin(ctx)
        )
        Prefs.setNextAt(ctx, at)
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx))
        Prefs.setNextAt(ctx, 0L)
    }
}
