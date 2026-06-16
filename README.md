# NeuraShhield

NeuraShhield is an Android notification attention filter that classifies notifications on-device, keeps urgent interruptions visible, and moves non-urgent notifications into a managed feed and digest.

## Privacy Model

- Notification content is processed locally on the device.
- ML inference runs on-device with TensorFlow Lite.
- Notification title/body/sender content is not sent to any server.
- Local notification history is stored in an encrypted Room database using SQLCipher.

## Current Behavior

- `URGENT` notifications remain visible in the Android notification shade.
- `IMPORTANT` and `LOW` notifications are silenced from the shade and stored in the managed feed/digest.
- OTP notifications are detected locally and copied to the clipboard.
- Spam-like notifications are logged for audit.
- The quick settings tile and home screen widget can toggle the AI filter.

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM + Use Cases + Repository pattern
- Coroutines + Flow
- Room + SQLCipher
- Proto DataStore
- TensorFlow Lite + ML Kit
- WorkManager
- JUnit 5, MockK, Espresso/Compose UI tests

## Build

```bash
./gradlew :app:assembleDebug
```

## Test

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
```

## ML Assets

The app ships a local TFLite classifier at:

```text
app/src/main/assets/notification_classifier.tflite
```

Training scripts and metadata live under:

```text
ml/training
ml/models
```

Datasets are intentionally ignored by Git and should be kept outside the repository unless they are sanitized and explicitly approved for publishing.
