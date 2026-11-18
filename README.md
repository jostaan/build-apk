# Alex Reminder

Nhắc uống nước và vận động theo khung giờ, kèm lối mở Gemini để luyện English. Không dùng debugging, không root, không thư viện ngoài.

Trạng thái: mã Kotlin chưa từng được biên dịch (môi trường tác giả không có Android SDK). Lỗi biên dịch, nếu có, sẽ hiện ở bước **Build debug APK** của workflow.

## 1. Build APK bằng GitHub Actions (làm hoàn toàn trên điện thoại)

Repo cần có: `.github/workflows/build.yml`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, thư mục `app/` (gồm cả `app/debug.keystore`).

1. Tab **Actions** → **Build APK** → **Run workflow**. Mất khoảng 5–10 phút. Workflow chỉ chạy thủ công, không tự chạy khi bạn tải file lên.
2. Khi xong, vào trang **Releases** của repo (cột bên phải, hoặc `Code` → `Releases`), mở bản mới nhất và chạm vào `AlexReminder.apk` để tải thẳng. Nếu repo private, trình duyệt phải đang đăng nhập GitHub.
3. Cài file APK. Cho phép "cài ứng dụng không rõ nguồn" khi máy hỏi.
4. Bản dự phòng: mở lần chạy trong tab Actions, mục **Artifacts** có `alex-reminder-apk` (file zip chứa APK).
5. Nếu bước build lỗi, mở log bước **Build debug APK**, copy phần lỗi gửi lại.

**Cài đè bản cũ:** mọi bản build dùng chung `app/debug.keystore` nên cùng chữ ký, cài đè được mà không mất cài đặt. Khóa này chỉ ký bản debug cá nhân; nên để repo ở chế độ **private**, vì ai có file keystore đều ký được APK trùng chữ ký.

Android Studio (nếu có máy tính): **Open** thư mục dự án → chờ Sync → **Build → Build APK(s)**, file ở `app/build/outputs/apk/debug/app-debug.apk`.

## 2. Thiết lập lần đầu trên HyperOS 3

Mở app, cấp quyền thông báo, rồi dùng các nút cuối màn hình:

| Nút | Cần đặt |
|---|---|
| Cài đặt thông báo của app | Bật **Thông báo nổi**, **Màn hình khóa**, âm thanh/rung |
| Tắt tối ưu pin cho app | Chọn **Không hạn chế** (Cài đặt → Ứng dụng → Quản lý ứng dụng → Alex Reminder → Tiết kiệm pin) |
| Tự khởi động (HyperOS) | Bật **Tự khởi động**. Nếu nút không mở đúng màn hình, vào chi tiết app và tìm mục này |
| (thủ công) | Khóa app trong màn hình đa nhiệm để hệ thống không dọn |

Thiếu hai mục đầu tiên thì HyperOS sẽ hoãn hoặc nuốt thông báo.

## 3. Cách hoạt động

- Mốc nhắc neo theo giờ bắt đầu. Ví dụ 07:00–20:00, cách 90 phút: 07:00, 08:30, 10:00, 11:30, 13:00, 14:30, 16:00, 17:30, 19:00 (9 lần/ngày). Hết khung giờ thì chờ 07:00 hôm sau.
- Khoảng cách chỉnh 60–120 phút (bước 5). English chỉnh "mọi lần" hoặc "mỗi 2–4 lần nhắc". Màn hình chính tính sẵn số phút English mỗi ngày để đối chiếu ngân sách.
- Alarm dùng `setExactAndAllowWhileIdle` (quyền `USE_EXACT_ALARM`, cấp tự động khi cài). Sau khởi động lại máy, cập nhật app, đổi giờ hoặc múi giờ, alarm tự đặt lại.
- Mỗi lần nhắc có thông báo kèm một bài vận động ngắn (8 bài xoay vòng).

## 4. Mở Gemini: giới hạn cần biết

- Android 15/16 chặn app nền tự mở Activity. Luồng chuẩn là **chạm vào thông báo** → app mở Gemini. Công tắc "Thử tự mở Gemini từ nền" chỉ là thử nghiệm, có thể không hiệu quả trên máy của bạn.
- Không có intent công khai đã được xác nhận nào bật thẳng Gemini Live hoặc chọn sẵn skill `@alex-english-coach`. Cách A (VOICE_ASSIST), B (ASSIST), C (mở app Gemini) chỉ đưa Gemini lên màn hình. Dùng nút **Thử mở Gemini ngay**, thử từng cách, giữ cách gần Live nhất. Có thể bạn vẫn phải chạm nút Live một lần.
- **Cách D (intent tùy chỉnh)** dành cho trường hợp tìm được activity vào thẳng Live. Cách tìm, không cần debugging:
  1. Cài app **Activity Launcher** (miễn phí trên Google Play).
  2. Tìm "Gemini" (hoặc "Google") → mở danh sách activity → chạm thử các mục có tên liên quan "live", "voice", "assistant".
  3. Mục nào mở đúng Gemini Live: nhấn giữ để lấy tên đầy đủ dạng `package/class`, dán vào ô D trong Alex Reminder, chọn cách D, bấm **Thử mở Gemini ngay**.
  Chỉ activity công khai (exported) mới gọi được; nếu không có mục nào mở Live, cách D không giúp được gì thêm. Tên activity do Google đặt và có thể đổi khi Gemini cập nhật.
- Câu mở đầu (mặc định `@alex-english-coach Start a 5-minute English speaking session.`) có thể chép vào clipboard khi mở Gemini. Chỉ hữu ích nếu bạn dán vào ô nhập văn bản; chế độ nói không có ô dán. Công tắc này tắt mặc định vì ghi đè clipboard mỗi lần.

## 5. Cấu trúc mã nguồn

```
.github/workflows/build.yml   build trên cloud, đăng APK lên Releases
app/debug.keystore            khóa ký debug cố định
app/src/main/java/vn/xin/alexreminder/
  MainActivity.kt    giao diện cài đặt
  Scheduler.kt       tính mốc kế tiếp và đặt alarm
  Receivers.kt       AlarmReceiver (alarm nổ), BootReceiver (đặt lại alarm)
  Notifier.kt        kênh và nội dung thông báo
  LaunchActivity.kt  nhận cú chạm thông báo, mở Gemini, tự đóng
  GeminiLauncher.kt  bốn cách mở Gemini (A–D)
  Prefs.kt           lưu cài đặt
```
