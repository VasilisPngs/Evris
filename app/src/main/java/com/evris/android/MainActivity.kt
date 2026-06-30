package com.evris.android

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evris.android.apkmirror.ApkMirrorApiClient
import com.evris.android.apkmirror.ApkMirrorSource
import com.evris.android.settings.ReleaseChannelFilter
import com.evris.android.settings.SettingsStore
import com.evris.android.play.PlayDownloader
import com.evris.android.play.PlayInstaller
import com.evris.android.play.PlayRepository
import com.evris.android.play.PlayUpdate
import com.evris.android.ui.SettingsScreen
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystem: Boolean,
    val icon: Bitmap?
)

data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val url: String
)

enum class AppListFilter {
    ALL,
    USER,
    SYSTEM,
    UPDATES
}

private enum class RootScreen {
    HOME,
    SEARCH,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var darkMode by remember { mutableStateOf(SettingsStore.readDarkMode(context)) }

            EvrisTheme(darkMode = darkMode) {
                EvrisRoot(
                    darkMode = darkMode,
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        SettingsStore.writeDarkMode(context, enabled)
                    }
                )
            }
        }
    }
}

@Composable
fun EvrisTheme(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    val darkWhite = Color(0xFFF5F5F5)
    val lightWhite = Color(0xFFFFFFFF)

    val colors = if (darkMode) {
        darkColorScheme(
            background = Color(0xFF151515),
            surface = Color(0xFF2B2B2B),
            surfaceVariant = Color(0xFF3A3A3A),
            primary = darkWhite,
            onPrimary = Color(0xFF111111),
            onBackground = darkWhite,
            onSurface = darkWhite,
            onSurfaceVariant = Color(0xFFD8D8D8),
            outline = Color(0xFF8A8A8A),
            outlineVariant = Color(0xFF686868)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF4F4F4),
            surface = lightWhite,
            surfaceVariant = Color(0xFFECECEC),
            primary = lightWhite,
            onPrimary = Color(0xFF171717),
            onBackground = Color(0xFF171717),
            onSurface = Color(0xFF171717),
            onSurfaceVariant = Color(0xFF4E4E4E),
            outline = Color(0xFF8A8A8A),
            outlineVariant = Color(0xFFD2D2D2)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

@Composable
fun EvrisRoot(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var currentScreen by remember { mutableStateOf(RootScreen.HOME) }
    var releaseSettings by remember { mutableStateOf(SettingsStore.read(context)) }
    var selectedFilter by remember { mutableStateOf(AppListFilter.UPDATES) }
    var searchQuery by remember { mutableStateOf("") }
    var scanRequest by remember { mutableIntStateOf(0) }
    var updateRequest by remember { mutableIntStateOf(0) }
    var scrollTopRequest by remember { mutableIntStateOf(0) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var rawUpdates by remember { mutableStateOf<Map<String, UpdateInfo>>(emptyMap()) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var loadingApps by remember { mutableStateOf(true) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var playEnabled by remember { mutableStateOf(SettingsStore.readPlayEnabled(context)) }
    var playUpdates by remember { mutableStateOf<Map<String, PlayUpdate>>(emptyMap()) }
    var installingPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentScreen) {
        if (currentScreen != RootScreen.SEARCH) {
            searchQuery = ""
        }
    }

    LaunchedEffect(scanRequest) {
        loadingApps = true
        checkingUpdates = false
        updateError = null
        rawUpdates = emptyMap()
        playUpdates = emptyMap()

        apps = withContext(Dispatchers.IO) {
            scanInstalledApps(packageManager = context.packageManager)
        }

        loadingApps = false
        updateRequest++
    }

    LaunchedEffect(updateRequest) {
        if (updateRequest <= 0 || apps.isEmpty()) return@LaunchedEffect

        checkingUpdates = true
        updateError = null

        val result = ApkMirrorApiClient.checkUpdates(
            apps = apps,
            packageManager = context.packageManager
        )

        rawUpdates = result.updates
        updateError = result.error
        checkingUpdates = false
    }

    LaunchedEffect(updateRequest, playEnabled, apps) {
        playUpdates = if (playEnabled && apps.isNotEmpty()) {
            PlayRepository.checkUpdates(context, apps)
        } else {
            emptyMap()
        }
    }

    val updates = remember(rawUpdates, releaseSettings) {
        rawUpdates.filterValues { update ->
            ReleaseChannelFilter.isAllowed(
                versionName = update.versionName,
                settings = releaseSettings
            )
        }
    }

    val homeVisibleApps = remember(apps, updates, playUpdates, selectedFilter) {
        filterApps(
            apps = apps,
            updates = updates,
            filter = selectedFilter,
            query = "",
            extraUpdatePackages = playUpdates.keys
        )
    }

    val searchVisibleApps = remember(apps, updates, playUpdates, searchQuery) {
        filterApps(
            apps = apps,
            updates = updates,
            filter = AppListFilter.ALL,
            query = searchQuery,
            extraUpdatePackages = playUpdates.keys
        )
    }

    val visibleApps = if (currentScreen == RootScreen.SEARCH) {
        searchVisibleApps
    } else {
        homeVisibleApps
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                EvrisTopBar(
                    screen = currentScreen,
                    darkMode = darkMode,
                    loading = loadingApps || checkingUpdates,
                    onToggleTheme = { onDarkModeChange(!darkMode) },
                    onRefresh = { scanRequest++ }
                )
            }
        ) { innerPadding ->
            when (currentScreen) {
                RootScreen.HOME,
                RootScreen.SEARCH -> {
                    EvrisScreen(
                        modifier = Modifier.padding(innerPadding),
                        listState = listState,
                        scrollTopRequest = scrollTopRequest,
                        apps = apps,
                        updates = updates,
                        playUpdates = playUpdates,
                        installingPackages = installingPackages,
                        visibleApps = visibleApps,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        loadingApps = loadingApps,
                        checkingUpdates = checkingUpdates,
                        updateError = updateError,
                        searchActive = currentScreen == RootScreen.SEARCH,
                        onFilterChange = { selectedFilter = it },
                        onSearchQueryChange = { searchQuery = it },
                        onInstallPlay = { update ->
                            installPlayUpdate(
                                context = context,
                                scope = scope,
                                update = update,
                                onState = { installing ->
                                    installingPackages = if (installing) {
                                        installingPackages + update.packageName
                                    } else {
                                        installingPackages - update.packageName
                                    }
                                }
                            )
                        }
                    )
                }

                RootScreen.SETTINGS -> {
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 120.dp)
                    ) {
                        SettingsScreen(
                            settings = releaseSettings,
                            playEnabled = playEnabled,
                            onPlayChanged = { value ->
                                playEnabled = value
                                SettingsStore.writePlayEnabled(context, value)
                            },
                            onDevChanged = { value ->
                                val next = releaseSettings.copy(includeDev = value)
                                releaseSettings = next
                                SettingsStore.write(context, next)
                            },
                            onAlphaChanged = { value ->
                                val next = releaseSettings.copy(includeAlpha = value)
                                releaseSettings = next
                                SettingsStore.write(context, next)
                            },
                            onBetaChanged = { value ->
                                val next = releaseSettings.copy(includeBeta = value)
                                releaseSettings = next
                                SettingsStore.write(context, next)
                            },
                            onRcChanged = { value ->
                                val next = releaseSettings.copy(includeRc = value)
                                releaseSettings = next
                                SettingsStore.write(context, next)
                            },
                            onPrereleaseChanged = { value ->
                                val next = releaseSettings.copy(includePrerelease = value)
                                releaseSettings = next
                                SettingsStore.write(context, next)
                            }
                        )
                    }
                }
            }
        }

        EvrisBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentScreen = currentScreen,
            onHomeClick = {
                currentScreen = RootScreen.HOME
                scrollTopRequest++
            },
            onSearchClick = {
                currentScreen = RootScreen.SEARCH
                scrollTopRequest++
            },
            onSettingsClick = {
                currentScreen = RootScreen.SETTINGS
            }
        )
    }
}

@Composable
private fun EvrisTopBar(
    screen: RootScreen,
    darkMode: Boolean,
    loading: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit
) {
    val title = when (screen) {
        RootScreen.HOME -> "Evris"
        RootScreen.SETTINGS -> "Settings"
        RootScreen.SEARCH -> ""
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 42.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            IconButton(onClick = onToggleTheme) {
                Icon(
                    painter = painterResource(if (darkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                    contentDescription = if (darkMode) "Switch to light mode" else "Switch to dark mode"
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !loading
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Refresh"
                )
            }
        }
    }
}
@Composable
private fun EvrisBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: RootScreen,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(R.drawable.ic_home, "Home", currentScreen == RootScreen.HOME, onHomeClick)
            BottomBarItem(R.drawable.ic_search, "Search", currentScreen == RootScreen.SEARCH, onSearchClick)
            BottomBarItem(R.drawable.ic_settings, "Settings", currentScreen == RootScreen.SETTINGS, onSettingsClick)
        }
    }
}
@Composable
private fun BottomBarItem(
    icon: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "navBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "navTint"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun EvrisScreen(
    modifier: Modifier,
    listState: LazyListState,
    scrollTopRequest: Int,
    apps: List<InstalledApp>,
    updates: Map<String, UpdateInfo>,
    playUpdates: Map<String, PlayUpdate>,
    installingPackages: Set<String>,
    visibleApps: List<InstalledApp>,
    selectedFilter: AppListFilter,
    searchQuery: String,
    loadingApps: Boolean,
    checkingUpdates: Boolean,
    updateError: String?,
    searchActive: Boolean,
    onFilterChange: (AppListFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onInstallPlay: (PlayUpdate) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(scrollTopRequest) {
        if (scrollTopRequest > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = 8.dp,
                end = 14.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (searchActive) {
                item {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange
                    )
                }
            } else {
                item {
                    ControlsCard(
                        selectedFilter = selectedFilter,
                        updatesCount = (updates.keys + playUpdates.keys).size,
                        loadingApps = loadingApps,
                        checkingUpdates = checkingUpdates,
                        updateError = updateError,
                        onFilterChange = onFilterChange
                    )
                }
            }

            items(
                items = visibleApps,
                key = { it.packageName }
            ) { app ->
                val playUpdate = playUpdates[app.packageName]
                InstalledAppCard(
                    app = app,
                    update = updates[app.packageName],
                    playUpdate = playUpdate,
                    installing = app.packageName in installingPackages,
                    onOpenAPKMirror = {
                        openAPKMirror(
                            context = context,
                            packageName = app.packageName,
                            update = updates[app.packageName]
                        )
                    },
                    onInstallPlay = {
                        if (playUpdate != null) onInstallPlay(playUpdate)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        label = { Text("Search apps") },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null
            )
        }
    )
}

@Composable
fun ControlsCard(
    selectedFilter: AppListFilter,
    updatesCount: Int,
    loadingApps: Boolean,
    checkingUpdates: Boolean,
    updateError: String?,
    onFilterChange: (AppListFilter) -> Unit
) {
    UniformCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTab(
                    label = "Updates",
                    selected = selectedFilter == AppListFilter.UPDATES,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterChange(AppListFilter.UPDATES) }
                )

                FilterTab(
                    label = "User",
                    selected = selectedFilter == AppListFilter.USER,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterChange(AppListFilter.USER) }
                )

                FilterTab(
                    label = "System",
                    selected = selectedFilter == AppListFilter.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterChange(AppListFilter.SYSTEM) }
                )

                FilterTab(
                    label = "All",
                    selected = selectedFilter == AppListFilter.ALL,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterChange(AppListFilter.ALL) }
                )
            }

            Text(
                text = when {
                    loadingApps -> "Loading apps..."
                    checkingUpdates -> "Checking updates..."
                    updateError != null -> "Update check failed"
                    updatesCount == 1 -> "1 Update available"
                    else -> "$updatesCount Updates available"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (updateError == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = shape
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun InstalledAppCard(
    app: InstalledApp,
    update: UpdateInfo?,
    playUpdate: PlayUpdate?,
    installing: Boolean,
    onOpenAPKMirror: () -> Unit,
    onInstallPlay: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppIcon(icon = app.icon)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AppTypeLabel(isSystem = app.isSystem)
                }

                VersionBlock(
                    app = app,
                    update = update,
                    playVersion = playUpdate?.versionName,
                    playVersionCode = playUpdate?.versionCode
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = if (playUpdate != null) onInstallPlay else onOpenAPKMirror,
                        enabled = !installing,
                        contentPadding = PaddingValues(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = when {
                                installing -> "Installing…"
                                playUpdate != null -> "Update"
                                update != null -> "Update"
                                else -> "APKMirror"
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(icon: Bitmap?) {
    if (icon == null) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}
    } else {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun AppTypeLabel(isSystem: Boolean) {
    Surface(
        modifier = Modifier.heightIn(min = 36.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSystem) "S" else "U",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VersionBlock(
    app: InstalledApp,
    update: UpdateInfo?,
    playVersion: String? = null,
    playVersionCode: Long? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = when {
                playVersion != null -> "${app.versionName} -> $playVersion"
                update != null -> buildUpdateLine(app.versionName, update)
                else -> "Installed: ${app.versionName}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = when {
                playVersionCode != null -> "${app.versionCode} -> $playVersionCode"
                update != null -> "${app.versionCode} -> ${update.versionCode}"
                else -> "Version code: ${app.versionCode}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun UniformCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(28.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

private fun buildUpdateLine(
    installedVersionName: String,
    update: UpdateInfo
): String {
    return "$installedVersionName -> ${update.versionName}"
}

fun filterApps(
    apps: List<InstalledApp>,
    updates: Map<String, UpdateInfo>,
    filter: AppListFilter,
    query: String,
    extraUpdatePackages: Set<String> = emptySet()
): List<InstalledApp> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)

    return apps
        .filter { app ->
            val matchesFilter = when (filter) {
                AppListFilter.ALL -> true
                AppListFilter.USER -> !app.isSystem
                AppListFilter.SYSTEM -> app.isSystem
                AppListFilter.UPDATES -> updates.containsKey(app.packageName) || app.packageName in extraUpdatePackages
            }

            val matchesQuery = normalizedQuery.isEmpty() ||
                app.label.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                app.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)

            matchesFilter && matchesQuery
        }
        .sortedWith(
            compareBy<InstalledApp> { app ->
                if (updates.containsKey(app.packageName) || app.packageName in extraUpdatePackages) 0 else 1
            }.thenBy { app ->
                app.label.lowercase(Locale.ROOT)
            }.thenBy { app ->
                app.packageName
            }
        )
}

fun scanInstalledApps(
    packageManager: PackageManager
): List<InstalledApp> {
    return packageManager
        .getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        .mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            InstalledApp(
                label = appInfo.loadLabel(packageManager).toString(),
                packageName = info.packageName,
                versionName = info.versionName ?: "Unknown",
                versionCode = info.longVersionCode,
                isSystem = isSystem,
                icon = appInfo.loadIcon(packageManager).toBitmap(size = 96)
            )
        }
        .sortedWith(
            compareBy<InstalledApp> { it.label.lowercase(Locale.ROOT) }
                .thenBy { it.packageName }
        )
}

fun Drawable.toBitmap(size: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap
    }

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap
}

fun openAPKMirror(
    context: Context,
    packageName: String,
    update: UpdateInfo?
) {
    val uri = update?.url?.let { android.net.Uri.parse(it) }
        ?: ApkMirrorSource.searchUrl(packageName)

    context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            uri
        )
    )
}


private fun installPlayUpdate(
    context: Context,
    scope: CoroutineScope,
    update: PlayUpdate,
    onState: (Boolean) -> Unit
) {
    if (!PlayInstaller.canInstall(context)) {
        PlayInstaller.requestPermission(context)
        return
    }
    onState(true)
    scope.launch {
        val files = PlayRepository.files(context, update)
        if (files.isEmpty()) {
            onState(false)
            Toast.makeText(context, "Play: no downloadable files", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val downloaded = runCatching { PlayDownloader.download(context, files) }.getOrDefault(emptyList())
        if (downloaded.isEmpty()) {
            onState(false)
            Toast.makeText(context, "Play: download failed", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val installed = runCatching { PlayInstaller.install(context, update.packageName, downloaded) }.getOrDefault(false)
        onState(false)
        Toast.makeText(
            context,
            if (installed) "Installed ${update.displayName}" else "Install failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}
