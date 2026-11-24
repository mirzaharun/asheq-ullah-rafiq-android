package com.ashequllah.portal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.PropertyName

enum class NotificationCategory(
    val key: String,
    val labelBn: String,
    val labelEn: String
) {
    URGENT("urgent", "জরুরী বিজ্ঞপ্তি", "Urgent Notice"),
    GENERAL("general", "সাধারণ বিজ্ঞপ্তি", "General Notice"),
    APP_UPDATE("app_update", "অ্যাপ আপডেট", "App Update"),
    DEVELOPMENT("development", "উন্নয়ন সংক্রান্ত আপডেট", "Development Update"),
    PUBLIC_SERVICE("public_service", "জনসেবা সংক্রান্ত আপডেট", "Public Service Update"),
    EVENTS("events", "অনুষ্ঠান ও কর্মসূচি", "Events & Programs"),
    OTHER("other", "অন্যান্য বিজ্ঞপ্তি", "Other Notice");

    companion object {
        fun fromRaw(raw: String?): NotificationCategory {
            val normalized = raw?.trim().orEmpty()
            return when (normalized.lowercase()) {
                "urgent", "urgent_notice" -> URGENT
                "general", "general_notice" -> GENERAL
                "app_update" -> APP_UPDATE
                "development", "development_update" -> DEVELOPMENT
                "public_service", "public_service_update" -> PUBLIC_SERVICE
                "events", "events_programs" -> EVENTS
                "other", "other_notice" -> OTHER
                else -> entries.firstOrNull {
                    it.key.equals(normalized, ignoreCase = true) ||
                        it.labelBn == normalized ||
                        it.labelEn.equals(normalized, ignoreCase = true)
                } ?: GENERAL
            }
        }
    }
}

/**
 * Matches Web Admin NotificationManager & crudService.ts saveNotification() payload 1:1,
 * while remaining resilient to legacy field names.
 */
data class NotificationItem(
    var id: String = "",
    var titleBn: String = "",
    var titleEn: String = "",
    var bodyBn: String = "",
    var bodyEn: String = "",
    var categoryId: String = NotificationCategory.GENERAL.key,
    var categoryBn: String = NotificationCategory.GENERAL.labelBn,
    var categoryEn: String = NotificationCategory.GENERAL.labelEn,
    var imageUrl: String = "",
    var destinationPage: String = "home",
    var externalLink: String = "",
    var targetAudience: String = "ALL",
    var priority: String = "NORMAL",
    var sendPush: Boolean = false,
    var pushStatus: String = "not_requested",
    @get:PropertyName("isPublished")
    @set:PropertyName("isPublished")
    var isPublished: Boolean = false,
    var scheduledAt: String = "",
    var publishedAt: Timestamp? = null,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
    var createdByUid: String = ""
) {
    val isUrgentPriority: Boolean
        get() = priority.equals("HIGH", ignoreCase = true) ||
            NotificationCategory.fromRaw(categoryId) == NotificationCategory.URGENT

    fun resolvedTitle(isBengali: Boolean): String =
        if (isBengali) titleBn.ifBlank { titleEn } else titleEn.ifBlank { titleBn }

    fun resolvedBody(isBengali: Boolean): String =
        if (isBengali) bodyBn.ifBlank { bodyEn } else bodyEn.ifBlank { bodyBn }

    fun resolvedCategory(): NotificationCategory =
        NotificationCategory.fromRaw(categoryId.ifBlank { categoryEn.ifBlank { categoryBn } })

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): NotificationItem? {
            if (!doc.exists()) return null
            val rawCategory = doc.getString("categoryId")
                ?: doc.getString("category")
                ?: doc.getString("categoryEn")
                ?: doc.getString("categoryBn")
            val resolvedCat = NotificationCategory.fromRaw(rawCategory)
            val published = doc.getBoolean("isPublished")
                ?: doc.getBoolean("is_published")
                ?: false
            val urgentFlag = doc.getBoolean("isUrgent") ?: false
            val priority = doc.getString("priority")
                ?: if (urgentFlag) "HIGH" else "NORMAL"

            return NotificationItem(
                id = doc.id,
                titleBn = doc.getString("titleBn") ?: doc.getString("title_bn") ?: "",
                titleEn = doc.getString("titleEn") ?: doc.getString("title_en") ?: "",
                bodyBn = doc.getString("bodyBn") ?: doc.getString("descriptionBn") ?: doc.getString("description_bn") ?: "",
                bodyEn = doc.getString("bodyEn") ?: doc.getString("descriptionEn") ?: doc.getString("description_en") ?: "",
                categoryId = resolvedCat.key,
                categoryBn = doc.getString("categoryBn") ?: resolvedCat.labelBn,
                categoryEn = doc.getString("categoryEn") ?: resolvedCat.labelEn,
                imageUrl = doc.getString("imageUrl") ?: doc.getString("image_url") ?: "",
                destinationPage = doc.getString("destinationPage") ?: doc.getString("targetScreen") ?: "home",
                externalLink = doc.getString("externalLink") ?: doc.getString("externalUrl") ?: "",
                targetAudience = doc.getString("targetAudience") ?: "ALL",
                priority = priority,
                sendPush = doc.getBoolean("sendPush") ?: false,
                pushStatus = doc.getString("pushStatus") ?: "not_requested",
                isPublished = published,
                scheduledAt = doc.getString("scheduledAt") ?: "",
                publishedAt = doc.getTimestamp("publishedAt"),
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                createdByUid = doc.getString("createdByUid") ?: ""
            )
        }
    }
}

/**
 * Matches Web Admin AppVersionManager & crudService.ts saveAppRelease() payload 1:1.
 * Note: minimum_supported_version is saved as a numeric versionCode integer by Admin Panel.
 */
data class AppReleaseInfo(
    var app_name: String = "Asheq Ullah Rafiq",
    var package_name: String = "com.ashequllah.portal",
    var latest_version: String = "",
    var version_code: Long = 0L,
    var release_title_bn: String = "",
    var release_title_en: String = "",
    var release_notes_bn: String = "",
    var release_notes_en: String = "",
    var download_url: String = "",
    var release_tag: String = "",
    var release_date: String = "",
    var minimum_supported_version: Long = 1L,
    var force_update: Boolean = false,
    var update_available: Boolean = false,
    @get:PropertyName("is_published")
    @set:PropertyName("is_published")
    var is_published: Boolean = false,
    var file_name: String = "AsheqUllahRafiq.apk",
    var file_size: String = "",
    var checksum_sha256: String = ""
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): AppReleaseInfo? {
            if (!doc.exists()) return null
            val minVerAny = doc.get("minimum_supported_version")
            val minVerLong = when (minVerAny) {
                is Number -> minVerAny.toLong()
                is String -> minVerAny.trim().toLongOrNull() ?: 1L
                else -> 1L
            }
            val verCodeAny = doc.get("version_code")
            val verCodeLong = when (verCodeAny) {
                is Number -> verCodeAny.toLong()
                is String -> verCodeAny.trim().toLongOrNull() ?: 0L
                else -> 0L
            }
            return AppReleaseInfo(
                app_name = doc.getString("app_name") ?: "Asheq Ullah Rafiq",
                package_name = doc.getString("package_name") ?: "com.ashequllah.portal",
                latest_version = doc.getString("latest_version") ?: "",
                version_code = verCodeLong,
                release_title_bn = doc.getString("release_title_bn") ?: "",
                release_title_en = doc.getString("release_title_en") ?: "",
                release_notes_bn = doc.getString("release_notes_bn") ?: "",
                release_notes_en = doc.getString("release_notes_en") ?: "",
                download_url = doc.getString("download_url") ?: "",
                release_tag = doc.getString("release_tag") ?: "",
                release_date = doc.getString("release_date") ?: "",
                minimum_supported_version = minVerLong,
                force_update = doc.getBoolean("force_update") ?: false,
                update_available = doc.getBoolean("update_available") ?: false,
                is_published = doc.getBoolean("is_published") ?: false,
                file_name = doc.getString("file_name") ?: "AsheqUllahRafiq.apk",
                file_size = doc.getString("file_size") ?: "",
                checksum_sha256 = doc.getString("checksum_sha256") ?: ""
            )
        }
    }
}

data class DigitalServiceItem(
    var id: String = "",
    var serviceNameBn: String = "",
    var serviceNameEn: String = "",
    var authorityBn: String = "",
    var authorityEn: String = "",
    var categoryBn: String = "",
    var categoryEn: String = "",
    var portalUrl: String = "",
    var helpline: String = "",
    var iconName: String = "public",
    var descriptionBn: String = "",
    var descriptionEn: String = "",
    var order: Int = 1,
    var published: Boolean = true
)

data class EducationSupportItem(
    var id: String = "",
    var titleBn: String = "",
    var titleEn: String = "",
    var resourceType: String = "",
    var categoryBn: String = "",
    var authorityBn: String = "",
    var authorityEn: String = "",
    var websiteUrl: String = "",
    var applyUrl: String = "",
    var eligibilityBn: String = "",
    var descriptionBn: String = "",
    var descriptionEn: String = "",
    var deadline: String = "",
    var contactInfo: String = "",
    var order: Int = 1,
    var published: Boolean = true
)

data class EmploymentEconomyItem(
    var id: String = "",
    var jobTitleBn: String = "",
    var jobTitleEn: String = "",
    var categoryBn: String = "",
    var categoryEn: String = "",
    var organizationBn: String = "",
    var organizationEn: String = "",
    var location: String = "",
    var vacancyCount: String = "",
    var deadline: String = "",
    var applyUrl: String = "",
    var circularFileUrl: String = "",
    var detailsBn: String = "",
    var detailsEn: String = "",
    var order: Int = 1,
    var published: Boolean = true
)

data class SocialWelfareItem(
    var id: String = "",
    var titleBn: String = "",
    var titleEn: String = "",
    var categoryBn: String = "",
    var categoryEn: String = "",
    var authorityBn: String = "",
    var authorityEn: String = "",
    var eligibilityBn: String = "",
    var benefitsBn: String = "",
    var applyUrl: String = "",
    var circularFileUrl: String = "",
    var helpline: String = "",
    var descriptionBn: String = "",
    var descriptionEn: String = "",
    var order: Int = 1,
    var published: Boolean = true
)
