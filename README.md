# WarX Downloader

Aplikasi pengunduh video native Android berperforma tinggi berbasis **Jetpack Compose** dan **Material 3**. Dirancang dengan arsitektur **Zero Transcoding** untuk mengunduh stream langsung (direct MP4/WebM) dan HLS (HTTP Live Streaming) secara instan, menggabungkan segmen `.ts` secara native, serta mendukung pemutaran langsung dan riwayat unduhan lokal dengan Room Database.

---

## 🚀 Ringkasan Pembaruan & Peningkatan Arsitektur

### 1. Sistem Layout Responsif & Presisi Layar (Adaptive UI)
Sebelumnya tampilan aplikasi dapat terpotong poni (notch), gesture navigation bar, atau melebar secara canggung di layar besar. Kini tata letak telah disempurnakan:
* **Penanganan WindowInsets Menyeluruh**:
  * Menggunakan `WindowInsets.statusBars` dan `WindowInsets.displayCutout` sehingga header, judul, dan dialog tidak lagi tertutup kamera depan (notch) atau status bar.
  * Menggunakan `WindowInsets.navigationBars` pada seluruh navigasi mengambang (*floating bottom bar*) dan tombol aksi agar tidak tumpang tindih dengan garis navigasi gestur (gesture pill) maupun 3-button navigation.
  * Menggunakan `Modifier.imePadding()` agar keyboard virtual tidak menutupi kolom input URL atau tombol analisis.
* **Skala Spacing Dinamis Berbasis Layar**:
  * **Compact Phone (< 600dp)**: Padding horizontal konsisten 16dp, kartu proporsional dengan rasio layar.
  * **Medium / Foldable (600–840dp)**: Padding horizontal 24dp.
  * **Expanded / Tablet (> 840dp / Landscape)**: Konten utama terpusat dengan batas maksimal lebar (`widthIn(max = 760.dp)`), serta otomatis beralih menggunakan **Glass Navigation Rail** di sebelah kiri yang ergonomis.

---

### 2. Zero-Crash & Audit Stabilitas Menyeluruh
Semua potensi *force close* (crash) yang terjadi akibat *race condition*, null pointer, atau siklus background telah dieliminasi:
* **Pemutusan Loop Rekursif DownloadManager ↔ ForegroundService**:
  * Memisahkan *Job Lifecycle*, *Service Lifecycle*, dan *Notification Lifecycle*.
  * Menambahkan **proteksi duplicate job** di `DownloadManager`: jika proses unduhan dengan ID yang sama masih aktif, permintaan baru tidak akan memicu eksekusi ganda.
  * `DownloadForegroundService` tidak lagi memanggil `startDownload()` di `ACTION_START`. Fungsinya murni mengelola notifikasi foreground, wake lock, dan meneruskan aksi kontrol (*Pause*, *Resume*, *Cancel*).
  * Kompatibel dengan Android 12+ (menangani `ForegroundServiceStartNotAllowedException`) dan Android 14+ (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`).
* **Eliminasi Total Double-Bang (`!!`) Assertions**:
  * Mengganti seluruh pembacaan state dialog dan modal yang rentan `NullPointerException` (seperti `previewVideo!!`, `playerTarget!!`, `itemToDelete!!`) dengan *safe immutable snapshot variables*.
* **Crash-Safe ExoPlayer (`VideoPlayerModal`)**:
  * Melakukan validasi fisik berkas (`File.exists()`) sebelum mencoba memutar video lokal.
  * Menambahkan `Player.Listener` yang menangkap `PlaybackException` (kesalahan jaringan, berkas rusak, atau codec tidak didukung) dan menampilkannya dalam UI error yang rapi, bukan force close.
  * Pelepasan memori `exoPlayer.release()` terjamin lewat `DisposableEffect`.
* **Akses File & FileProvider Aman**:
  * Verifikasi ketersediaan berkas sebelum melakukan pemutaran atau pembagian (*share video*). Jika berkas telah dihapus dari luar aplikasi, sistem memberikan pesan Toast yang informatif.

---

### 3. Design System Premium Glassmorphism
Antarmuka dibangun dengan sistem desain transparan bertingkat (*frosted glass*):
* **Komponen Reusable**:
  * `GlassScaffold`: Kerangka responsif dengan gradien ambient glow dan insets otomatis.
  * `GlassCard`: Kartu semi-transparan dengan border luminous halus dan sudut melengkung konsisten (16–22dp).
  * `GlassButton`: Tombol bergradasi cyan/violet dengan umpan balik sentuh taktil (`bounceClick` scale animation) dan status loading.
  * `GlassIconButton`: Tombol aksi 48dp sesuai standar aksesibilitas Material Design 3.
  * `GlassTextField`: Kolom input transparan dengan glowing border saat aktif dan tombol instan Tempel/Hapus.
  * `GlassBottomBar`: Navigasi bawah mengambang dengan indikator pill animasi dan badge penghitung unduhan aktif secara realtime.
  * `GlassDialog`: Dialog pop-up frosted dengan perlindungan display cutout.
  * `GlassChip`: Lencana penanda resolusi, bitrate, codec, format, dan uploader.
  * `GlassProgress`: Bar progres animasi halus tanpa lonjakan kasar.

---

### 4. Persistent Theme System (System / Light / Dark)
Pilihan tema disimpan secara permanen di perangkat melalui `ThemePreferences`:
* **Sistem**: Menyesuaikan secara otomatis dengan preferensi tema perangkat Android.
* **Terang (Light)**: Tampilan *Frosted Arctic Glass* yang bersih dengan tipografi slate tua (`#0F172A`), border luminous biru samudra, dan kartu putih semi-transparan.
* **Gelap (Dark)**: Tampilan *Deep Space Obsidian* dengan aksen neon cyan (`#00F0FF`) dan cyber violet (`#8A2BE2`).
* Semua komponen menggunakan token semantik dari `WarXTheme.colors` tanpa hardcoded `Color.White`/`Color.Black`.

---

### 5. Arsitektur Inti Ekstraksi & Unduhan
* **Deep Extraction Engine**:
  * Pemindaian DOM HTML, OpenGraph, JSON-LD, pemutar video berbasis JavaScript (JWPlayer, VideoJS, Hls.js), iframe bersarang, dan skrip *packed eval*.
  * Deteksi otomatis HLS Master Playlist (`.m3u8`), parsing varian resolusi (240p hingga 4K), audio tracks, dan subtitle.
  * Penanganan decoding URL percent-encoded dan karakter escape Unicode.
* **Native TS Concatenation**:
  * Mengunduh dan menggabungkan segmen transport stream secara langsung ke penyimpanan lokal tanpa transcoding, menghemat daya baterai dan CPU secara maksimal.
  * Mendukung dekripsi segmen terenkripsi **AES-128** secara native.
  * Mendukung jeda (*pause*) dan melanjutkan (*resume*) unduhan.
* **Room Database**:
  * Menyimpan seluruh riwayat unduhan, metadata, ukuran berkas, dan tanggal penyelesaian secara lokal dan aman.

---

## 🛠️ Build & Uji Coba

### Menjalankan Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Build APK Debug
```bash
gradle assembleDebug
```

### Build APK Release
```bash
gradle assembleRelease
```
*Catatan: Konfigurasi GitHub Actions CI (`.github/workflows/build.yml`) telah dilengkapi pembuatan keystore otomatis via `keytool` sehingga proses build di CI berjalan tanpa kegagalan signing.*
