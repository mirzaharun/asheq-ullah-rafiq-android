package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.data.model.*
import com.example.data.repository.DefaultPortalContent
import com.example.ui.components.*

@Composable
fun HomeScreen(
    weatherData: WeatherData,
    publicServices: List<PublicServiceItem>,
    digitalServices: List<DigitalServiceItem>,
    dailySchedules: List<DailyScheduleItem>,
    specialEvents: List<SpecialEventItem> = emptyList(),
    language: AppLanguage,
    onWeatherClick: () -> Unit,
    onSeaTruckClick: () -> Unit,
    onServiceClick: (String) -> Unit,
    onTourismClick: () -> Unit,
    onDecadeDetailClick: () -> Unit,
    onNoticeClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaAssets: DynamicMediaAssets = DynamicMediaAssets(),
    appConfig: AppConfig = AppConfig(),
    isAdminAuthenticated: Boolean = false,
    onEditClick: () -> Unit = {},
    onSaveConfig: (AppConfig) -> Unit = {},
    onSaveService: (DigitalServiceItem) -> Unit = {},
    onDeleteService: (String) -> Unit = {},
    onSaveSchedule: (DailyScheduleItem) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onSaveSpecialEvent: (SpecialEventItem) -> Unit = {},
    onDeleteSpecialEvent: (String) -> Unit = {},
    homepageCategories: List<HomepageCategoryItem> = DefaultPortalContent.defaultHomepageCategories,
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick
) {
    val scrollState = rememberScrollState()
    var isMuted by remember { mutableStateOf(true) }
    var showEditNoticeDialog by remember { mutableStateOf(false) }
    
    // CRUD state
    var editingSchedule by remember { mutableStateOf<DailyScheduleItem?>(null) }
    var isAddingSchedule by remember { mutableStateOf(false) }
    var editingSpecialEvent by remember { mutableStateOf<SpecialEventItem?>(null) }
    var isAddingSpecialEvent by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("home_screen_content")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeroBanner(
                language = language,
                customVideoSource = mediaAssets.bannerVideoSource,
                customPosterSource = mediaAssets.bannerPosterSource,
                isMuted = isMuted,
                onToggleMute = { isMuted = !isMuted }
            )
            if (isAdminAuthenticated) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit Banner", tint = Color.White)
                }
            }
        }

        MarqueeNoticeBar(
            appConfig = appConfig,
            language = language,
            isAdminAuthenticated = isAdminAuthenticated,
            onEditClick = { showEditNoticeDialog = true }
        )

        WeatherAndTransportCards(
            weatherData = weatherData,
            language = language,
            onWeatherClick = onWeatherClick,
            onSeaTruckClick = onSeaTruckClick
        )

        PublicServicesSection(
            services = publicServices,
            homepageCategories = homepageCategories,
            language = language,
            onServiceClick = onServiceClick,
            onTourismClick = onTourismClick
        )

        SpecialEventsSection(
            events = specialEvents,
            language = language,
            isAdmin = isAdminAuthenticated,
            onAddClick = { isAddingSpecialEvent = true },
            onEditClick = { editingSpecialEvent = it },
            onDeleteClick = onDeleteSpecialEvent
        )

        DailyScheduleSection(
            schedules = dailySchedules,
            language = language,
            isAdmin = isAdminAuthenticated,
            onAddClick = { isAddingSchedule = true },
            onEditClick = { editingSchedule = it },
            onDeleteClick = onDeleteSchedule
        )
    }

    // Dialogs
    if (showEditNoticeDialog) {
        AdminNoticeEditDialog(
            appConfig = appConfig,
            language = language,
            onSave = onSaveConfig,
            onDismiss = { showEditNoticeDialog = false }
        )
    }

    if (isAddingSchedule || editingSchedule != null) {
        DailyScheduleEditDialog(item = editingSchedule, isNew = isAddingSchedule, onSave = { onSaveSchedule(it); isAddingSchedule = false; editingSchedule = null }, onDismiss = { isAddingSchedule = false; editingSchedule = null }, language = language)
    }

    if (isAddingSpecialEvent || editingSpecialEvent != null) {
        val initial = editingSpecialEvent ?: SpecialEventItem(id = "SE_${System.currentTimeMillis()}")
        SpecialEventFormDialog(
            item = initial,
            isNew = isAddingSpecialEvent,
            language = language,
            onSave = {
                onSaveSpecialEvent(it)
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            },
            onDismiss = {
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            }
        )
    }
}
