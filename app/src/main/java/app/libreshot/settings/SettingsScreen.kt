package app.libreshot.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.libreshot.BuildConfig
import app.libreshot.R
import app.libreshot.ui.icons.AppIcons
import app.libreshot.ui.theme.Ios
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by Prefs.flow(context).collectAsStateWithLifecycle(initialValue = PrefsSnapshot())

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings),
                color = Ios.PrimaryText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(AppIcons.Close, contentDescription = null, tint = Ios.SecondaryText)
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(stringResource(R.string.settings_capture))
        Group {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { openGestureSettings(context) }
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.settings_quick_tap), color = Ios.Accent, fontSize = 16.sp)
                Text(
                    stringResource(R.string.settings_quick_tap_summary),
                    color = Ios.SecondaryText,
                    fontSize = 13.sp,
                )
            }
            ToggleRow(
                title = stringResource(R.string.settings_volume_chord),
                summary = stringResource(R.string.settings_volume_chord_summary),
                checked = prefs.volumeChord,
            ) { scope.launch { Prefs.setVolumeChord(context, it) } }
            ToggleRow(
                title = stringResource(R.string.settings_haptic),
                summary = null,
                checked = prefs.haptic,
            ) { scope.launch { Prefs.setHaptic(context, it) } }
        }

        SectionLabel(stringResource(R.string.settings_thumbnail))
        Group {
            ToggleRow(
                title = stringResource(R.string.settings_show_thumbnail),
                summary = stringResource(R.string.settings_show_thumbnail_summary),
                checked = prefs.showThumbnail,
            ) { scope.launch { Prefs.setShowThumbnail(context, it) } }
            ChoiceRow(
                title = stringResource(R.string.settings_thumb_duration),
                options = listOf("3 s" to 3000L, "6 s" to 6000L, "10 s" to 10000L),
                selected = prefs.thumbDurationMs,
            ) { scope.launch { Prefs.setThumbDuration(context, it) } }
            ChoiceRow(
                title = stringResource(R.string.settings_corner),
                options = listOf(
                    stringResource(R.string.settings_corner_left) to true,
                    stringResource(R.string.settings_corner_right) to false,
                ),
                selected = prefs.cornerLeft,
            ) { scope.launch { Prefs.setCornerLeft(context, it) } }
        }

        SectionLabel(stringResource(R.string.settings_about))
        Group {
            TextRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
            Text(
                stringResource(R.string.settings_privacy),
                color = Ios.SecondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun openGestureSettings(context: android.content.Context) {
    val intent = Intent("android.settings.QUICK_TAP_GESTURE_SETTINGS")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title.uppercase(),
        color = Ios.SecondaryText,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Ios.Surface),
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ios.PrimaryText, fontSize = 16.sp)
            if (summary != null) {
                Text(summary, color = Ios.SecondaryText, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Ios.Accent,
                uncheckedTrackColor = Ios.Separator,
            ),
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = Ios.PrimaryText, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, value) ->
                val active = value == selected
                Text(
                    label,
                    color = if (active) Ios.Background else Ios.PrimaryText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Ios.Accent else Ios.Separator)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun TextRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Ios.PrimaryText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ios.SecondaryText, fontSize = 16.sp)
    }
}
