package vn.xin.alexreminder

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var swEnabled: Switch
    private lateinit var btnStart: Button
    private lateinit var btnEnd: Button
    private lateinit var tvInterval: TextView
    private lateinit var tvEvery: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvNext: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun LinearLayout.add(v: View) {
        addView(
            v,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun label(text: String, sizeSp: Float = 15f, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(4))
        }

    private fun button(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun seek(max: Int, progress: Int, onValue: (Int) -> Unit): SeekBar =
        SeekBar(this).apply {
            this.max = max
            this.progress = progress
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onValue(p)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}

                override fun onStopTrackingTouch(s: SeekBar?) {
                    afterChange()
                }
            })
        }

    private fun field(hintText: String, initial: String, onChange: (String) -> Unit): EditText {
        val et = EditText(this)
        et.hint = hintText
        et.setText(initial)
        et.textSize = 13f
        et.setSingleLine(true)
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChange(s?.toString() ?: "")
            }
        })
        return et
    }

    private fun fmt(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không mở được màn hình cài đặt này.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }
        root.addView(col)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsets.CONSUMED
        }
        setContentView(root)

        col.add(label("Alex Reminder", 26f, true))
        col.add(label("Nhắc uống nước, vận động và mở Gemini để luyện English.", 14f))

        // --- Bật/tắt ---
        swEnabled = Switch(this).apply {
            text = "Bật nhắc"
            textSize = 18f
            isChecked = Prefs.enabled(this@MainActivity)
            setPadding(0, dp(12), 0, dp(12))
        }
        col.add(swEnabled)

        // --- Khung giờ ---
        col.add(label("Khung giờ", 16f, true))
        btnStart = button("") { pickTime(true) }
        btnEnd = button("") { pickTime(false) }
        col.add(btnStart)
        col.add(btnEnd)

        // --- Khoảng cách ---
        tvInterval = label("")
        col.add(tvInterval)
        col.add(
            seek(12, (Prefs.intervalMin(this) - 60) / 5) { p ->
                Prefs.setIntervalMin(this@MainActivity, 60 + 5 * p)
                refreshLabels()
            }
        )

        // --- Tần suất English ---
        tvEvery = label("")
        col.add(tvEvery)
        col.add(
            seek(3, Prefs.englishEvery(this) - 1) { p ->
                Prefs.setEnglishEvery(this@MainActivity, p + 1)
                refreshLabels()
            }
        )
        tvSummary = label("", 14f)
        col.add(tvSummary)

        // --- Cách mở Gemini ---
        col.add(label("Cách mở Gemini (thử từng cách, giữ cách chạy tốt nhất)", 16f, true))
        val rg = RadioGroup(this)
        GeminiLauncher.METHOD_LABELS.forEachIndexed { i, name ->
            val rb = RadioButton(this)
            rb.text = name
            rb.id = 100 + i
            rg.addView(rb)
        }
        rg.check(100 + Prefs.method(this))
        rg.setOnCheckedChangeListener { _, checkedId ->
            Prefs.setMethod(this@MainActivity, checkedId - 100)
        }
        col.add(rg)

        col.add(label("Cách D: package/class, action, data (bỏ trống nếu không dùng)", 13f))
        col.add(field("package/class, ví dụ com.example/.LiveActivity", Prefs.customComponent(this)) {
            Prefs.setCustomComponent(this@MainActivity, it)
        })
        col.add(field("action (tùy chọn)", Prefs.customAction(this)) {
            Prefs.setCustomAction(this@MainActivity, it)
        })
        col.add(field("data URI (tùy chọn)", Prefs.customData(this)) {
            Prefs.setCustomData(this@MainActivity, it)
        })

        col.add(button("Thử mở Gemini ngay") {
            if (!GeminiLauncher.launch(this@MainActivity)) {
                Toast.makeText(this@MainActivity, "Cách này không mở được. Thử cách khác.", Toast.LENGTH_LONG).show()
            }
        })

        col.add(button("Chẩn đoán Gemini / Assistant (gửi báo cáo cho Claude)") {
            startActivity(Intent(this@MainActivity, DiagActivity::class.java))
        })

        // --- Tự mở / clipboard ---
        val swAuto = Switch(this).apply {
            text = "Thử tự mở Gemini từ nền (thử nghiệm)"
            isChecked = Prefs.autoLaunch(this@MainActivity)
            setPadding(0, dp(12), 0, dp(4))
            setOnCheckedChangeListener { _, on -> Prefs.setAutoLaunch(this@MainActivity, on) }
        }
        col.add(swAuto)

        val swCopy = Switch(this).apply {
            text = "Chép câu mở đầu vào clipboard khi mở Gemini"
            isChecked = Prefs.copyPhrase(this@MainActivity)
            setPadding(0, dp(12), 0, dp(4))
            setOnCheckedChangeListener { _, on -> Prefs.setCopyPhrase(this@MainActivity, on) }
        }
        col.add(swCopy)

        val etPhrase = EditText(this).apply {
            setText(Prefs.phrase(this@MainActivity))
            minLines = 2
            textSize = 14f
        }
        etPhrase.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Prefs.setPhrase(this@MainActivity, s?.toString() ?: "")
            }
        })
        col.add(etPhrase)

        // --- Kiểm tra & cấp quyền ---
        col.add(label("Kiểm tra và quyền hệ thống", 16f, true))
        tvNext = label("", 14f)
        col.add(tvNext)
        col.add(button("Nhắc thử ngay (có English)") {
            Notifier.show(this@MainActivity, true, 0)
        })
        col.add(button("Cài đặt thông báo của app (bật thông báo nổi)") {
            safeStart(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CHANNEL_ID)
            )
        })
        col.add(button("Tắt tối ưu pin cho app") {
            safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        col.add(button("Tự khởi động (HyperOS) / chi tiết app") {
            try {
                startActivity(
                    Intent().setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            } catch (e: Exception) {
                safeStart(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
        })

        // Gắn listener sau khi đã đặt trạng thái ban đầu.
        swEnabled.setOnCheckedChangeListener { _, on ->
            Prefs.setEnabled(this@MainActivity, on)
            if (on) Scheduler.schedule(this@MainActivity) else Scheduler.cancel(this@MainActivity)
            refreshLabels()
        }

        refreshLabels()
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.needRefresh(this)) {
            Prefs.setNeedRefresh(this, false)
            recreate()
            return
        }
        // Đặt lại alarm nếu app từng bị force-stop.
        if (Prefs.enabled(this)) Scheduler.schedule(this)
        refreshLabels()
    }

    private fun pickTime(isStart: Boolean) {
        val cur = if (isStart) Prefs.startMin(this) else Prefs.endMin(this)
        TimePickerDialog(this, { _, h, m ->
            val v = h * 60 + m
            if (isStart) Prefs.setStartMin(this, v) else Prefs.setEndMin(this, v)
            afterChange()
        }, cur / 60, cur % 60, true).show()
    }

    private fun afterChange() {
        if (Prefs.endMin(this) <= Prefs.startMin(this)) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu.", Toast.LENGTH_LONG).show()
        }
        if (Prefs.enabled(this)) Scheduler.schedule(this)
        refreshLabels()
    }

    private fun refreshLabels() {
        val s = Prefs.startMin(this)
        val e = Prefs.endMin(this)
        val i = Prefs.intervalMin(this)
        val n = Prefs.englishEvery(this)

        btnStart.text = "Bắt đầu: ${fmt(s)}"
        btnEnd.text = "Kết thúc: ${fmt(e)}"
        tvInterval.text = "Khoảng cách giữa hai lần nhắc: $i phút"
        tvEvery.text = if (n == 1) "English: mọi lần nhắc" else "English: mỗi $n lần nhắc"

        val slots = if (e >= s) (e - s) / i + 1 else 0
        val englishMin = (slots / n) * 5
        tvSummary.text = "≈ $slots lần nhắc/ngày · English ≈ $englishMin phút/ngày"

        val next = Prefs.nextAt(this)
        tvNext.text = if (Prefs.enabled(this) && next > 0L) {
            "Lần nhắc kế tiếp: " +
                SimpleDateFormat("HH:mm EEE dd/MM", Locale.getDefault()).format(Date(next))
        } else {
            "Đang tắt"
        }
    }
}
