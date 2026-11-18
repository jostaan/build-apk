package vn.xin.alexreminder

import android.content.Context

object Prefs {
    const val DEFAULT_PHRASE = "@alex-english-coach Start a 5-minute English speaking session."

    private fun sp(c: Context) = c.getSharedPreferences("alex_reminder", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = sp(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    /** Phút tính từ 00:00. Mặc định 07:00. */
    fun startMin(c: Context): Int = sp(c).getInt("startMin", 7 * 60)
    fun setStartMin(c: Context, v: Int) = sp(c).edit().putInt("startMin", v).apply()

    /** Phút tính từ 00:00. Mặc định 20:00. */
    fun endMin(c: Context): Int = sp(c).getInt("endMin", 20 * 60)
    fun setEndMin(c: Context, v: Int) = sp(c).edit().putInt("endMin", v).apply()

    /** Khoảng cách giữa hai lần nhắc, 60–120 phút. */
    fun intervalMin(c: Context): Int = sp(c).getInt("intervalMin", 90)
    fun setIntervalMin(c: Context, v: Int) = sp(c).edit().putInt("intervalMin", v).apply()

    /** English kích hoạt mỗi N lần nhắc (1 = mọi lần). */
    fun englishEvery(c: Context): Int = sp(c).getInt("englishEvery", 1)
    fun setEnglishEvery(c: Context, v: Int) = sp(c).edit().putInt("englishEvery", v).apply()

    /** Thử mở Gemini ngay từ nền, không cần chạm thông báo (thử nghiệm). */
    fun autoLaunch(c: Context): Boolean = sp(c).getBoolean("autoLaunch", false)
    fun setAutoLaunch(c: Context, v: Boolean) = sp(c).edit().putBoolean("autoLaunch", v).apply()

    fun copyPhrase(c: Context): Boolean = sp(c).getBoolean("copyPhrase", false)
    fun setCopyPhrase(c: Context, v: Boolean) = sp(c).edit().putBoolean("copyPhrase", v).apply()

    fun phrase(c: Context): String = sp(c).getString("phrase", DEFAULT_PHRASE) ?: DEFAULT_PHRASE
    fun setPhrase(c: Context, v: String) = sp(c).edit().putString("phrase", v).apply()

    /** Intent tùy chỉnh (cách mở D): component dạng "package/class", action, data URI. */
    fun customComponent(c: Context): String = sp(c).getString("customComponent", "") ?: ""
    fun setCustomComponent(c: Context, v: String) = sp(c).edit().putString("customComponent", v).apply()

    fun customAction(c: Context): String = sp(c).getString("customAction", "") ?: ""
    fun setCustomAction(c: Context, v: String) = sp(c).edit().putString("customAction", v).apply()

    fun customData(c: Context): String = sp(c).getString("customData", "") ?: ""
    fun setCustomData(c: Context, v: String) = sp(c).edit().putString("customData", v).apply()

    /** Cờ yêu cầu màn hình chính vẽ lại sau khi màn hình chẩn đoán đổi cài đặt. */
    fun needRefresh(c: Context): Boolean = sp(c).getBoolean("needRefresh", false)
    fun setNeedRefresh(c: Context, v: Boolean) = sp(c).edit().putBoolean("needRefresh", v).apply()

    /** 0 = VOICE_ASSIST, 1 = ASSIST, 2 = mở app Gemini, 3 = intent tùy chỉnh. */
    fun method(c: Context): Int = sp(c).getInt("method", 0)
    fun setMethod(c: Context, v: Int) = sp(c).edit().putInt("method", v).apply()

    fun nextAt(c: Context): Long = sp(c).getLong("nextAt", 0L)
    fun setNextAt(c: Context, v: Long) = sp(c).edit().putLong("nextAt", v).apply()

    /** Tăng và trả về số thứ tự lần nhắc. */
    fun nextSeq(c: Context): Int {
        val n = sp(c).getInt("seq", 0) + 1
        sp(c).edit().putInt("seq", n).apply()
        return n
    }
}
