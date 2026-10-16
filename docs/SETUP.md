# Setup Guide

## Prerequisites

1. **JDK 17 or 21**:
   - Recommended: Android Studio bundled JBR (e.g. `C:\Program Files\Android\Android Studio\jbr`) or OpenJDK 17+.
   - Set environment variable:
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
     $env:Path = "$env:JAVA_HOME\bin;$env:Path"
     ```

2. **Android SDK**:
   - `compileSdk = 35`
   - `targetSdk = 35`
   - `minSdk = 26` (Android 8.0 Oreo)
   - Ensure `local.properties` contains your local SDK path:
     ```properties
     sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
     ```
     *(Note: `local.properties` is ignored by git and should not be checked into source control).*

3. **Gradle**:
   - Gradle wrapper version `8.11.1` is included (`gradlew` for macOS/Linux, `gradlew.bat` for Windows).

## Building the Project

### Command Line (Windows PowerShell)
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

# Run all unit tests
.\gradlew.bat test

# Build debug APK
.\gradlew.bat assembleDebug
```

### Command Line (Linux / macOS)
```bash
# Make wrapper executable
chmod +x ./gradlew

# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```

### Generated Outputs
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test Reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Adding a TensorFlow Lite Model

1. Place your `.tflite` model (SSD MobileNet, EfficientDet, etc.) into `app/src/main/assets/` (e.g., `app/src/main/assets/ssd_mobilenet_v1.tflite`).
2. Open the app, navigate to **Settings**, and enter the asset filename in **TFLite model asset filename**.
3. Navigate to **CV Lab**, import an image, and run the pipeline.
