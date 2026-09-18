# Consumely 🥫🥶

**Consumely** is a local-first, privacy-focused Android application designed to track food items across your **Pantry**, **Fridge**, **Freezer**, and **Basement**. It features smart age and expiration alerts, multi-device cloud synchronization via **JSONBin.io**, on-device barcode product scanning powered by **Google ML Kit**, automated product name lookup via **Open Food Facts**, and native multi-language support (English & German).

---

## 🌟 Key Features

- **Offline-First Inventory Management**: Powered by Room Database for instantaneous UI performance without loading spinners.
- **Smart Location Rules & Freezer Age Badges**:
  - Pre-populated default locations (*Pantry, Fridge, Freezer, Basement*).
  - Items stored in **Freezer** locations automatically display **Yellow Warning Badges** (at 6 months) and **Red Alert Badges** (at 9 months) when no explicit expiration date is specified.
  - Explicit expiration dates take precedence over default age rules.
- **Quantity Steppers**: Quick `+` and `-` buttons on item cards to easily adjust stock counts.
- **On-Device Barcode Scanner & Product Lookup**:
  - Uses CameraX and **Google ML Kit Barcode Scanning** running 100% on-device (no cloud AI keys required).
  - Queries the **Open Food Facts** database (`ch.openfoodfacts.org`) to auto-fill German or English product names.
- **Multi-Device Cloud Synchronization**:
  - Two-way delta sync via **JSONBin.io** using Last-Write-Wins (LWW) timestamp conflict resolution.
  - Automatic in-app sync on app startup and whenever data changes are saved.
- **Single Expiration Push Notifications**:
  - Sends a single system notification per item when approaching expiration or reaching freezer age thresholds (prevents daily notification spam).
- **System Language Localization**:
  - Full native translations for **English** and **German** (`values/strings.xml` and `values-de/strings.xml`).

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose Material 3](https://developer.android.com/jetpack/compose)
- **Language**: Kotlin 2.x
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room) (with KSP schema export and AutoMigrations)
- **Networking**: Retrofit 2 + OkHttp 4 + `kotlinx.serialization`
- **Camera & ML**: CameraX + [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
- **Navigation**: Jetpack Compose Navigation
- **Architecture Pattern**: MVVM with reactive `Flow` data streams

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer
- **Android SDK**: `compileSdk = 37`, `minSdk = 26` (Android 8.0+)
- **JDK**: Java 17

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/Consumely.git
   cd Consumely
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## ⚙️ Cloud Sync Setup (JSONBin.io)

1. Create a free account on [JSONBin.io](https://jsonbin.io).
2. Generate an **Access Key** or **Master Key** under **API Keys**.
3. Create a new Bin (or let Consumely initialize a new Bin ID).
4. In Consumely, tap the **Settings** gear icon in the top-right corner, enter your **Access Key** and **Bin ID**, and tap **Sync Now**.

---

## 📜 License

Distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for more details.
