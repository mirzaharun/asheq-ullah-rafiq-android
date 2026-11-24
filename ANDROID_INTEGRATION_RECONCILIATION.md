# Android Integration Reconciliation Report

## Source of truth retained
The original Android project is retained as the application source. Existing UI, tabs, ViewModel, repository, Firebase messaging service, notification synchronization, and FileProvider configuration were not replaced.

## Verified identifiers
- Gradle namespace: `com.example` (matches the existing Kotlin source package declarations).
- Android applicationId: `com.ashequllah.portal`.
- `app/google-services.json` contains an Android client for `com.ashequllah.portal`, so the Firebase Android client package matches the applicationId.
- Existing FCM service: `com.example.service.PortalFirebaseMessagingService`; it is already registered in `AndroidManifest.xml`.
- Existing FileProvider: `${applicationId}.fileprovider`; existing `res/xml/provider_paths.xml` is present.
- Existing Gradle dependencies already include Firebase BOM, Firestore, Auth, Storage, Messaging, coroutines, and Compose Material 3.

## Safe changes made
- Added `REQUEST_INSTALL_PACKAGES` permission for the optional user-initiated APK update/install flow.
- Added `.github/workflows/android-build.yml`, which installs Gradle 9.3.1 and builds `:app:assembleDebug` and uploads the APK as a GitHub Actions artifact.
- Preserved the complete original project and added the separate integration ZIP contents under `android-integration-reference/` for review.

## Deliberately not auto-merged
The integration ZIP targets package paths under `com.ashequllah.portal.*`, while the actual Kotlin source namespace/package is `com.example.*`. It also defines another `PortalRepository` and models that overlap with the existing app's `PortalRepository`, `DigitalServiceItem`, and notification systems. Its FCM service would duplicate the already-registered `PortalFirebaseMessagingService`. Blindly copying these files into `app/src/main` would create duplicate/conflicting classes and risk breaking the existing UI. Therefore these files are retained as references and not compiled into the app.

The provided `AppUpdateChecker.kt` and `NotificationCenterScreen.kt` also require explicit wiring into the current MainActivity/ViewModel and a reconciled Firestore model schema. That wiring cannot be safely inferred from the integration package alone without changing existing behavior.

## Build and verification status
This package has been structurally reconciled, but an Android Gradle build has **not** been executed in this environment. GitHub Actions will run the debug build after this project is pushed to the repository. A green workflow is required before treating the APK as build-verified.

## GitHub upload
The source archive did not include the Gradle wrapper scripts/JAR, so the workflow uses the installed Gradle 9.3.1 rather than `./gradlew`.

Upload the contents of this ZIP to the root of the Android repository (not as a nested ZIP). Keep the `.github/` directory. Do not upload `android-integration-reference/` into `app/src/main`; it is documentation/reference material.
