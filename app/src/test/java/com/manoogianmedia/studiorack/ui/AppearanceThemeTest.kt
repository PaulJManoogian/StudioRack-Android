package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeTest {
    @Test
    fun synchronizedThemeIdentifiersRoundTrip() {
        val settings = AppearanceSettings.fromJson(
            """{"app_theme":"graphite","performance_theme":"stage_black"}"""
        )

        assertEquals("graphite", settings.appTheme)
        assertEquals("stage_black", settings.performanceTheme)
        assertEquals("graphite", settings.toJson().getString("app_theme"))
        assertEquals("stage_black", settings.toJson().getString("performance_theme"))
    }

    @Test
    fun unknownThemeIdentifiersUseSafeDefaults() {
        val settings = AppearanceSettings.fromJson(
            """{"app_theme":"unknown","performance_theme":"unknown"}"""
        )

        assertEquals("leviathan_dark", settings.appTheme)
        assertEquals("app_theme", settings.performanceTheme)
    }
}
