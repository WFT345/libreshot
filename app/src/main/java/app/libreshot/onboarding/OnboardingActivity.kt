package app.libreshot.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
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
                OnboardingFlow(onFinished = { openSettings ->
                    if (openSettings) startActivity(Intent(this, SettingsActivity::class.java))
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

private const val PAGE_SERVICE = 1
private const val PAGE_READY = 3
private const val PAGE_COUNT = 4

@Composable
private fun OnboardingFlow(onFinished: (openSettings: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { PAGE_COUNT }
    var serviceEnabled by remember { mutableStateOf(isCaptureServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        // A return visit (the OS disabled the service later) lands on the fix, not the pitch.
        if (Prefs.load(context).onboarded && !serviceEnabled) pager.scrollToPage(PAGE_SERVICE)
        while (true) {
            val now = isCaptureServiceEnabled(context)
            if (now && !serviceEnabled && pager.currentPage == PAGE_SERVICE) {
                pager.animateScrollToPage(PAGE_SERVICE + 1)
            }
            serviceEnabled = now
            delay(500)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < PAGE_READY) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(PAGE_READY) } }) {
                    Text(stringResource(R.string.onboarding_skip), color = Ios.SecondaryText)
                }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> IntroPage()
                PAGE_SERVICE -> ServicePage(serviceEnabled)
                2 -> TriggerPage()
                PAGE_READY -> ReadyPage()
            }
        }
        PageDots(pager)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = {
                    when {
                        pager.currentPage < PAGE_READY -> scope.launch {
                            pager.animateScrollToPage(pager.currentPage + 1)
                        }

                        serviceEnabled -> {
                            scope.launch { Prefs.setOnboarded(context, true) }
                            onFinished(false)
                        }

                        else -> scope.launch { pager.animateScrollToPage(PAGE_SERVICE) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ios.Accent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        when {
                            pager.currentPage < PAGE_READY -> R.string.onboarding_next
                            serviceEnabled -> R.string.onboarding_start
                            else -> R.string.onboarding_enable_first
                        },
                    ),
                )
            }
            TextButton(
                onClick = {
                    scope.launch { Prefs.setOnboarded(context, true) }
                    onFinished(true)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_open_settings), color = Ios.SecondaryText)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PageDots(pager: PagerState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(PAGE_COUNT) { page ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (page == pager.currentPage) Ios.Accent else Ios.Separator),
            )
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Illustration(res: Int) {
    Image(
        painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp)),
    )
}

@Composable
private fun PageTitle(text: String) {
    Text(text, color = Ios.PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun PageBody(text: String) {
    Text(text, color = Ios.SecondaryText, fontSize = 15.sp, lineHeight = 21.sp)
}

@Composable
private fun IntroPage() {
    Page {
        Illustration(R.drawable.onboarding_peek)
        Spacer(Modifier.height(20.dp))
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
        Spacer(Modifier.height(12.dp))
        PageBody(stringResource(R.string.onboarding_intro_body))
    }
}

@Composable
private fun ServicePage(serviceEnabled: Boolean) {
    val context = LocalContext.current
    Page {
        PageTitle(stringResource(R.string.onboarding_a11y_title))
        Spacer(Modifier.height(12.dp))
        PageBody(stringResource(R.string.onboarding_a11y_body))
        Spacer(Modifier.height(20.dp))
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
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(
                if (serviceEnabled) R.string.onboarding_service_on
                else R.string.onboarding_service_off,
            ),
            color = if (serviceEnabled) Ios.Accent else Ios.SecondaryText,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TriggerPage() {
    Page {
        Illustration(R.drawable.onboarding_tap)
        Spacer(Modifier.height(20.dp))
        PageTitle(stringResource(R.string.onboarding_trigger_title))
        Spacer(Modifier.height(12.dp))
        val context = LocalContext.current
        val isPixel = Build.MANUFACTURER.equals("google", ignoreCase = true)
        // Quick Tap exists only on stock Pixel software; AOSP-based builds on Pixel
        // hardware (e.g. GrapheneOS) lack it, so gate on the intent actually resolving.
        val hasQuickTap = remember {
            isPixel && Intent("android.settings.QUICK_TAP_GESTURE_SETTINGS")
                .resolveActivity(context.packageManager) != null
        }
        PageBody(
            stringResource(
                when {
                    hasQuickTap -> R.string.onboarding_trigger_pixel
                    isPixel -> R.string.onboarding_trigger_aosp
                    else -> R.string.onboarding_trigger_generic
                },
            ),
        )
    }
}

@Composable
private fun ReadyPage() {
    Page {
        Illustration(R.drawable.onboarding_markup)
        Spacer(Modifier.height(20.dp))
        PageTitle(stringResource(R.string.onboarding_ready_title))
        Spacer(Modifier.height(12.dp))
        PageBody(stringResource(R.string.onboarding_ready_body))
    }
}
