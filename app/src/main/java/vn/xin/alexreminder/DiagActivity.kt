package vn.xin.alexreminder

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Đọc từ chính máy này: trợ lý mặc định, activity nào xử lý ASSIST / VOICE_ASSIST,
 * và các activity công khai của Gemini và Google app. Dùng để tìm intent thật,
 * thay vì đoán tên activity.
 */
class DiagActivity : Activity() {

    private val report = StringBuilder()
    private val rows = LinkedHashMap<String, String>()
    private lateinit var col: LinearLayout

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun line(s: String) {
        report.append(s).append('\n')
    }

    private fun secure(key: String): String {
        return try {
            Settings.Secure.getString(contentResolver, key) ?: "(trống)"
        } catch (e: Exception) {
            "(không đọc được)"
        }
    }

    private fun addRow(comp: String, note: String) {
        if (!rows.containsKey(comp)) rows[comp] = note
    }

    private fun collect() {
        line("Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        line("assistant = ${secure("assistant")}")
        line("voice_interaction_service = ${secure("voice_interaction_service")}")

        val pm = packageManager
        val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())

        val actions = listOf(
            "android.intent.action.ASSIST",
            "android.intent.action.VOICE_ASSIST",
            "android.intent.action.VOICE_COMMAND"
        )
        for (action in actions) {
            line("")
            line("Activity xử lý $action:")
            val list: List<ResolveInfo> = try {
                pm.queryIntentActivities(Intent(action), flags)
            } catch (e: Exception) {
                emptyList()
            }
            if (list.isEmpty()) line("  (không có)")
            for (ri in list) {
                val ai = ri.activityInfo ?: continue
                val comp = ai.packageName + "/" + ai.name
                line("  $comp")
                addRow(comp, action.substringAfterLast('.'))
            }
        }

        line("")
        line("VoiceInteractionService đã cài:")
        val svcs: List<ResolveInfo> = try {
            pm.queryIntentServices(Intent("android.service.voice.VoiceInteractionService"), flags)
        } catch (e: Exception) {
            emptyList()
        }
        if (svcs.isEmpty()) line("  (không thấy)")
        for (ri in svcs) {
            val si = ri.serviceInfo ?: continue
            line("  " + si.packageName + "/" + si.name)
        }

        listPackage("com.google.android.apps.bard", false)
        listPackage("com.google.android.googlequicksearchbox", true)
    }

    private fun listPackage(pkg: String, onlyKeywords: Boolean) {
        line("")
        val info: PackageInfo? = try {
            packageManager.getPackageInfo(
                pkg,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
            )
        } catch (e: Exception) {
            null
        }
        if (info == null) {
            line("$pkg: không có hoặc không đọc được")
            return
        }
        val acts: Array<ActivityInfo> = info.activities ?: emptyArray<ActivityInfo>()
        val exported = acts.filter { it.exported && it.enabled }
        val keys = listOf("assist", "voice", "gemini", "live", "bard", "hotword", "interaction", "session", "opa")
        val shown = if (onlyKeywords) {
            exported.filter { a ->
                val n = a.name.lowercase()
                keys.any { k -> n.contains(k) }
            }
        } else {
            exported
        }
        val mode = if (onlyKeywords) "lọc theo từ khóa" else "tất cả"
        line("$pkg: ${acts.size} activity, ${exported.size} công khai, hiển thị ${shown.size} ($mode)")
        for (a in shown) {
            val comp = a.packageName + "/" + a.name
            line("  $comp")
            addRow(comp, "exported")
        }
    }

    private fun tryLaunch(comp: String) {
        try {
            val i = Intent()
            i.setClassName(comp.substringBefore('/'), comp.substringAfter('/'))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Không mở được: " + e.javaClass.simpleName, Toast.LENGTH_SHORT).show()
        }
    }

    private fun adopt(comp: String) {
        Prefs.setCustomComponent(this, comp)
        Prefs.setMethod(this, 3)
        Prefs.setNeedRefresh(this, true)
        Toast.makeText(this, "Đã đặt vào cách D", Toast.LENGTH_SHORT).show()
    }

    private fun text(s: String, sizeSp: Float, bold: Boolean): TextView {
        val tv = TextView(this)
        tv.text = s
        tv.textSize = sizeSp
        if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(0, dp(10), 0, dp(4))
        return tv
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this)
        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(24))
        root.addView(col)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsets.CONSUMED
        }
        setContentView(root)

        collect()

        col.addView(text("Chẩn đoán Gemini / Assistant", 22f, true))

        val btn = Button(this)
        btn.text = "Chép báo cáo vào clipboard"
        btn.isAllCaps = false
        btn.setOnClickListener {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("diag", report.toString()))
            Toast.makeText(this, "Đã chép. Dán vào cuộc trò chuyện với Claude.", Toast.LENGTH_LONG).show()
        }
        col.addView(
            btn,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val body = TextView(this)
        body.text = report.toString()
        body.typeface = Typeface.MONOSPACE
        body.textSize = 11f
        body.setTextIsSelectable(true)
        body.setPadding(0, dp(8), 0, dp(8))
        col.addView(body)

        col.addView(text("Thử từng activity: chạm để mở, nhấn giữ để đặt vào cách D", 15f, true))
        if (rows.isEmpty()) {
            col.addView(text("(không có activity nào để thử)", 13f, false))
        }
        for ((comp, note) in rows) {
            val tv = TextView(this)
            tv.text = comp + "  [" + note + "]"
            tv.typeface = Typeface.MONOSPACE
            tv.textSize = 11f
            tv.setPadding(0, dp(9), 0, dp(9))
            tv.setOnClickListener { _: View -> tryLaunch(comp) }
            tv.setOnLongClickListener { _: View ->
                adopt(comp)
                true
            }
            col.addView(tv)
        }
    }
}
