package com.ashequllah.portal.data.repository

import android.content.Context
import com.ashequllah.portal.data.model.AppReleaseInfo
import com.ashequllah.portal.data.model.DigitalServiceItem
import com.ashequllah.portal.data.model.EducationSupportItem
import com.ashequllah.portal.data.model.EmploymentEconomyItem
import com.ashequllah.portal.data.model.NotificationItem
import com.ashequllah.portal.data.model.SocialWelfareItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object PortalFirestoreProvider {
    private const val DATABASE_ID = "ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943"

    val db: FirebaseFirestore by lazy {
        Firebase.firestore(DATABASE_ID).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(persistentCacheSettings { })
                .build()
        }
    }
}

class PortalRepository(
    private val db: FirebaseFirestore = PortalFirestoreProvider.db,
    private val context: Context? = null
) {
    private val prefs by lazy {
        context?.getSharedPreferences("portal_notif_reads", Context.MODE_PRIVATE)
    }

    /**
     * Uses single equality filter + client-side sort so Spark Plan projects never crash
     * with FAILED_PRECONDITION if composite Firestore indexes have not been manually created.
     */
    fun observePublishedNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        val registration = db.collection("notifications")
            .whereEqualTo("isPublished", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { doc -> NotificationItem.fromSnapshot(doc) }
                    ?.filter { it.isPublished }
                    ?.sortedByDescending {
                        it.publishedAt?.toDate()?.time
                            ?: it.createdAt?.toDate()?.time
                            ?: 0L
                    }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeReadNotificationIds(userId: String): Flow<Set<String>> = callbackFlow {
        val localReads = prefs?.getStringSet("read_ids", emptySet())?.toSet().orEmpty()
        if (userId.isBlank()) {
            trySend(localReads)
            awaitClose { }
            return@callbackFlow
        }
        val registration = db.collection("notification_read_status")
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localReads)
                    return@addSnapshotListener
                }
                val remoteIds = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                trySend(localReads + remoteIds)
            }
        awaitClose { registration.remove() }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        if (notificationId.isBlank()) return
        prefs?.let {
            val updated = it.getStringSet("read_ids", emptySet()).orEmpty().toMutableSet()
            updated.add(notificationId)
            it.edit().putStringSet("read_ids", updated).apply()
        }
        if (userId.isBlank()) return
        runCatching {
            db.collection("notification_read_status")
                .document(userId)
                .collection("items")
                .document(notificationId)
                .set(mapOf("read" to true, "readAt" to Timestamp.now()))
                .await()
        }
    }

    suspend fun markAllNotificationsRead(userId: String, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        prefs?.let {
            val updated = it.getStringSet("read_ids", emptySet()).orEmpty().toMutableSet()
            updated.addAll(notificationIds)
            it.edit().putStringSet("read_ids", updated).apply()
        }
        if (userId.isBlank()) return
        runCatching {
            val batch = db.batch()
            val now = Timestamp.now()
            val parent = db.collection("notification_read_status").document(userId).collection("items")
            notificationIds.forEach { id ->
                batch.set(parent.document(id), mapOf("read" to true, "readAt" to now))
            }
            batch.commit().await()
        }
    }

    fun observeLatestAppRelease(): Flow<AppReleaseInfo?> = callbackFlow {
        val registration = db.collection("app_release")
            .document("latest")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val release = snapshot?.let { AppReleaseInfo.fromSnapshot(it) }
                if (
                    release != null &&
                    release.is_published &&
                    release.package_name == "com.ashequllah.portal" &&
                    release.download_url.startsWith("https://") &&
                    !release.download_url.contains("firebasestorage.googleapis.com") &&
                    !release.download_url.contains("firebasestorage.app")
                ) {
                    trySend(release)
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeDigitalServices(): Flow<List<DigitalServiceItem>> =
        observeOrderedPublished("digital_services") { it.order }

    fun observeEducationSupport(): Flow<List<EducationSupportItem>> =
        observeOrderedPublished("education_scholarships") { it.order }

    fun observeEmploymentEconomy(): Flow<List<EmploymentEconomyItem>> =
        observeOrderedPublished("employment") { it.order }

    fun observeSocialWelfare(): Flow<List<SocialWelfareItem>> =
        observeOrderedPublished("social_welfare") { it.order }

    private inline fun <reified T> observeOrderedPublished(
        collectionName: String,
        crossinline orderSelector: (T) -> Int
    ): Flow<List<T>> = callbackFlow {
        val registration = db.collection(collectionName)
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(T::class.java) }
                    ?.sortedBy { orderSelector(it) }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
}
