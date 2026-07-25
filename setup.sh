#!/bin/bash
echo "📁 إنشاء مشروع SiteManager..."

# ═══════════════════════════════════════
# إنشاء المجلدات
# ═══════════════════════════════════════
mkdir -p app/src/main/java/com/example/sitemanager/data/local
mkdir -p app/src/main/java/com/example/sitemanager/data/repository
mkdir -p app/src/main/java/com/example/sitemanager/ui/screens
mkdir -p app/src/main/java/com/example/sitemanager/ui/components
mkdir -p app/src/main/java/com/example/sitemanager/ui/theme
mkdir -p app/src/main/java/com/example/sitemanager/ui/viewmodel
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p gradle/wrapper
mkdir -p .github/workflows

echo "✅ المجلدات"

# ═══════════════════════════════════════
# build.gradle.kts (Project)
# ═══════════════════════════════════════
cat > build.gradle.kts << 'EOF'
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
EOF

# ═══════════════════════════════════════
# settings.gradle.kts
# ═══════════════════════════════════════
cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SiteManager"
include(":app")
EOF

# ═══════════════════════════════════════
# gradle.properties
# ═══════════════════════════════════════
cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
EOF

# ═══════════════════════════════════════
# gradle-wrapper.properties
# ═══════════════════════════════════════
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# ═══════════════════════════════════════
# .gitignore
# ═══════════════════════════════════════
cat > .gitignore << 'EOF'
.gradle/
build/
*.iml
local.properties
.DS_Store
EOF

# ═══════════════════════════════════════
# .github/workflows/build.yml
# ═══════════════════════════════════════
cat > .github/workflows/build.yml << 'EOF'
name: Build APK
on:
  push:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v3
      - run: gradle wrapper --gradle-version 8.5
      - run: ./gradlew assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: site-manager-debug
          path: app/build/outputs/apk/debug/*.apk
EOF

# ═══════════════════════════════════════
# app/build.gradle.kts
# ═══════════════════════════════════════
cat > app/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.sitemanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sitemanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coil (favicon loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Kotlinx coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
EOF

# ═══════════════════════════════════════
# AndroidManifest.xml
# ═══════════════════════════════════════
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize">

            <!-- Launcher -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- استقبال المشاركة من المتصفح -->
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>

            <!-- فتح الروابط مباشرة -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" />
                <data android:scheme="https" />
            </intent-filter>

        </activity>

    </application>
</manifest>
EOF

# ═══════════════════════════════════════
# strings.xml
# ═══════════════════════════════════════
cat > app/src/main/res/values/strings.xml << 'EOF'
<resources>
    <string name="app_name">مدير المواقع</string>
</resources>
EOF

echo "✅ ملفات البناء وال Android"

# ═══════════════════════════════════════
# SiteEntity.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/data/local/SiteEntity.kt << 'EOF'
package com.example.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val tabType: String,        // "favorites" أو "saved"
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clickCount: Int = 0
)
EOF

# ═══════════════════════════════════════
# SiteDao.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/data/local/SiteDao.kt << 'EOF'
package com.example.sitemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Query("SELECT * FROM sites WHERE tabType = 'favorites' ORDER BY clickCount DESC")
    fun getFavorites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE tabType = 'saved' ORDER BY createdAt DESC")
    fun getSaved(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY clickCount DESC")
    fun search(query: String): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: SiteEntity): Long

    @Update
    suspend fun update(site: SiteEntity)

    @Delete
    suspend fun delete(site: SiteEntity)

    @Query("UPDATE sites SET tabType = :newTabType WHERE id = :id")
    suspend fun moveToTab(id: Long, newTabType: String)

    @Query("UPDATE sites SET clickCount = clickCount + 1 WHERE id = :id")
    suspend fun incrementClickCount(id: Long)

    @Query("UPDATE sites SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
}
EOF

# ═══════════════════════════════════════
# SiteDatabase.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/data/local/SiteDatabase.kt << 'EOF'
package com.example.sitemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SiteEntity::class], version = 1, exportSchema = false)
abstract class SiteDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "site_manager_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
EOF

# ═══════════════════════════════════════
# SiteRepository.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/data/repository/SiteRepository.kt << 'EOF'
package com.example.sitemanager.data.repository

import com.example.sitemanager.data.local.SiteDao
import com.example.sitemanager.data.local.SiteEntity
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val dao: SiteDao) {

    fun getFavorites(): Flow<List<SiteEntity>> = dao.getFavorites()

    fun getSaved(): Flow<List<SiteEntity>> = dao.getSaved()

    fun search(query: String): Flow<List<SiteEntity>> = dao.search(query)

    suspend fun addSite(site: SiteEntity): Boolean {
        val existing = dao.getByUrl(site.url)
        if (existing != null) return false
        dao.insert(site)
        return true
    }

    suspend fun update(site: SiteEntity) = dao.update(site)

    suspend fun delete(site: SiteEntity) = dao.delete(site)

    suspend fun moveToTab(id: Long, newTabType: String) = dao.moveToTab(id, newTabType)

    suspend fun incrementClick(id: Long) = dao.incrementClickCount(id)

    suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)
}
EOF

echo "✅ Data Layer"

# ═══════════════════════════════════════
# Color.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/theme/Color.kt << 'EOF'
package com.example.sitemanager.ui.theme

import androidx.compose.ui.graphics.Color

val Blue900 = Color(0xFF1A237E)
val Blue700 = Color(0xFF1976D2)
val Blue200 = Color(0xFF90CAF9)
val Blue50 = Color(0xFFE3F2FD)
val Orange500 = Color(0xFFFF9800)
val Gray100 = Color(0xFFF5F5F5)
val Gray300 = Color(0xFFE0E0E0)
val Gray600 = Color(0xFF757575)
val DarkSurface = Color(0xFF1E1E1E)
val DarkBackground = Color(0xFF121212)
EOF

# ═══════════════════════════════════════
# Theme.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/theme/Theme.kt << 'EOF'
package com.example.sitemanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Blue900,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue900,
    secondary = Orange500,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Gray100,
    background = Gray100,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = Gray600
)

private val DarkColors = darkColorScheme(
    primary = Blue200,
    onPrimary = Blue900,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue200,
    secondary = Orange500,
    surface = DarkSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2C),
    background = DarkBackground,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0)
)

@Composable
fun SiteManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
EOF

echo "✅ Theme"

# ═══════════════════════════════════════
# SiteViewModel.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/viewmodel/SiteViewModel.kt << 'EOF'
package com.example.sitemanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sitemanager.data.local.SiteDatabase
import com.example.sitemanager.data.local.SiteEntity
import com.example.sitemanager.data.repository.SiteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SiteManagerUiState(
    val selectedTab: Int = 0,       // 0 = favorites, 1 = saved
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val showAddSheet: Boolean = false,
    val sharedUrl: String? = null,
    val sharedTitle: String? = null,
    val editingSite: SiteEntity? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SiteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SiteDatabase.getDatabase(application)
    private val repository = SiteRepository(db.siteDao())

    private val _uiState = MutableStateFlow(SiteManagerUiState())
    val uiState: StateFlow<SiteManagerUiState> = _uiState.asStateFlow()

    // ═══ بحث ═══
    private val _searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<SiteEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                repository.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ التبويبات ═══
    val favorites: StateFlow<List<SiteEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSites: StateFlow<List<SiteEntity>> = repository.getSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ اختيار التبويب ═══
    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // ═══ البحث ═══
    fun onSearchToggle() {
        val current = _uiState.value
        if (current.isSearching) {
            _uiState.value = current.copy(isSearching = false, searchQuery = "")
            _searchQuery.value = ""
        } else {
            _uiState.value = current.copy(isSearching = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    // ═══ استقبال المشاركة ═══
    fun onShareReceived(url: String?, title: String?) {
        if (url.isNullOrBlank()) return
        _uiState.value = _uiState.value.copy(
            showAddSheet = true,
            sharedUrl = url,
            sharedTitle = title ?: extractDomain(url)
        )
    }

    fun onAddSheetDismissed() {
        _uiState.value = _uiState.value.copy(
            showAddSheet = false,
            sharedUrl = null,
            sharedTitle = null
        )
    }

    fun addSite(tabType: String, customTitle: String? = null) {
        val url = _uiState.value.sharedUrl ?: return
        val title = customTitle?.ifBlank { null }
            ?: _uiState.value.sharedTitle
            ?: extractDomain(url)

        val faviconUrl = "https://www.google.com/s2/favicons?domain=${extractDomain(url)}&sz=64"

        viewModelScope.launch {
            val site = SiteEntity(
                url = url,
                title = title,
                tabType = tabType,
                faviconUrl = faviconUrl
            )
            val added = repository.addSite(site)
            _uiState.value = _uiState.value.copy(
                showAddSheet = false,
                sharedUrl = null,
                sharedTitle = null,
                message = if (added) "تمت الإضافة" else "الموقع موجود مسبقاً"
            )
        }
    }

    // ═══ فتح الموقع ═══
    fun onSiteOpened(site: SiteEntity) {
        viewModelScope.launch {
            repository.incrementClick(site.id)
        }
    }

    // ═══ تعديل الاسم ═══
    fun onEditSite(site: SiteEntity) {
        _uiState.value = _uiState.value.copy(editingSite = site)
    }

    fun onEditDismissed() {
        _uiState.value = _uiState.value.copy(editingSite = null)
    }

    fun onTitleUpdated(newTitle: String) {
        val site = _uiState.value.editingSite ?: return
        viewModelScope.launch {
            repository.updateTitle(site.id, newTitle)
            _uiState.value = _uiState.value.copy(
                editingSite = null,
                message = "تم التعديل"
            )
        }
    }

    // ═══ نقل بين التبويبات ═══
    fun onMoveToTab(site: SiteEntity) {
        val newTab = if (site.tabType == "favorites") "saved" else "favorites"
        viewModelScope.launch {
            repository.moveToTab(site.id, newTab)
            _uiState.value = _uiState.value.copy(
                message = if (newTab == "favorites") "نُقل إلى الأكثر استخداماً" else "نُقل إلى المحفوظات"
            )
        }
    }

    // ═══ حذف ═══
    fun onDeleteSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.delete(site)
            _uiState.value = _uiState.value.copy(message = "تم الحذف")
        }
    }

    // ═══ رسالة ═══
    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    // ═══ أداة مساعدة ═══
    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url.take(30)
        }
    }
}
EOF

echo "✅ ViewModel"

# ═══════════════════════════════════════
# SiteCard.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/components/SiteCard.kt << 'EOF'
package com.example.sitemanager.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sitemanager.data.local.SiteEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SiteCard(
    site: SiteEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ═══ أيقونة الموقع ═══
            AsyncImage(
                model = site.faviconUrl
                    ?: "https://www.google.com/s2/favicons?domain=${site.url}&sz=64",
                contentDescription = site.title,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // ═══ معلومات الموقع ═══
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (site.clickCount > 0) {
                    Text(
                        text = "تم الفتح ${site.clickCount} مرة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ═══ أزرار الإجراءات ═══
            IconButton(
                onClick = {
                    onOpen()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
                    context.startActivity(intent)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "فتح",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
EOF

# ═══════════════════════════════════════
# AddSiteBottomSheet.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/components/AddSiteBottomSheet.kt << 'EOF'
package com.example.sitemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSiteBottomSheet(
    url: String,
    title: String,
    onAddToFavorites: (String?) -> Unit,
    onAddToSaved: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var customTitle by remember { mutableStateOf(title) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "إضافة موقع",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text("اسم الموقع (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "اختر التبويب:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))

            Row {
                Button(
                    onClick = {
                        onAddToFavorites(customTitle.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.padding(end = 4.dp))
                    Text("الأكثر استخداماً")
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        onAddToSaved(customTitle.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 4.dp))
                    Text("المحفوظات")
                }
            }
        }
    }
}
EOF

# ═══════════════════════════════════════
# EditNameBottomSheet.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/components/EditNameBottomSheet.kt << 'EOF'
package com.example.sitemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sitemanager.data.local.SiteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameBottomSheet(
    site: SiteEntity,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(site.title) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "تعديل اسم الموقع",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = site.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("اسم الموقع") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onSave(title) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("حفظ")
            }
        }
    }
}
EOF

echo "✅ Components"

# ═══════════════════════════════════════
# MainScreen.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/ui/screens/MainScreen.kt << 'EOF'
package com.example.sitemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitemanager.data.local.SiteEntity
import com.example.sitemanager.ui.components.AddSiteBottomSheet
import com.example.sitemanager.ui.components.EditNameBottomSheet
import com.example.sitemanager.ui.components.SiteCard
import com.example.sitemanager.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SiteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val savedSites by viewModel.savedSites.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ═══ تأكيد الحذف ═══
    var showDeleteDialog by remember { mutableStateOf<SiteEntity?>(null) }

    // ═══ القائمة المنبثقة (Long Press) ═══
    var showContextMenu by remember { mutableStateOf<SiteEntity?>(null) }

    // ═══ رسالة ═══
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSearching) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("ابحث عن موقع...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = "مدير المواقع",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onSearchToggle() }) {
                        Icon(
                            if (uiState.isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ═══ عرض نتائج البحث ═══
            if (uiState.isSearching && uiState.searchQuery.isNotBlank()) {
                SearchResultsList(
                    results = searchResults,
                    onSiteClick = { viewModel.onSiteOpened(it) },
                    onEdit = { viewModel.onEditSite(it) },
                    onMove = { viewModel.onMoveToTab(it) },
                    onDelete = { showDeleteDialog = it },
                    onLongClick = { showContextMenu = it }
                )
            } else {
                // ═══ التبويبات ═══
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = {
                            Text("الأكثر استخداماً (${favorites.size})")
                        },
                        icon = { Icon(Icons.Default.Favorite, null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = {
                            Text("المحفوظات (${savedSites.size})")
                        },
                        icon = { Icon(Icons.Default.Bookmark, null) }
                    )
                }

                // ═══ محتوى التبويب ═══
                val currentList = if (uiState.selectedTab == 0) favorites else savedSites

                if (currentList.isEmpty()) {
                    EmptyState(
                        if (uiState.selectedTab == 0)
                            "لا توجد مواقع في الأكثر استخداماً"
                        else
                            "لا توجد مواقع محفوظة"
                    )
                } else {
                    SiteList(
                        sites = currentList,
                        onSiteClick = { viewModel.onSiteOpened(it) },
                        onEdit = { viewModel.onEditSite(it) },
                        onMove = { viewModel.onMoveToTab(it) },
                        onDelete = { showDeleteDialog = it },
                        onLongClick = { showContextMenu = it }
                    )
                }
            }
        }
    }

    // ═══ Bottom Sheet: إضافة موقع (من المشاركة) ═══
    if (uiState.showAddSheet && uiState.sharedUrl != null) {
        AddSiteBottomSheet(
            url = uiState.sharedUrl!!,
            title = uiState.sharedTitle ?: "",
            onAddToFavorites = { title ->
                viewModel.addSite("favorites", title)
            },
            onAddToSaved = { title ->
                viewModel.addSite("saved", title)
            },
            onDismiss = { viewModel.onAddSheetDismissed() }
        )
    }

    // ═══ Bottom Sheet: تعديل الاسم ═══
    if (uiState.editingSite != null) {
        EditNameBottomSheet(
            site = uiState.editingSite!!,
            onSave = { viewModel.onTitleUpdated(it) },
            onDismiss = { viewModel.onEditDismissed() }
        )
    }

    // ═══ مربع حوار تأكيد الحذف ═══
    showDeleteDialog?.let { site ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("حذف الموقع", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف \"${site.title}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteSite(site)
                    showDeleteDialog = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ═══ قائمة منبثقة (Long Press) ═══
    showContextMenu?.let { site ->
        AlertDialog(
            onDismissRequest = { showContextMenu = null },
            title = { Text(site.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ContextMenuItem("🌐 فتح في المتصفح") {
                        viewModel.onSiteOpened(site)
                        showContextMenu = null
                    }
                    ContextMenuItem("✏️ تعديل الاسم") {
                        viewModel.onEditSite(site)
                        showContextMenu = null
                    }
                    ContextMenuItem(
                        if (site.tabType == "favorites") "↔️ نقل إلى المحفوظات"
                        else "↔️ نقل إلى الأكثر استخداماً"
                    ) {
                        viewModel.onMoveToTab(site)
                        showContextMenu = null
                    }
                    ContextMenuItem("🗑️ حذف") {
                        showDeleteDialog = site
                        showContextMenu = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContextMenu = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun SiteList(
    sites: List<SiteEntity>,
    onSiteClick: (SiteEntity) -> Unit,
    onEdit: (SiteEntity) -> Unit,
    onMove: (SiteEntity) -> Unit,
    onDelete: (SiteEntity) -> Unit,
    onLongClick: (SiteEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = sites, key = { it.id }) { site ->
            SiteCard(
                site = site,
                onOpen = { onSiteClick(site) },
                onEdit = { onEdit(site) },
                onMove = { onMove(site) },
                onDelete = { onDelete(site) },
                onClick = { onSiteClick(site) },
                onLongClick = { onLongClick(site) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SiteEntity>,
    onSiteClick: (SiteEntity) -> Unit,
    onEdit: (SiteEntity) -> Unit,
    onMove: (SiteEntity) -> Unit,
    onDelete: (SiteEntity) -> Unit,
    onLongClick: (SiteEntity) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState("لا توجد نتائج")
    } else {
        SiteList(results, onSiteClick, onEdit, onMove, onDelete, onLongClick)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📂", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "شارك رابطاً من المتصفح لإضافته هنا",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContextMenuItem(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.fillMaxWidth())
    }
}
EOF

echo "✅ Screens"

# ═══════════════════════════════════════
# MainActivity.kt
# ═══════════════════════════════════════
cat > app/src/main/java/com/example/sitemanager/MainActivity.kt << 'EOF'
package com.example.sitemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sitemanager.ui.screens.MainScreen
import com.example.sitemanager.ui.theme.SiteManagerTheme
import com.example.sitemanager.ui.viewmodel.SiteViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            SiteManagerTheme {
                val viewModel: SiteViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

            if (!url.isNullOrBlank()) {
                // استخراج URL من النص (قد يحتوي على نص إضافي)
                val extractedUrl = extractUrl(url)
                setContent {
                    SiteManagerTheme {
                        val viewModel: SiteViewModel = viewModel()
                        viewModel.onShareReceived(extractedUrl, subject)
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value ?: text.trim()
    }
}
EOF

echo "✅ MainActivity"

# ═══════════════════════════════════════
# التحقق النهائي
# ═══════════════════════════════════════
echo ""
echo "═══════════════════════════════════════════"
echo "✅ تم إنشاء جميع الملفات!"
echo "═══════════════════════════════════════════"
echo ""
echo "📁 ملفات Kotlin:"
find app/src -name "*.kt" | sort
echo ""
echo "📋 ملفات XML:"
find app/src -name "*.xml" | sort
echo ""
echo "📋 ملفات البناء:"
ls -la build.gradle.kts settings.gradle.kts gradle.properties .gitignore
