<p align="center">
  <img src="assets/logo.png" alt="Cine-Sakura Logo" width="160" style="border-radius: 28px;" />
</p>

<h1 align="center">🌸 Cine-Sakura</h1>

<p align="center">
  <b>A modern, high-definition streaming and media discovery app for Android.</b><br>
  Built 100% native with <b>Kotlin</b>, <b>Jetpack Compose</b>, and <b>Material 3</b>.
</p>

<p align="center">
  <a href="https://github.com/d4rxh/Cine-sakura/actions"><img src="https://img.shields.io/github/actions/workflow/status/d4rxh/Cine-sakura/build-apk.yml?branch=main&label=Build%20APK&style=for-the-badge&logo=githubactions&logoColor=white&color=ff7597" alt="Build Status" /></a>
  <a href="https://github.com/d4rxh/Cine-sakura/releases/latest"><img src="https://img.shields.io/github/v/release/d4rxh/Cine-sakura?style=for-the-badge&logo=android&logoColor=white&color=ec407a&label=Version" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20First-success?style=for-the-badge&logo=shield&logoColor=white" alt="Privacy" />
</p>

---

## 📲 Download APK Directly

Download the ready-to-install Android APK directly from GitHub Releases:

<p align="center">
  <a href="https://github.com/d4rxh/Cine-sakura/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20Release%20APK-ff2d55?style=for-the-badge&logo=googleplay&logoColor=white" height="48" alt="Download Latest Release APK" />
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/d4rxh/Cine-sakura/releases/download/v1.0.2/app-debug.apk">
    <img src="https://img.shields.io/badge/Direct%20APK-Debug%20Build-4a148c?style=for-the-badge&logo=android&logoColor=white" height="48" alt="Direct APK Debug" />
  </a>
</p>

> [!TIP]
> **How to install on Android:**
> 1. Tap **Download Latest Release APK** above on your Android phone.
> 2. Open the downloaded `.apk` file.
> 3. If prompted, allow *"Install unknown apps"* for your browser or file manager.
> 4. Tap **Install** and enjoy watching!

---

## ✨ Highlights & Features

### 🎬 Infinite Catalog & Instant Search
- **Cinemeta Engine:** High-speed, live metadata scraping for trending movies, top-rated TV series, and popular anime.
- **Instant Search:** Lightning-fast search across thousands of movies and TV shows with real-time query filtering.
- **Detailed Media Sheets:** Posters, banners, IMDB ratings, synopsis, release year, runtime, and genre tags.

### ⚡ Multi-Server Playback Engine
- **Fail-Safe Server Switcher:** Seamlessly switch between multiple streaming hosts inside the player:
  - **VidLink Pro:** Ultra HD multi-quality player with dual audio and subtitle tracks.
  - **VidSrc Prime:** High-speed streaming source with broad catalog coverage.
  - **2Embed:** Fast reliable mirror server.
  - **111Movies:** Indian & regional dubbed audio streams.
- **Ad & Popup Shield:** Automated popup suppression and sandbox protection for clean, uninterrupted viewing.
- **Hardware-Accelerated Fullscreen:** Native landscape rotation, immersive status bar hiding, and full hardware video decoding.

### 📺 TV Series Episode Navigator
- Multi-season support with dynamic episode listing.
- 1-tap playback from any episode with automatic history logging.

### 📚 Personal Library & Watch History
- **Watchlist (Bookmarks):** Save any movie or series with a single tap for later viewing.
- **Resume History:** Automatically tracks recently played movies and series episodes with exact timestamps and 1-tap resume.
- **100% Client-Side Storage:** All library data is saved locally on your device in secure JSON format.

### 🛡️ Privacy & Aesthetics
- **Zero Accounts & Zero Tracking:** No logins, no passwords, no analytics trackers, no telemetry.
- **Sakura & Obsidian UI:** Tailored dark mode theme designed with Japanese cherry blossom pink accents and deep OLED black surfaces.

---

## 🏗️ Architecture & Tech Stack

Cine-Sakura is built following modern Android development best practices and Clean MVVM architecture:

```
app/
├── domain/
│   ├── model/             # MediaItem, MediaDetail, Episode, StreamServer, History
│   └── repository/        # MovieRepository (Cinemeta API & Stream servers), WatchlistRepository
├── ui/
│   ├── components/        # HeroSpotlight, MediaCard, MediaRow, SectionHeader
│   ├── navigation/        # BottomNav, Screen routes, Navigation graph
│   ├── screens/
│   │   ├── home/          # HomeScreen & HomeViewModel
│   │   ├── search/        # SearchScreen & SearchViewModel
│   │   ├── details/       # DetailsScreen & DetailsViewModel
│   │   ├── player/        # PlayerScreen (Hardware accelerated webview + server bar)
│   │   ├── watchlist/     # WatchlistScreen & WatchlistViewModel
│   │   └── settings/      # SettingsScreen
│   └── theme/             # Material 3 Sakura Rose & Obsidian Dark Theme
└── CineSakuraApp.kt       # Application class & DI singleton container
```

- **Language:** Kotlin 2.0.21
- **UI Toolkit:** Jetpack Compose with Material Design 3
- **Network & Scraping:** OkHttp 4.12.0 + Cinemeta API
- **Image Loading:** Coil 2.7.0 (with hardware bitmap caching)
- **Asynchronous Flow:** Kotlin Coroutines & StateFlow
- **Navigation:** Jetpack Navigation Compose
- **Build System:** Gradle 8.11.1 with Kotlin DSL (`build.gradle.kts`)

---

## 🤖 Automated CI/CD (GitHub Actions)

Every push to `main` and every tag push (`v*`) automatically triggers GitHub Actions to:
1. Set up JDK 17 (Temurin).
2. Run Gradle assemble tasks for both **Debug** and **Release** builds.
3. Upload APK artifacts with a 30-day retention window.
4. Publish a new GitHub Release with release notes and downloadable APKs.

You can inspect the pipeline at [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17+
- Android SDK 35 (minSdk 24)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/d4rxh/Cine-sakura.git
cd Cine-sakura

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The output APKs will be located in `app/build/outputs/apk/`.

---

## ⚖️ Legal Disclaimer

Cine-Sakura does not host, upload, or store any video files on any servers. The application scrapes and formats publicly available metadata from third-party services and loads public streaming embed links provided by third-party mirrors. All trademarks, logos, and copyrights belong to their respective owners.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
