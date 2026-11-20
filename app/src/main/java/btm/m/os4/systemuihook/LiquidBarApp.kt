// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright 2026 LiquidBar Contributors
package btm.m.os4.systemuihook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import btm.m.liquidglass.*
import io.github.libxposed.service.XposedService
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

data class LiquidTargetApp(
    val name: String,
    val packageName: String,
    val isPrimary: Boolean = false,
)

private val TARGET_APPS = listOf(
    LiquidTargetApp("WhatsApp", "com.whatsapp", isPrimary = true),
    LiquidTargetApp("Google Play Store", "com.android.vending", isPrimary = true),
    LiquidTargetApp("Gmail", "com.google.android.gm", isPrimary = true),
    LiquidTargetApp("Instagram", "com.instagram.android"),
    LiquidTargetApp("X / Twitter", "com.twitter.android"),
    LiquidTargetApp("Reddit", "com.reddit.frontpage"),
    LiquidTargetApp("Pinterest", "com.pinterest"),
    LiquidTargetApp("GitHub", "com.github.android"),
)

private val STYLE_OPTIONS = listOf(
    "HyperOS Capsule Flottante" to NavigationStyle.HYPER_OS_FLOATING,
    "iOS Liquid Glass (Verre)" to NavigationStyle.LIQUID_GLASS,
    "HyperOS Docké" to NavigationStyle.HYPER_OS,
)

private val LABEL_OPTIONS = listOf(
    "Icônes et texte" to LabelMode.ICON_AND_TEXT,
    "Icônes uniquement" to LabelMode.ICON_ONLY,
    "Texte uniquement" to LabelMode.TEXT_ONLY,
)

private val COLOR_OPTIONS = listOf(
    "Système (Automatique)" to AppColorMode.SYSTEM,
    "Mode Clair" to AppColorMode.LIGHT,
    "Mode Sombre" to AppColorMode.DARK,
)

@Composable
fun LiquidBarApp() {
    val themeController = remember { ThemeController() }
    val context = LocalContext.current
    val activity = context as? Activity
    val service by HookApplication.service.collectAsState()
    val store = remember(context) { AppNavigationSettingsStore(context) }

    var prefRevision by remember { mutableIntStateOf(0) }

    MiuixTheme(controller = themeController) {
        val surfaceColor = MiuixTheme.colorScheme.surface
        val isDark = surfaceColor.luminance() < 0.5f

        SideEffect {
            activity?.let { act ->
                act.window.statusBarColor = android.graphics.Color.TRANSPARENT
                act.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = "LiquidBar",
                    largeTitle = "LiquidBar",
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Card
                item {
                    LsposedStatusCard(service = service)
                }

                // Global Settings
                item {
                    SmallTitle(text = "GÉNÉRAL")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        val isMasterEnabled = remember(prefRevision) {
                            store.preferences.getBoolean(ScopedSettings.KEY_MODULE_ENABLED, true)
                        }
                        SwitchPreference(
                            checked = isMasterEnabled,
                            onCheckedChange = { enabled ->
                                store.edit(service) {
                                    putBoolean(ScopedSettings.KEY_MODULE_ENABLED, enabled)
                                }
                                prefRevision++
                            },
                            title = "Activer LiquidBar",
                            summary = "Active l'injection dynamique des barres flottantes et liquid glass",
                        )
                    }
                }

                // Primary Target Applications
                item {
                    SmallTitle(text = "APPLICATIONS PRINCIPALES")
                }

                items(TARGET_APPS.filter { it.isPrimary }, key = { it.packageName }) { app ->
                    TargetAppCard(
                        app = app,
                        store = store,
                        service = service,
                        revision = prefRevision,
                        onChanged = { prefRevision++ }
                    )
                }

                // Other Supported Applications
                item {
                    SmallTitle(text = "AUTRES APPLICATIONS COMPATIBLES")
                }

                items(TARGET_APPS.filter { !it.isPrimary }, key = { it.packageName }) { app ->
                    TargetAppCard(
                        app = app,
                        store = store,
                        service = service,
                        revision = prefRevision,
                        onChanged = { prefRevision++ }
                    )
                }

                // Style & Material Tuning
                item {
                    SmallTitle(text = "GÉOMÉTRIE & MATÉRIAU")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        val hideGestureBar = remember(prefRevision) {
                            store.preferences.getBoolean(KEY_HIDE_GLOBAL_GESTURE_HANDLE, false)
                        }
                        SwitchPreference(
                            checked = hideGestureBar,
                            onCheckedChange = { active ->
                                store.edit(service) {
                                    putBoolean(KEY_HIDE_GLOBAL_GESTURE_HANDLE, active)
                                }
                                prefRevision++
                            },
                            title = "Masquer l'indicateur de gestes HyperOS",
                            summary = "Rend la barre blanche de navigation système totalement invisible et transparente",
                        )

                        val globalOffset = remember(prefRevision) {
                            NavigationOffsetSettings.load(store.preferences)
                        }
                        val offsetIndices = listOf(0, 8, 16, 24)
                        val offsetLabels = listOf(
                            "Standard (0 dp)",
                            "Légèrement relevé (+8 dp)",
                            "Relevé (+16 dp)",
                            "Flottant haut (+24 dp)"
                        )
                        val currentOffsetIndex = offsetIndices.indexOf(globalOffset.valueDp).coerceAtLeast(0)

                        OverlayDropdownPreference(
                            title = "Décalage vertical du bas",
                            summary = "Ajuste l'élévation de la capsule au-dessus du bas de l'écran",
                            items = offsetLabels,
                            selectedIndex = currentOffsetIndex,
                            onSelectedIndexChange = { idx ->
                                val newDp = offsetIndices[idx]
                                NavigationOffsetSettings.save(
                                    store.preferences,
                                    service,
                                    null,
                                    NavigationOffsetSettings.Settings(enabled = newDp > 0, valueDp = newDp)
                                )
                                prefRevision++
                            }
                        )
                    }
                }

                // About & Credits
                item {
                    SmallTitle(text = "À PROPOS")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LiquidBar v1.0.0",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Barres de navigation dynamiques HyperOS 4 & iOS Liquid Glass pour Android.",
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Interface MIUIX native HyperOS 4.\nCrédits au projet original HyperChanger (ColdP, AGPL-3.0) & Antigravity.",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        }

                        ArrowPreference(
                            title = "Code source sur GitHub",
                            summary = "paulfremfrem-cmyk/LiquidBar",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/paulfremfrem-cmyk/LiquidBar")
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LsposedStatusCard(service: XposedService?) {
    val isConnected = service != null
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF34C759) else Color(0xFFFF9500))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) "Service LSPosed Connecté" else "En attente du service LSPosed",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConnected) {
                        "Le daemon Xposed est actif. Les modifications sont synchronisées en direct."
                    } else {
                        "Activez le module dans LSPosed et cochez vos applications cibles."
                    },
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

@Composable
private fun TargetAppCard(
    app: LiquidTargetApp,
    store: AppNavigationSettingsStore,
    service: XposedService?,
    revision: Int,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val (appInfo, isInstalled) = remember(app.packageName) {
        runCatching {
            pm.getApplicationInfo(app.packageName, 0) to true
        }.getOrElse { null to false }
    }

    val appIcon: Drawable? = remember(appInfo) {
        appInfo?.let { pm.getApplicationIcon(it) }
    }

    val isEnabled = remember(revision, app.packageName) {
        ScopedSettings.getBoolean(store.preferences, app.packageName, ScopedSettings.KEY_ENABLED, true)
    }

    val currentStylePref = remember(revision, app.packageName) {
        store.preferences.getString(
            ScopedSettings.key(app.packageName, NavigationStyle.PREFERENCE_KEY),
            NavigationStyle.DEFAULT_VALUE
        )
    }
    val currentStyle = NavigationStyle.fromPreference(currentStylePref)
    val selectedStyleIndex = STYLE_OPTIONS.indexOfFirst { it.second == currentStyle }.coerceAtLeast(0)

    val currentLabelPref = remember(revision, app.packageName) {
        store.preferences.getString(
            ScopedSettings.key(app.packageName, ScopedSettings.KEY_LABEL_MODE),
            LabelMode.DEFAULT_VALUE
        )
    }
    val currentLabel = LabelMode.fromPreference(currentLabelPref)
    val selectedLabelIndex = LABEL_OPTIONS.indexOfFirst { it.second == currentLabel }.coerceAtLeast(0)

    val currentColorPref = remember(revision, app.packageName) {
        store.preferences.getString(
            ScopedSettings.key(app.packageName, ScopedSettings.KEY_COLOR_MODE),
            AppColorMode.DEFAULT_VALUE
        )
    }
    val currentColor = AppColorMode.fromPreference(currentColorPref)
    val selectedColorIndex = COLOR_OPTIONS.indexOfFirst { it.second == currentColor }.coerceAtLeast(0)

    Card(modifier = Modifier.fillMaxWidth()) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(1),
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isInstalled) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MiuixTheme.colorScheme.surfaceContainerHigh
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isInstalled) "Installée" else "Non détectée",
                    fontSize = 11.sp,
                    color = if (isInstalled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        // Switch to enable for this app
        SwitchPreference(
            checked = isEnabled,
            onCheckedChange = { active ->
                store.edit(service) {
                    putBoolean(ScopedSettings.key(app.packageName, ScopedSettings.KEY_MODULE_ENABLED), true)
                    putBoolean(ScopedSettings.key(app.packageName, ScopedSettings.KEY_ENABLED), active)
                }
                onChanged()
            },
            title = "Activer la barre personnalisée",
            summary = "Remplace la barre native par le composant LiquidBar",
        )

        AnimatedVisibility(
            visible = isEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                OverlayDropdownPreference(
                    title = "Style visuel",
                    summary = "Choisissez le style de la capsule",
                    items = STYLE_OPTIONS.map { it.first },
                    selectedIndex = selectedStyleIndex,
                    onSelectedIndexChange = { idx ->
                        val selected = STYLE_OPTIONS[idx].second
                        store.edit(service) {
                            putString(
                                ScopedSettings.key(app.packageName, NavigationStyle.PREFERENCE_KEY),
                                selected.preferenceValue
                            )
                        }
                        onChanged()
                    }
                )

                OverlayDropdownPreference(
                    title = "Mode d'affichage des labels",
                    summary = "Contenu des onglets de navigation",
                    items = LABEL_OPTIONS.map { it.first },
                    selectedIndex = selectedLabelIndex,
                    onSelectedIndexChange = { idx ->
                        val selected = LABEL_OPTIONS[idx].second
                        store.edit(service) {
                            putString(
                                ScopedSettings.key(app.packageName, ScopedSettings.KEY_LABEL_MODE),
                                selected.preferenceValue
                            )
                        }
                        onChanged()
                    }
                )

                OverlayDropdownPreference(
                    title = "Thème de couleur",
                    summary = "Palette appliquée au fond et aux icônes",
                    items = COLOR_OPTIONS.map { it.first },
                    selectedIndex = selectedColorIndex,
                    onSelectedIndexChange = { idx ->
                        val selected = COLOR_OPTIONS[idx].second
                        store.edit(service) {
                            putString(
                                ScopedSettings.key(app.packageName, ScopedSettings.KEY_COLOR_MODE),
                                selected.preferenceValue
                            )
                        }
                        onChanged()
                    }
                )
            }
        }
    }
}
