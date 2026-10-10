package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

data class AppearanceSettings(
    val appTheme: String = "leviathan_dark",
    val performanceTheme: String = "app_theme",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("app_theme", cleanAppTheme(appTheme))
        .put("performance_theme", cleanPerformanceTheme(performanceTheme))

    companion object {
        val appThemeIds = setOf("leviathan_dark", "stage_black", "studio_light", "graphite", "follow_device")
        val performanceThemeIds = setOf("app_theme", "stage_black")

        fun fromJson(value: String): AppearanceSettings {
            val json = runCatching { JSONObject(value) }.getOrDefault(JSONObject())
            return AppearanceSettings(
                appTheme = cleanAppTheme(json.optString("app_theme")),
                performanceTheme = cleanPerformanceTheme(json.optString("performance_theme")),
            )
        }

        private fun cleanAppTheme(value: String): String = value.takeIf(appThemeIds::contains) ?: "leviathan_dark"
        private fun cleanPerformanceTheme(value: String): String = value.takeIf(performanceThemeIds::contains) ?: "app_theme"
    }
}

private val LeviathanDark = darkColorScheme(
    background = Color(0xFF07090F), surface = Color(0xFF121621), surfaceVariant = Color(0xFF202635),
    primary = Color(0xFFFF9D1E), onPrimary = Color(0xFF170E03), secondary = Color(0xFF42D9FF),
    onSecondary = Color(0xFF06111A), onBackground = Color(0xFFF7F7FB), onSurface = Color(0xFFF7F7FB),
    onSurfaceVariant = Color(0xFFAEB8CB), outline = Color(0x33FFFFFF), error = Color(0xFFE55757),
)

private val StageBlack = darkColorScheme(
    background = Color.Black, surface = Color(0xFF07090C), surfaceVariant = Color(0xFF11151B),
    primary = Color(0xFFFF9D1E), onPrimary = Color(0xFF170E03), secondary = Color(0xFF42D9FF),
    onSecondary = Color(0xFF06111A), onBackground = Color(0xFFF8F8FA), onSurface = Color(0xFFF8F8FA),
    onSurfaceVariant = Color(0xFFBBC2CE), outline = Color(0x3DFFFFFF), error = Color(0xFFE55757),
)

private val Graphite = darkColorScheme(
    background = Color(0xFF111318), surface = Color(0xFF1D2027), surfaceVariant = Color(0xFF292D36),
    primary = Color(0xFFE8A33E), onPrimary = Color(0xFF171108), secondary = Color(0xFF72C9DC),
    onSecondary = Color(0xFF071316), onBackground = Color(0xFFF2F3F6), onSurface = Color(0xFFF2F3F6),
    onSurfaceVariant = Color(0xFFB9C0CC), outline = Color(0x38FFFFFF), error = Color(0xFFE06464),
)

private val StudioLight = lightColorScheme(
    background = Color(0xFFEEF1F5), surface = Color.White, surfaceVariant = Color(0xFFE6EAF0),
    primary = Color(0xFFB75E00), onPrimary = Color.White, secondary = Color(0xFF006E88),
    onSecondary = Color.White, onBackground = Color(0xFF151A24), onSurface = Color(0xFF151A24),
    onSurfaceVariant = Color(0xFF566174), outline = Color(0x3D181F2B), error = Color(0xFFBB303B),
)

@Composable
fun studioColorScheme(themeId: String): ColorScheme = when (themeId) {
    "stage_black" -> StageBlack
    "studio_light" -> StudioLight
    "graphite" -> Graphite
    "follow_device" -> if (isSystemInDarkTheme()) LeviathanDark else StudioLight
    else -> LeviathanDark
}
