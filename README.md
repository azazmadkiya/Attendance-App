# 📱 Attendance App (Haazri & Payroll Pro)

[![Android CI](https://github.com/azazmadkiya/Attendance-App/actions/workflows/android-build.yml/badge.svg)](https://github.com/azazmadkiya/Attendance-App/actions/workflows/android-build.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-34A853.svg)](https://m3.material.io)
[![Room DB](https://img.shields.io/badge/Database-Room%20SQLite-F58220.svg)](https://developer.android.com/training/data-storage/room)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An all-in-one Android workforce management, daily attendance logging, wage & overtime calculation, and petty cash ledger application built specifically for small business owners, contractors, factory managers, and site supervisors.

---

## ✨ Features

- **⚡ Instant Daily Attendance**: Mark **Present (P)**, **Absent (A)**, **Half-Day (HD)**, or **Overtime (OT)** with single-tap controls.
- **💰 Flexible Wage Models**: Supports **Daily Wages**, **Monthly Fixed Salary**, **Per Hour Rates**, and **Piece Rate / Per Unit** calculations.
- **📍 Geofenced Attendance Verification**: Prevent buddy punching or fake check-ins by locking attendance marking within a designated work site GPS perimeter.
- **📒 Digital Cashbook & Ledger**: Track salary advance payments, bonus disbursements, deductions, and petty cash expenses with running balances.
- **📊 Automated Reports & Salary Slips**:
  - Export monthly attendance registers in **PDF** and **Excel (.csv)** formats.
  - Generate instant worker salary slips and share via **WhatsApp** or Email.
- **🌐 Multi-Language Support**: Fully localized in **English**, **Hindi (हिंदी)**, **Gujarati (ગુજરાતી)**, and **Marathi (मराठी)**.
- **🔒 Privacy & Security First**:
  - **Local-first SQLite architecture (Room DB)** for 100% offline functionality and maximum data privacy.
  - **4-Digit PIN App Lock** to safeguard sensitive payroll records.
- **☁️ Cloud Sync & Backups**: Secure Google Sign-In and JSON-based local/cloud backup and restore.
- **⚖️ Google Play Store Ready**: Includes complete in-app and web-hosted **Privacy Policy**, **Terms & Conditions**, and **Data Safety** documentation.

---

## 🛠️ Tech Stack & Architecture

- **Language:** [Kotlin](https://kotlinlang.org) (100%)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **Database:** [Room Database (SQLite)](https://developer.android.com/training/data-storage/room) with KSP code generation
- **Authentication:** Firebase Auth & Jetpack Credential Manager (Google Sign-In)
- **Location Services:** Google Play Services Location API (FusedLocationProviderClient)
- **CI/CD:** GitHub Actions workflow for automated APK building and GitHub Releases
- **Hosting:** GitHub Pages for Play Store Legal & Privacy Policy portal

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **JDK 17** or higher.
- Android device or emulator running **Android 7.0 (API Level 24)** or higher.

### Installation & Build

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/azazmadkiya/Attendance-App.git
   cd Attendance-App
   ```

2. **Open in Android Studio:**
   - Launch Android Studio and choose **Open an Existing Project**.
   - Select the cloned root directory and let Gradle sync dependencies automatically.

3. **Run on Device/Emulator:**
   - Select your connected device or emulator.
   - Click the green **Run (Shift+F10)** button.

4. **Build APK via Command Line:**
   ```bash
   ./gradlew :app:assembleDebug
   # APK location: app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🤖 Automated CI/CD (GitHub Actions)

This repository includes a pre-configured GitHub Actions workflow (`.github/workflows/android-build.yml`):
- **Automatic APK Compilation**: Compiles on every push or pull request to `main`.
- **Downloadable Artifacts**: Directly download `app-debug.apk` from the **Actions** tab.
- **GitHub Releases**: Manually trigger **Workflow Dispatch** to auto-generate a tagged release with attached APK.

---

## 📜 Legal & Google Play Store Links

| Document | Link |
| :--- | :--- |
| **Privacy Policy** | [Privacy Policy](https://azazmadkiya.github.io/Attendance-App/privacy-policy.html) |
| **Terms of Service** | [Terms & Conditions](https://azazmadkiya.github.io/Attendance-App/terms.html) |
| **Data Safety & Deletion** | [Account & Data Deletion](https://azazmadkiya.github.io/Attendance-App/data-deletion.html) |
| **Policy Center Portal** | [Documentation Portal](https://azazmadkiya.github.io/Attendance-App/) |

---

## 👨‍💻 Author & Support

- **Developer:** Azaz Madkiya
- **Email:** [azazmadkiya@gmail.com](mailto:azazmadkiya@gmail.com)
- **Project:** Attendance App (Haazri & Payroll Pro)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
