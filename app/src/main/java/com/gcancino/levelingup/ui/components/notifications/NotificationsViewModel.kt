package com.gcancino.levelingup.ui.components.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.domain.models.notification.NotificationItem
import com.gcancino.levelingup.domain.models.notification.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "NotificationsViewModel"

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    val notificationCount: StateFlow<Int> = _notifications
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        checkForNotifications()
    }

    private fun checkForNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.tag(TAG).w("No authenticated user — skipping notification check")
                return@launch
            }

            Timber.tag(TAG).d("Checking notifications for user: $userId")

            val pending = mutableListOf<NotificationItem>()

            // ── Check 1: baseline body measurements ───────────────────────────────
            // If no document exists in player_measurements where uID == userId
            // AND initialData == true, the user hasn't set their baseline yet.
            try {
                val measurementsSnapshot = firestore
                    .collection("player_measurements")
                    .whereEqualTo("uID", userId)
                    .whereEqualTo("initialData", true)
                    .limit(1)
                    .get()
                    .await()

                if (measurementsSnapshot.isEmpty) {
                    Timber.tag(TAG).d("✘ No initial measurements found → adding notification")
                    pending.add(
                        NotificationItem(
                            id      = NotificationType.MISSING_INITIAL_MEASUREMENTS.name,
                            type    = NotificationType.MISSING_INITIAL_MEASUREMENTS,
                            title   = "Set your baseline",
                            message = "Add your initial body measurements so we can track your progress over time."
                        )
                    )
                } else {
                    Timber.tag(TAG).d("✔ Initial measurements found")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error checking player_measurements")
            }

            // ── Check 2: baseline body composition ────────────────────────────────
            // Same logic for player_bodyComposition
            try {
                val compositionSnapshot = firestore
                    .collection("player_bodyComposition")
                    .whereEqualTo("uID", userId)
                    .whereEqualTo("initialData", true)
                    .limit(1)
                    .get()
                    .await()

                if (compositionSnapshot.isEmpty) {
                    Timber.tag(TAG).d("✘ No initial body composition found → adding notification")
                    pending.add(
                        NotificationItem(
                            id      = NotificationType.MISSING_INITIAL_COMPOSITION.name,
                            type    = NotificationType.MISSING_INITIAL_COMPOSITION,
                            title   = "Set your body composition",
                            message = "Add your initial body composition data to enable progression tracking."
                        )
                    )
                } else {
                    Timber.tag(TAG).d("✔ Initial body composition found")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error checking player_bodyComposition")
            }

            Timber.tag(TAG).i("Notification check complete → ${pending.size} pending notification(s)")
            _notifications.value = pending
        }
    }

    /** Call this after the user saves their initial data to dismiss the notification. */
    fun dismissNotification(type: NotificationType) {
        Timber.tag(TAG).d("Dismissing notification: $type")
        _notifications.value = _notifications.value.filter { it.type != type }
    }

    /** Re-run all checks — call after user saves data to refresh the list. */
    fun refresh() {
        Timber.tag(TAG).d("Refreshing notifications...")
        _notifications.value = emptyList()
        checkForNotifications()
    }
}