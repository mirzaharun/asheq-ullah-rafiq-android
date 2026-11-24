# Asheq Ullah Rafiq (`com.ashequllah.portal`) — Android Integration & Placement Guide

This workspace contains the Web Admin Panel and the Kotlin/YAML integration package under `android-integration/`. Copy the files below into your **Android Studio Kotlin repository** (`com.ashequllah.portal`).

## 1. Exact File Placement in Your Android Repository

| Source File in `android-integration/` | Exact Destination in Android Repo |
|---|---|
| `PortalModels.kt` | `app/src/main/java/com/ashequllah/portal/data/model/PortalModels.kt` |
| `PortalRepository.kt` | `app/src/main/java/com/ashequllah/portal/data/repository/PortalRepository.kt` |
| `NotificationCenterScreen.kt` | `app/src/main/java/com/ashequllah/portal/ui/notifications/NotificationCenterScreen.kt` |
| `MyFirebaseMessagingService.kt` | `app/src/main/java/com/ashequllah/portal/fcm/MyFirebaseMessagingService.kt` |
| `AppUpdateChecker.kt` | `app/src/main/java/com/ashequllah/portal/update/AppUpdateChecker.kt` |
| `release-apk.yml` | `.github/workflows/release-apk.yml` |

> **Important Bottom Navigation Preservation:** Do **NOT** alter your existing 5-tab `NavigationBar` (`Home`, `Development`, `Services`, `Media`, `More / Contact`). Open `NotificationCenterScreen` from a top-bar Bell icon (`notifications` route) with a live unread badge, and present `AppUpdateDialog` as a modal overlay when `isNewerReleaseAvailable(...)` returns `true`.

---

## 2. `AndroidManifest.xml` Required Permissions, FCM Service & `FileProvider`

Add these permissions inside `<manifest>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

Inside `<application ...>` register **both** the Firebase Messaging service and the `FileProvider`:

```xml
<service
    android:name=".fcm.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

---

## 3. Create `app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="app_downloads"
        path="Download/" />
    <external-path
        name="public_downloads"
        path="Download/" />
    <cache-path
        name="apk_cache"
        path="." />
</paths>
```

---

## 4. `app/build.gradle.kts` Signing & Firebase Dependencies

Confirm `applicationId = "com.ashequllah.portal"` and configure release signing cleanly using environment variables matching `.github/workflows/release-apk.yml`:

```kotlin
android {
    defaultConfig {
        applicationId = "com.ashequllah.portal"
        // ...
    }

    signingConfigs {
        create("release") {
            val ksFile = file("release-keystore.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (file("release-keystore.jks").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}
```

Ensure Firebase dependencies in `app/build.gradle.kts` include:
- `implementation(platform("com.google.firebase:firebase-bom:<latest>"))`
- `implementation("com.google.firebase:firebase-firestore-ktx")`
- `implementation("com.google.firebase:firebase-messaging-ktx")`
- `implementation("com.google.firebase:firebase-auth-ktx")`

---

## 5. Runtime Initialization Checklist

1. Call `MyFirebaseMessagingService.subscribeDefaultTopics()` once on app launch (`MainActivity.onCreate`) and request `Manifest.permission.POST_NOTIFICATIONS` on Android 13+ (API 33+).
2. Pass `PortalRepository(context = applicationContext)` so unauthenticated/guest users preserve local notification read state alongside Firestore sync for signed-in users.
3. Read `BuildConfig.VERSION_CODE.toLong()` and observe `portalRepository.observeLatestAppRelease()` in your root ViewModel/Composable to trigger `AppUpdateDialog` and `enqueueOfficialApkDownload(context, release)`.
4. Verify no collection or reference to `supporter_cards` remains in Kotlin source.
