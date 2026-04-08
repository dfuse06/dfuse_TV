package com.example.dfuselauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.dfuselauncher.ui.theme.DfuseLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

enum class ScreenState {
    HOME,
    SETTINGS,
    WALLPAPERS,
    FAVORITES
}

data class WallpaperOption(
    val name: String,
    val imageUrl: String
)

class MainActivity : ComponentActivity() {

    private val prefsName = "dfuse_settings"
    private val wallpaperKey = "selected_wallpaper_url"
    private val favoritesKey = "favorite_packages"

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DfuseLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LauncherScreen(
                        packageManager = packageManager,
                        onLaunchApp = { packageName ->
                            launchApp(packageName)
                        }
                    )
                }
            }
        }
    }

    private fun launchApp(packageName: String) {
        try {
            if (packageName.contains("settings", ignoreCase = true)) {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                return
            }

            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openQuickSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openScreensaverSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_DREAM_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fallback)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun saveSelectedWallpaper(url: String?) {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit().putString(wallpaperKey, url).apply()
    }

    private fun loadSelectedWallpaper(): String? {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        return prefs.getString(wallpaperKey, null)
    }

    private fun loadFavoritePackages(): MutableSet<String> {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        return prefs.getStringSet(favoritesKey, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveFavoritePackages(favorites: Set<String>) {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit().putStringSet(favoritesKey, favorites).apply()
    }

    private fun toggleFavorite(packageName: String): MutableSet<String> {
        val favorites = loadFavoritePackages()
        if (favorites.contains(packageName)) {
            favorites.remove(packageName)
        } else {
            favorites.add(packageName)
        }
        saveFavoritePackages(favorites)
        return favorites
    }

    @Composable
    fun LauncherScreen(
        packageManager: PackageManager,
        onLaunchApp: (String) -> Unit
    ) {
        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var favoritePackages by remember { mutableStateOf(loadFavoritePackages()) }
        var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
        var selectedWallpaperUrl by remember { mutableStateOf(loadSelectedWallpaper()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()

        fun refreshApps() {
            scope.launch {
                apps = withContext(Dispatchers.IO) {
                    loadLaunchableApps(packageManager, packageName)
                }
            }
        }

        LaunchedEffect(Unit) {
            refreshApps()
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshApps()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }


        val cloudinaryWallpapers = listOf(
            WallpaperOption(
                name = "Dfuse TV",
                imageUrl = "https://res.cloudinary.com/dfuhaulsy/image/upload/v1775408372/dfuse_2_gnbrr1.png"
            ),
            WallpaperOption(
                name = "BrokenOS",
                imageUrl = "https://res.cloudinary.com/dfuhaulsy/image/upload/v1775445424/dfcc270c-c9ca-4824-90fd-d0f72f508cf4_todwbg.png"
            ),
            WallpaperOption(
                name = "Hacker",
                imageUrl = "https://res.cloudinary.com/dfuhaulsy/image/upload/v1775444412/9f662600-76d8-4e97-a78c-6010d5174120_x2bui3.png"
            ),
            WallpaperOption(
                name = "Hip Hop Rock",
                imageUrl = "https://res.cloudinary.com/dfuhaulsy/image/upload/v1775444413/6033ea60-c1db-4960-b16b-1aae6738d26a_uttany.png"
            ),
            WallpaperOption(
                name = "Beach",
                imageUrl = "https://res.cloudinary.com/dfuhaulsy/image/upload/v1775612580/ft_luaderdale_dqynsy.png"
            )
        )

        BackHandler(enabled = currentScreen != ScreenState.HOME) {
            currentScreen = when (currentScreen) {
                ScreenState.WALLPAPERS -> ScreenState.SETTINGS
                ScreenState.FAVORITES -> ScreenState.SETTINGS
                ScreenState.SETTINGS -> ScreenState.HOME
                ScreenState.HOME -> ScreenState.HOME
            }
        }

        LaunchedEffect(Unit) {
            apps = withContext(Dispatchers.IO) {
                loadLaunchableApps(packageManager, packageName)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedWallpaperUrl != null && selectedWallpaperUrl!!.startsWith("http")) {
                AsyncImage(
                    model = selectedWallpaperUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.dfuse_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990A0A0A))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 60.dp, top = 40.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "DFUSE TV",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                if (currentScreen == ScreenState.HOME) {
                    TopActionRow(
                        currentScreen = currentScreen,
                        onGoHome = {
                            currentScreen = ScreenState.HOME
                        },
                        onOpenScreensaver = {
                            openScreensaverSettings()
                        },
                        onOpenQuickSettings = {
                            openQuickSettings()
                        },
                        onOpenDfuseSettings = {
                            currentScreen = ScreenState.SETTINGS
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = when (currentScreen) {
                        ScreenState.HOME -> "Home"
                        ScreenState.SETTINGS -> "DFUSE Settings"
                        ScreenState.WALLPAPERS -> "Wallpapers"
                        ScreenState.FAVORITES -> "Manage Favorites"
                    },
                    fontSize = 18.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                when (currentScreen) {
                    ScreenState.HOME -> {
                        val favoriteApps = apps.filter { favoritePackages.contains(it.packageName) }
                        val nonFavoriteApps = apps.filter { !favoritePackages.contains(it.packageName) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (favoriteApps.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Favorites",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                items(favoriteApps.chunked(6)) { rowApps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowApps.forEach { app ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                FavoriteAppCard(
                                                    app = app,
                                                    onClick = { onLaunchApp(app.packageName) }
                                                )
                                            }
                                        }

                                        repeat(6 - rowApps.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "All Apps",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            item {
                                CircularAppsRow(
                                    apps = nonFavoriteApps,
                                    onLaunchApp = onLaunchApp
                                )
                            }
                        }
                    }

                    ScreenState.SETTINGS -> {
                        SettingsListScreen(
                            onWallpaperClick = {
                                currentScreen = ScreenState.WALLPAPERS
                            },
                            onFavoritesClick = {
                                currentScreen = ScreenState.FAVORITES
                            },
                            onRefreshApps = { refreshApps() },
                            onBack = {
                                currentScreen = ScreenState.HOME
                            }
                        )
                    }

                    ScreenState.WALLPAPERS -> {
                        WallpaperSettingsScreen(
                            wallpapers = cloudinaryWallpapers,
                            onSelectWallpaper = { url ->
                                selectedWallpaperUrl = url
                                saveSelectedWallpaper(url)
                                currentScreen = ScreenState.HOME
                            },
                            onUseDefault = {
                                selectedWallpaperUrl = null
                                saveSelectedWallpaper(null)
                                currentScreen = ScreenState.HOME
                            },
                            onBack = {
                                currentScreen = ScreenState.SETTINGS
                            }
                        )
                    }

                    ScreenState.FAVORITES -> {
                        FavoritesManagementScreen(
                            apps = apps,
                            favoritePackages = favoritePackages,
                            onToggleFavorite = { packageName ->
                                favoritePackages = toggleFavorite(packageName)
                            },
                            onBack = {
                                currentScreen = ScreenState.SETTINGS
                            }
                        )
                    }

                }
            }
        }
    }

    @Composable
    fun TopActionRow(
        currentScreen: ScreenState,
        onGoHome: () -> Unit,
        onOpenScreensaver: () -> Unit,
        onOpenQuickSettings: () -> Unit,
        onOpenDfuseSettings: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopActionButton(
                    label = "Quick Settings",
                    icon = Icons.Filled.Settings,
                    onClick = onOpenQuickSettings
                )

                TopActionButton(
                    label = "DFUSE Settings",
                    icon = Icons.Filled.Tune,
                    isSelected = currentScreen == ScreenState.SETTINGS ||
                            currentScreen == ScreenState.WALLPAPERS ||
                            currentScreen == ScreenState.FAVORITES,
                    onClick = onOpenDfuseSettings
                )
            }
        }
    }

    @Composable
    fun TopActionButton(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .scale(if (isFocused) 1.03f else 1f)
                .shadow(
                    elevation = if (isFocused) 12.dp else 4.dp,
                    shape = RoundedCornerShape(50)
                )
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        isFocused -> Color.White.copy(alpha = 0.16f)
                        isSelected -> Color.White.copy(alpha = 0.12f)
                        else -> Color.White.copy(alpha = 0.08f)
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.9f)
                    } else {
                        Color.White.copy(alpha = 0.20f)
                    },
                    shape = RoundedCornerShape(50)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .clickable { onClick() }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFocused || isSelected) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    fun FavoriteAppCard(
        app: AppInfo,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .scale(if (isFocused) 1.03f else 1f)
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isFocused) Color.White.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.08f)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White.copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = app.icon,
                    contentDescription = app.name,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun CircularAppsRow(
        apps: List<AppInfo>,
        onLaunchApp: (String) -> Unit
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(apps) { app ->
                CircularAppButton(
                    app = app,
                    onClick = { onLaunchApp(app.packageName) }
                )
            }
        }
    }

    @Composable
    fun CircularAppButton(
        app: AppInfo,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.width(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isFocused) 92.dp else 84.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFocused) Color.White.copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) Color.White.copy(alpha = 0.90f)
                        else Color.White.copy(alpha = 0.20f),
                        shape = CircleShape
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    AsyncImage(
                        model = app.icon,
                        contentDescription = app.name,
                        modifier = Modifier.size(46.dp)
                    )
                } else {
                    Text(
                        text = app.name.take(1),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = app.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun SettingsListScreen(
        onWallpaperClick: () -> Unit,
        onFavoritesClick: () -> Unit,
        onRefreshApps: () -> Unit,
        onBack: () -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { SettingsListItem("Wallpapers", onWallpaperClick) }
            item { SettingsListItem("Manage Favorites", onFavoritesClick) }
            item { SettingsListItem("Refresh Apps", onRefreshApps)}
            item { SettingsListItem("Appearance", {}) }
            item { SettingsListItem("About", {}) }
            item { SettingsListItem("Back", onBack) }
        }
    }

    @Composable
    fun SettingsListItem(
        title: String,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (isFocused) 1.02f else 1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) Color.White.copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.06f)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White.copy(alpha = 0.90f)
                    else Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(16.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }

    @Composable
    fun WallpaperSettingsScreen(
        wallpapers: List<WallpaperOption>,
        onSelectWallpaper: (String) -> Unit,
        onUseDefault: () -> Unit,
        onBack: () -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                SettingsListItem(
                    title = "Use Default Background",
                    onClick = onUseDefault
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(700.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(wallpapers) { wallpaper ->
                        RemoteWallpaperCard(
                            wallpaper = wallpaper,
                            onClick = { onSelectWallpaper(wallpaper.imageUrl) }
                        )
                    }
                }
            }

            item {
                SettingsListItem(
                    title = "Back",
                    onClick = onBack
                )
            }
        }
    }

    @Composable
    fun RemoteWallpaperCard(
        wallpaper: WallpaperOption,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.7f)
                .scale(if (isFocused) 1.03f else 1f)
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White.copy(alpha = 0.90f)
                    else Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
        ) {
            AsyncImage(
                model = wallpaper.imageUrl,
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = wallpaper.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    fun FavoritesManagementScreen(
        apps: List<AppInfo>,
        favoritePackages: Set<String>,
        onToggleFavorite: (String) -> Unit,
        onBack: () -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "Select apps to favorite",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(700.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps) { app ->
                        FavoriteToggleCard(
                            app = app,
                            isFavorite = favoritePackages.contains(app.packageName),
                            onClick = { onToggleFavorite(app.packageName) }
                        )
                    }
                }
            }

            item {
                SettingsListItem(
                    title = "Back",
                    onClick = onBack
                )
            }
        }
    }

    @Composable
    fun FavoriteToggleCard(
        app: AppInfo,
        isFavorite: Boolean,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .scale(if (isFocused) 1.03f else 1f)
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) Color.White.copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.06f)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White.copy(alpha = 0.90f)
                    else Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = app.icon,
                    contentDescription = app.name,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isFavorite) "★ Favorite" else "+ Add Favorite",
                    color = Color.White.copy(alpha = if (isFavorite) 1f else 0.75f),
                    fontSize = 14.sp
                )
            }
        }
    }

    private fun loadLaunchableApps(
        packageManager: PackageManager,
        myPackageName: String
    ): List<AppInfo> {
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val leanbackApps = packageManager.queryIntentActivities(leanbackIntent, 0)
        val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)

        return (leanbackApps + launcherApps)
            .map {
                AppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(packageManager)
                )
            }
            .filter { it.packageName != myPackageName }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }
}