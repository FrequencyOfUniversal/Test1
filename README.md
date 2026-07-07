# Music Player (Android) — Kotlin + Jetpack Compose + Media3

## Lưu ý quan trọng
Project này **không sao chép code/giao diện độc quyền** của bất kỳ app cụ thể nào (Pulsar hay app khác).
Nó cung cấp bộ chức năng **tương đương** một app nghe nhạc phổ thông:

- Quét toàn bộ nhạc trên máy qua `MediaStore`
- Danh sách bài hát, mini player, điều khiển play/pause/next
- Phát nhạc nền qua `MediaSessionService` (Media3/ExoPlayer) — có notification điều khiển
- **Tính năng riêng theo yêu cầu:** Auto DJ-Mix — tự động chuyển bài mượt với các hiệu ứng:
  - `CROSSFADE`: hoà tiếng tuyến tính
  - `ECHO_OUT`: bài cũ tắt dần kiểu dư âm
  - `FILTER_SWEEP`: bài mới vào kiểu "mở lọc" dần
  - `BEATMATCH_CUT`: cắt nhanh giữa đoạn chuyển

## Cấu trúc
```
app/src/main/java/com/example/musicplayer/
├── data/          # Song model + MusicRepository (quét MediaStore)
├── player/        # PlaybackService (MediaSessionService nền)
├── dj/            # DjTransitionEngine — lõi xử lý 2 deck ExoPlayer + hiệu ứng
└── ui/            # MainActivity (Compose UI) + PlayerViewModel
```

## Cách build

### A. Bằng Android Studio (nếu có máy tính)
1. Mở thư mục này bằng **Android Studio (Koala trở lên)**.
2. Để Gradle tự sync (cần mạng để tải dependency lần đầu).
3. Chạy trên thiết bị/emulator Android 7.0 (API 24) trở lên.
4. Cấp quyền đọc nhạc khi app yêu cầu.

### B. Build APK từ điện thoại, không cần máy tính (GitHub Actions)
Project đã có sẵn `.github/workflows/build.yml` để tự build file APK trên cloud của GitHub.

1. Giải nén file zip này bằng app quản lý file trên điện thoại (Files, ZArchiver, hoặc app có sẵn của Honor).
2. Vào **github.com** bằng trình duyệt điện thoại (hoặc app GitHub) → đăng nhập/tạo tài khoản miễn phí.
3. Tạo repository mới (nút **New repository**), đặt tên tuỳ ý, để **Public**, không tick thêm README.
4. Vào repo vừa tạo → **Add file > Upload files** → chọn toàn bộ các file/thư mục đã giải nén ở bước 1
   (trình duyệt Chrome trên Android cho phép chọn cả thư mục, giữ nguyên cấu trúc).
5. Nhấn **Commit changes**. Việc push code sẽ tự động kích hoạt GitHub Actions.
6. Vào tab **Actions** ở đầu trang repo → chờ job "Build Debug APK" chạy xong (khoảng 3–5 phút, có dấu ✓ xanh).
7. Bấm vào job vừa chạy xong → kéo xuống mục **Artifacts** → tải file `app-debug-apk` (dạng .zip) về máy.
8. Giải nén file zip đó ra để lấy `app-debug.apk`.
9. Vào **Cài đặt > Bảo mật** trên Honor X50, bật **"Cài đặt ứng dụng không rõ nguồn gốc"** cho app quản lý file bạn dùng.
10. Mở `app-debug.apk` bằng app quản lý file để cài đặt.

> Đây là bản **debug APK**, dùng để test trên máy bạn — chưa ký để phát hành lên Play Store.

## Hướng mở rộng tiếp theo (gợi ý)
- Phân tích BPM thật (dùng thư viện như `TarsosDSP` hoặc xử lý FFT thủ công) để `DjTransitionEngine`
  canh đúng nhịp thay vì chỉ dựa vào thời gian còn lại.
- Thêm equalizer thật bằng `android.media.audiofx.Equalizer` gắn vào audio session của ExoPlayer.
- Thêm màn hình Playlist, Album, Artist (hiện mới có danh sách phẳng tất cả bài hát).
- Lưu trạng thái autoMix/effect vào DataStore để giữ cấu hình giữa các lần mở app.
- Widget màn hình khoá / home screen.
