package com.example.dfuselauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.dfuselauncher.ui.theme.DfuseLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String
)

enum class ScreenState {
    HOME,
    SETTINGS,
    WALLPAPERS
}

data class WallpaperOption(
    val name: String,
    val imageUrl: String
)

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DfuseLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
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
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    @Composable
    fun LauncherScreen(
        packageManager: PackageManager,
        onLaunchApp: (String) -> Unit
    ) {
        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
        var selectedWallpaperUrl by remember { mutableStateOf<String?>(null) }
        var settingsFocused by remember { mutableStateOf(false) }

        val settingsFocusRequester = remember { FocusRequester() }

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
            )
        )

        LaunchedEffect(Unit) {
            apps = withContext(Dispatchers.IO) {
                loadLaunchableApps(packageManager, "com.example.dfuselauncher")
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    .background(Color(0xBB101010))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 60.dp, top = 40.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DFUSE TV",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .focusRequester(settingsFocusRequester)
                            .onFocusChanged { settingsFocused = it.isFocused }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (settingsFocused) {
                                    Color.White.copy(alpha = 0.12f)
                                } else {
                                    Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                width = if (settingsFocused) 2.dp else 1.dp,
                                color = if (settingsFocused) {
                                    Color.White.copy(alpha = 0.8f)
                                } else {
                                    Color.White.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .focusable()
                            .clickable {
                                currentScreen = ScreenState.SETTINGS
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚙",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = when (currentScreen) {
                        ScreenState.HOME -> "Apps"
                        ScreenState.SETTINGS -> "Settings"
                        ScreenState.WALLPAPERS -> "Wallpapers"
                    },
                    fontSize = 18.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                when (currentScreen) {
                    ScreenState.HOME -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(apps.size) { index ->
                                val app = apps[index]

                                AppCard(
                                    app = app,
                                    onClick = {
                                        onLaunchApp(app.packageName)
                                    },
                                    modifier = if (index < 5) {
                                        Modifier.focusProperties {
                                            up = settingsFocusRequester
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                            }
                        }
                    }

                    ScreenState.SETTINGS -> {
                        SettingsListScreen(
                            onWallpaperClick = {
                                currentScreen = ScreenState.WALLPAPERS
                            },
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
                                currentScreen = ScreenState.HOME
                            },
                            onUseDefault = {
                                selectedWallpaperUrl = null
                                currentScreen = ScreenState.HOME
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
    fun AppCard(
        app: AppInfo,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.06f)
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .focusable()
                .clickable { onClick() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun SettingsListScreen(
        onWallpaperClick: () -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsListItem(
                title = "Wallpapers",
                onClick = onWallpaperClick
            )

            SettingsListItem(
                title = "Refresh Apps",
                onClick = { }
            )

            SettingsListItem(
                title = "Appearance",
                onClick = { }
            )

            SettingsListItem(
                title = "About",
                onClick = { }
            )

            SettingsListItem(
                title = "Back",
                onClick = onBack
            )
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
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.06f)
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsListItem(
                title = "Use Default Background",
                onClick = onUseDefault
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(wallpapers) { wallpaper ->
                    RemoteWallpaperCard(
                        wallpaper = wallpaper,
                        onClick = {
                            onSelectWallpaper(wallpaper.imageUrl)
                        }
                    )
                }
            }

            SettingsListItem(
                title = "Back",
                onClick = onBack
            )
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
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .focusable()
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
                    .background(Color(0x55000000))
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

    fun loadLaunchableApps(
        packageManager: PackageManager,
        myPackageName: String
    ): List<AppInfo> {
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val leanbackApps = packageManager.queryIntentActivities(leanbackIntent, 0)

        return leanbackApps
            .map {
                AppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .filter { it.packageName != myPackageName }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }
}