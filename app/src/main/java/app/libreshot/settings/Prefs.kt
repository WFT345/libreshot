package app.libreshot.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "libreshot")

data class PrefsSnapshot(
    val showThumbnail: Boolean = true,
    val thumbDurationMs: Long = 6000L,
    val cornerLeft: Boolean = true,
    val haptic: Boolean = true,
    val volumeChord: Boolean = false,
    val backTap: Boolean = false,
    val onboarded: Boolean = false,
)

object Prefs {

    private val SHOW_THUMBNAIL = booleanPreferencesKey("show_thumbnail")
    private val THUMB_DURATION_MS = longPreferencesKey("thumb_duration_ms")
    private val CORNER_LEFT = booleanPreferencesKey("corner_left")
    private val HAPTIC = booleanPreferencesKey("haptic")
    private val VOLUME_CHORD = booleanPreferencesKey("volume_chord")
    private val BACK_TAP = booleanPreferencesKey("back_tap")
    private val ONBOARDED = booleanPreferencesKey("onboarded")

    fun flow(context: Context): Flow<PrefsSnapshot> = context.dataStore.data.map { p ->
        PrefsSnapshot(
            showThumbnail = p[SHOW_THUMBNAIL] ?: true,
            thumbDurationMs = p[THUMB_DURATION_MS] ?: 6000L,
            cornerLeft = p[CORNER_LEFT] ?: true,
            haptic = p[HAPTIC] ?: true,
            volumeChord = p[VOLUME_CHORD] ?: false,
            backTap = p[BACK_TAP] ?: false,
            onboarded = p[ONBOARDED] ?: false,
        )
    }

    suspend fun load(context: Context): PrefsSnapshot = flow(context).first()

    suspend fun setShowThumbnail(context: Context, value: Boolean) =
        context.dataStore.edit { it[SHOW_THUMBNAIL] = value }

    suspend fun setThumbDuration(context: Context, value: Long) =
        context.dataStore.edit { it[THUMB_DURATION_MS] = value }

    suspend fun setCornerLeft(context: Context, value: Boolean) =
        context.dataStore.edit { it[CORNER_LEFT] = value }

    suspend fun setHaptic(context: Context, value: Boolean) =
        context.dataStore.edit { it[HAPTIC] = value }

    suspend fun setVolumeChord(context: Context, value: Boolean) =
        context.dataStore.edit { it[VOLUME_CHORD] = value }

    suspend fun setBackTap(context: Context, value: Boolean) =
        context.dataStore.edit { it[BACK_TAP] = value }

    suspend fun setOnboarded(context: Context, value: Boolean) =
        context.dataStore.edit { it[ONBOARDED] = value }
}
