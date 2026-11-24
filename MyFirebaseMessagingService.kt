package com.ashequllah.portal.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_URGENT = "portal_urgent_notices"
        const val CHANNEL_GENERAL = "portal_general_notices"

        fun subscribeDefaultTopics() {
            listOf(
                "all_users",
                "urgent",
                "general",
                "app_update",
                "development",
                "public_service",
                "events",
                "other"
            ).forEach { topic ->
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val title = remoteMessage.notification?.title
            ?: data["titleBn"]
            ?: data["titleEn"]
            ?: "Asheq Ullah Rafiq"
        val body = remoteMessage.notification?.body
            ?: data["bodyBn"]
            ?: data["bodyEn"]
            ?: data["descriptionBn"]
            ?: ""
        val categoryId = data["categoryId"] ?: data["category"] ?: "general"
        val priority = data["priority"] ?: "NORMAL"
        val isUrgent = priority.equals("HIGH", ignoreCase = true) ||
            categoryId.equals("urgent", ignoreCase = true) ||
            data["isUrgent"] == "true"
        val destinationPage = data["destinationPage"] ?: data["targetScreen"] ?: "notifications"
        val externalLink = data["externalLink"] ?: data["externalUrl"] ?: ""

        showNotification(title, body, isUrgent, destinationPage, externalLink)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        subscribeDefaultTopics()
    }

    private fun showNotification(
        title: String,
        body: String,
        isUrgent: Boolean,
        destinationPage: String,
        externalLink: String
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isUrgent) CHANNEL_URGENT else CHANNEL_GENERAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isUrgent) "জরুরী বিজ্ঞপ্তি (Urgent Notices)" else "সাধারণ বিজ্ঞপ্তি (Portal Notices)",
                if (isUrgent) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destinationPage", destinationPage)
            putExtra("externalLink", externalLink)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
