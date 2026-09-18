# Ammu — Native Android Offline-First Audio Player

**Ammu** is a production-ready, 100% offline-first native Android audio player built with **Kotlin** and **Jetpack Compose** (targeting API 26+). It faithfully replicates the web audio player project down to every micro-interaction, gesture, DSP effect, and cryptographic safeguard.

---

## Key Highlights & Architecture

### 1. Pure AMOLED Midnight Black UI (`#000000`)
- **Aesthetic**: Deep blacks (`#000000`) tailored for AMOLED power efficiency and high-contrast neon accents (Cyan `#00E5FF`, Pink `#FF2A6D`, Purple `#9D4EDD`, Red `#FF1744`).
- **Material 3 & Jetpack Compose**: Reactive StateFlow architecture with seamless animations and zero external theme overhead.
- **Clutter Cleaner**: Real-time "✨ Clean" button removing junk video tags (`[Official Music Video]`, `(Lyrics)`, `4K`, `HD`, `ft.`, etc.) from song titles.
- **Multi-Select Batch Actions**: Select All, Add to Custom Playlist, and Bulk Delete with a 5-second Undo Snackbar.

### 2. Studio Audio Engine & Dual Equalizer
- **Media3 ExoPlayer & MediaSessionService**:
  - Full background playback with persistent media notifications and lock-screen controls.
  - Automatic `AudioFocus` handling (audio ducking during notifications, pause on phone calls).
  - Audio becoming noisy handling (unplugging headphones or disconnecting Bluetooth immediately pauses playback).
  - CPU WakeLock and high-performance WifiLock to ensure uninterrupted offline playback.
- **Dual-Mode Equalizer Engine**:
  - **Native VLC Mode**: Interfaces directly with Android hardware `AudioEffect.Equalizer`.
  - **Vivo Studio DSP Mode**: Software Direct-Form Biquad peaking equalizer based on Robert Bristow-Johnson's Audio EQ Cookbook.
  - **Real-Time Bezier Spline Canvas**: Dynamic cubic Bézier curve frequency response rendering with neon glow gradients.
  - **Auto Headroom Guard**: Automatically attenuates master playback by -20% (-2.0dB) when EQ is enabled to completely prevent digital clipping distortion.
  - **JSON Preset Import & Export**: Flat, Bass Boost, Vocal Booster, Rock, Pop, Jazz, Electronic, and custom presets.

### 3. Gesture Micro-Interactions & Player Sheet
- **Vinyl Disc Mode**: Tap the album artwork to toggle between modern rounded card art and a realistic spinning vinyl record complete with concentric light-sheen grooves and center spindle hole.
- **Double-Tap Seek**:
  - Left 50% double-tap: Seek backwards -10s with floating animated `⏪ -10s` feedback pill.
  - Right 50% double-tap: Seek forwards +10s with floating animated `⏩ +10s` feedback pill.
- **Interactive Scrubber**: Draggable progress thumb with visual timestamp marker pins displayed directly on the timeline.
- **Heart Burst Particle Explosion**:
  - Favorite added: Heart particle explosion with emotional toast: *"Thank you for loving me 🥺❤️"*
  - Favorite removed: Broken heart particles with toast: *"Dil tod diya na mera 😿💔"*
- **Two-Zone In-Card Queue**:
  - **Zone A (Drag & Reorder)**: Long-press to drag with red inline drop-indicator target line and floating preview elevation.
  - **Zone B (Action Buttons)**: Dedicated Shift Up, Shift Down, and Remove buttons ensuring zero accidental swaps while scrolling.
- **Sub-Drawers**:
  1. Volume Slider
  2. Dual EQ with Bezier spline canvas
  3. Timestamp Markers (Jump-to points, color-coded)
  4. A-B Loop Station (Point A, Point B, repeat segment)
  5. Offline Lyrics & Personal Notes Editor
  6. Two-Zone Active Queue
  7. Real MP3 Audio Trimmer / Slicer

### 4. 4-Key Security Suite & Vault Backup
- **4-Key Cryptographic Suite**:
  1. **Master Key**: Root administrator secret.
  2. **AES-256-GCM Key**: Authenticated symmetric encryption (`AES/GCM/NoPadding`, PBKDF2WithHmacSHA256, 12-byte IV, 128-bit authentication tag).
  3. **Creator Passkey**: Author signature.
  4. **Download Key**: Granular asset download control.
- **Granular Matrix Export**: Scoped playlist export, optional embedded Base64 audio blobs, trimmed clips, lyrics, and markers.
- **Dual-Phase Import Modal**:
  - **Phase 1**: AES-256-GCM decryption with Admin Super-Key recovery guard (*"There is no super access. You have to put keys to get access."*).
  - **Phase 2**: Storage Matcher verification audit displaying `✓ Available` / `✕ Missing` badges before merging data into Room.
- **Storage Auditor & Duplicate Cleaner**: Scans internal audio blobs for identical SHA-256 signatures and file sizes, grouping duplicates and safely purging redundant blobs.
- **Real Audio Trimmer**: Native `MediaExtractor` and `MediaMuxer` frame slicer outputting persistent audio clip files.

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── drawable/ic_ammu_logo.xml
│   └── values/{colors.xml, strings.xml, themes.xml}
└── java/com/ammu/player/
    ├── AmmuApplication.kt
    ├── MainActivity.kt
    ├── audio/
    │   ├── AmmuAudioController.kt
    │   ├── AmmuMediaService.kt
    │   ├── AudioFocusHelper.kt
    │   └── dsp/
    │       ├── BiquadFilter.kt
    │       └── DualEqualizerEngine.kt
    ├── data/
    │   ├── backup/
    │   │   ├── BackupExportManager.kt
    │   │   ├── BackupModels.kt
    │   │   └── DualPhaseImportManager.kt
    │   ├── db/
    │   │   ├── AmmuDatabase.kt
    │   │   ├── AuditLogDao.kt
    │   │   ├── FavoriteDao.kt
    │   │   ├── LyricsDao.kt
    │   │   ├── PlaylistDao.kt
    │   │   ├── TimestampDao.kt
    │   │   ├── TrackDao.kt
    │   │   └── TrimmedClipDao.kt
    │   ├── model/
    │   │   ├── AuditLogEntity.kt
    │   │   ├── FavoriteEntity.kt
    │   │   ├── LyricsEntity.kt
    │   │   ├── PlaylistEntity.kt
    │   │   ├── PlaylistTrackCrossRef.kt
    │   │   ├── TimestampEntity.kt
    │   │   ├── TrackEntity.kt
    │   │   └── TrimmedClipEntity.kt
    │   ├── security/
    │   │   ├── AesGcmEngine.kt
    │   │   └── SecurityManager.kt
    │   ├── storage/
    │   │   └── StorageAuditor.kt
    │   └── trimmer/
    │       └── AudioTrimmer.kt
    └── ui/
        ├── components/
        │   ├── HeartBurstOverlay.kt
        │   ├── MiniPlayerBar.kt
        │   ├── QueueTwoZoneList.kt
        │   ├── VinylDiscView.kt
        │   └── WaveformScrubber.kt
        ├── dialogs/
        │   ├── BackupExportImportDialog.kt
        │   ├── DualEqualizerView.kt
        │   └── StorageAuditorDialog.kt
        ├── screens/
        │   ├── AmmuMainScreen.kt
        │   └── FullscreenPlayerSheet.kt
        ├── theme/
        │   ├── Color.kt
        │   ├── Theme.kt
        │   └── Type.kt
        └── viewmodel/
            ├── AmmuViewModel.kt
            └── AmmuViewModelFactory.kt
```

---

## How to Open and Run

1. Open **Android Studio** (Hedgehog 2023.1.1 or newer).
2. Select **Open** and select `c:\delta batch\musicapp`.
3. Gradle will automatically sync dependencies via `libs.versions.toml`.
4. Run on any Android device or emulator running **API 26 (Android 8.0)** or higher (up to Android 15 / API 35).
