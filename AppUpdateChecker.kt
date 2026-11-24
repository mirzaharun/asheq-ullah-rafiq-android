package com.ashequllah.portal.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ashequllah.portal.data.model.AppReleaseInfo
import java.io.File
import java.security.MessageDigest

fun isNewerReleaseAvailable(
    installedVersionCode: Long,
    release: AppReleaseInfo?
): Boolean {
    if (release == null || !release.is_published || !release.update_available) return false
    if (release.package_name != "com.ashequllah.portal") return false
    if (release.download_url.contains("firebasestorage.googleapis.com")) return false
    if (release.download_url.contains("firebasestorage.app")) return false
    if (!release.download_url.startsWith("https://")) return false
    return release.version_code > installedVersionCode ||
        installedVersionCode < release.minimum_supported_version
}

fun mustForceUpdate(
    installedVersionCode: Long,
    release: AppReleaseInfo?
): Boolean {
    if (release == null) return false
    return release.force_update || installedVersionCode < release.minimum_supported_version
}

fun enqueueOfficialApkDownload(
    context: Context,
    release: AppReleaseInfo,
    onInstallReady: ((File) -> Unit)? = null,
    onIntegrityFailed: (() -> Unit)? = null
): Long {
    val fileName = release.file_name.ifBlank { "AsheqUllahRafiq.apk" }
    val destinationFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        fileName
    )
    if (destinationFile.exists()) {
        destinationFile.delete()
    }

    val request = DownloadManager.Request(Uri.parse(release.download_url))
        .setTitle("${release.app_name} v${release.latest_version}")
        .setDescription("Downloading official APK from GitHub Releases")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.fromFile(destinationFile))

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = dm.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (completedId != downloadId) return
            runCatching { context.applicationContext.unregisterReceiver(this) }

            val expectedSha = release.checksum_sha256.trim()
            if (expectedSha.isNotEmpty() && !verifyFileSha256(destinationFile, expectedSha)) {
                destinationFile.delete()
                onIntegrityFailed?.invoke()
                return
            }

            if (onInstallReady != null) {
                onInstallReady(destinationFile)
            } else {
                installDownloadedApk(context, destinationFile)
            }
        }
    }

    ContextCompat.registerReceiver(
        context.applicationContext,
        receiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        ContextCompat.RECEIVER_EXPORTED
    )

    return downloadId
}

fun verifyFileSha256(file: File, expectedHex: String): Boolean {
    if (!file.exists()) return false
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read = input.read(buffer)
        while (read != -1) {
            digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    }
    val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
    return actualHex.equals(expectedHex.trim(), ignoreCase = true)
}

fun installDownloadedApk(context: Context, apkFile: File) {
    if (!apkFile.exists()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)
        return
    }

    val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(installIntent)
}

fun openFallbackBrowserDownload(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
fun AppUpdateDialog(
    installedVersionCode: Long,
    release: AppReleaseInfo,
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onDownloadClicked: () -> Unit
) {
    val forceRequired = mustForceUpdate(installedVersionCode, release)

    AlertDialog(
        onDismissRequest = {
            if (!forceRequired) onDismiss()
        },
        title = {
            Text(
                text = if (isBengali) {
                    release.release_title_bn.ifBlank { "অ্যাপের নতুন সংস্করণ উপলব্ধ (${release.latest_version})" }
                } else {
                    release.release_title_en.ifBlank { "New App Update Available (${release.latest_version})" }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (isBengali) release.release_notes_bn else release.release_notes_en.ifBlank { release.release_notes_bn },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (release.file_size.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "APK: ${release.file_name} · ${release.file_size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownloadClicked) {
                Text(if (isBengali) "এখনই আপডেট করুন" else "Update Now")
            }
        },
        dismissButton = {
            if (!forceRequired) {
                OutlinedButton(onClick = onDismiss) {
                    Text(if (isBengali) "পরে করব" else "Later")
                }
            }
        }
    )
}
