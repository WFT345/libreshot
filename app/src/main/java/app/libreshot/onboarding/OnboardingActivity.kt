package app.libreshot.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.libreshot.R
import app.libreshot.settings.Prefs
import app.libreshot.settings.SettingsActivity
import app.libreshot.ui.theme.Ios
import app.libreshot.ui.theme.LibreShotTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            LibreShotTheme {
                OnboardingScreen(onContinue = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                })
            }
        }
    }
}

fun isCaptureServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.contains(context.packageName)
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serviceEnabled by remember { mutableStateOf(isCaptureServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            serviceEnabled = isCaptureServiceEnabled(context)
            delay(500)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.onboarding_title),
            color = Ios.PrimaryText,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.onboarding_tagline),
            color = Ios.SecondaryText,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))

        Card {
            Text(
                stringResource(R.string.onboarding_a11y_title),
                color = Ios.PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_a11y_body),
                color = Ios.SecondaryText,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ios.Accent),
            ) {
                Text(stringResource(R.string.onboarding_enable_service))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    if (serviceEnabled) R.string.onboarding_service_on
                    else R.string.onboarding_service_off,
                ),
                color = if (serviceEnabled) Ios.Accent else Ios.SecondaryText,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(16.dp))

        Card {
            Text(
                stringResource(R.string.onboarding_trigger_title),
                color = Ios.PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            val isPixel = Build.MANUFACTURER.equals("google", ignoreCase = true)
            Text(
                stringResource(
                    if (isPixel) R.string.onboarding_trigger_pixel
                    else R.string.onboarding_trigger_generic,
                ),
                color = Ios.SecondaryText,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch { Prefs.setOnboarded(context, true) }
                onContinue()
            },
            enabled = serviceEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Ios.Accent),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings), color = Ios.SecondaryText)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Ios.Surface)
            .padding(16.dp),
    ) {
        content()
    }
}
