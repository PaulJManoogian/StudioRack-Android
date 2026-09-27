package com.manoogianmedia.studiorack.performance

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class PerformanceControlsTest {
    @Test
    fun mapsAllFourAssignablePedalActions() {
        val settings = PerformanceSettings(
            previousKey = "ArrowDown",
            nextKey = "ArrowUp",
            metronomeKey = "ArrowRight",
            muteKey = "ArrowLeft",
        )
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_DPAD_DOWN, settings))
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
        assertEquals(PedalAction.METRONOME, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertEquals(PedalAction.MUTE, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_ENTER, settings))
    }

    @Test
    fun reverseOnlySwapsSongNavigation() {
        val settings = PerformanceSettings(pedalReverse = true)
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertEquals(PedalAction.METRONOME, mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
        assertEquals(PedalAction.MUTE, mappedPedalAction(KeyEvent.KEYCODE_DPAD_DOWN, settings))
    }

    @Test
    fun mapsCommonAirTurnKeyboardModesPrecisely() {
        val settings = PerformanceSettings(
            previousKey = "PageUp",
            nextKey = "PageDown",
            metronomeKey = "Space",
            muteKey = "Enter",
        )
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_PAGE_UP, settings))
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_PAGE_DOWN, settings))
        assertEquals(PedalAction.METRONOME, mappedPedalAction(KeyEvent.KEYCODE_SPACE, settings))
        assertEquals(PedalAction.MUTE, mappedPedalAction(KeyEvent.KEYCODE_ENTER, settings))
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_TAB, settings))
    }

    @Test
    fun settingsRoundTripForOfflinePersistence() {
        val expected = PerformanceSettings(
            metronomeAutostart = true,
            metronomeMode = "downbeat",
            metronomeSound = "cowbell",
            pedalEnabled = true,
            pedalMode = "scroll",
            pedalReverse = true,
            pedalScrollAmount = "full",
            previousKey = "ArrowUp",
            nextKey = "ArrowDown",
            metronomeKey = "ArrowLeft",
            muteKey = "ArrowRight",
            pedalButtonCount = 6,
            pedalBindings = listOf(
                PedalBinding("PageUp", PedalAction.PREVIOUS_PAGE),
                PedalBinding("PageDown", PedalAction.NEXT_PAGE),
                PedalBinding("ArrowLeft", PedalAction.TOGGLE_PLAYBACK),
                PedalBinding("ArrowRight", PedalAction.TOGGLE_AUTOPLAY),
                PedalBinding("ArrowUp", PedalAction.METRONOME),
                PedalBinding("ArrowDown", PedalAction.IGNORE),
            ),
            showClock = false,
            attachmentPreferences = listOf("lyrics", "chart", "guitar_tab", "sheet_music", "other"),
        )
        val encoded = expected.toJson(pendingSync = true)
        assertEquals(1, encoded.getInt("_mobile_pending"))
        assertEquals(expected, PerformanceSettings.fromJson(encoded.toString()))
    }

    @Test
    fun flexibleBindingsSupportPlaybackAndIgnoreWithoutChangingLegacyDefaults() {
        val settings = PerformanceSettings(
            pedalButtonCount = 4,
            pedalBindings = listOf(
                PedalBinding("ArrowLeft", PedalAction.TOGGLE_PLAYBACK),
                PedalBinding("ArrowRight", PedalAction.TOGGLE_AUTOPLAY),
                PedalBinding("ArrowUp", PedalAction.PLAY_PAUSE_AUDIO),
                PedalBinding("ArrowDown", PedalAction.IGNORE),
            ),
        )
        assertEquals(PedalAction.TOGGLE_PLAYBACK, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertEquals(PedalAction.TOGGLE_AUTOPLAY, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertEquals(PedalAction.PLAY_PAUSE_AUDIO, mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_DPAD_DOWN, settings))
    }

    @Test
    fun pedalSizeLimitsActiveBindingsButKeepsLegacyTwoButtonBehavior() {
        val settings = PerformanceSettings(pedalButtonCount = 2)
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
    }

    @Test
    fun reducingPedalSizePreservesHiddenBindings() {
        val sixButton = PerformanceSettings(pedalButtonCount = 6)
            .withPedalBinding(5, PedalBinding("PageDown", PedalAction.STOP_AUDIO))
        val reduced = sixButton.copy(pedalButtonCount = 2)

        assertEquals(6, reduced.pedalBindings.size)
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_PAGE_DOWN, reduced))
        assertEquals(
            PedalAction.STOP_AUDIO,
            mappedPedalAction(KeyEvent.KEYCODE_PAGE_DOWN, reduced.copy(pedalButtonCount = 6)),
        )
    }

    @Test
    fun performanceLayoutUsesEventThenSetListThenActiveAssignment() {
        fun template(id: String, materialRows: Int) = JSONObject()
            .put("id", id)
            .put("published", JSONObject().put("breakpoints", JSONObject().put("tablet", JSONObject()
                .put("items", JSONArray().put(JSONObject()
                    .put("id", "material").put("order", 11).put("span", 8).put("rows", materialRows).put("visible", true))))))
        val payload = JSONObject()
            .put("active", template("active", 8))
            .put("templates", JSONArray().put(template("set-layout", 10)).put(template("event-layout", 12)))
            .put("assignments", JSONArray()
                .put(JSONObject().put("target_type", "set_list").put("target_id", "set-1").put("layout_id", "set-layout"))
                .put(JSONObject().put("target_type", "event").put("target_id", "event-1").put("layout_id", "event-layout")))
        val settings = PerformanceSettings.fromJson(JSONObject().put("performance_layouts", payload).toString())

        assertEquals("event-layout", settings.performanceLayoutCatalog.resolve("event-1", "set-1").id)
        assertEquals("set-layout", settings.performanceLayoutCatalog.resolve("other-event", "set-1").id)
        assertEquals("active", settings.performanceLayoutCatalog.resolve("other-event", "other-set").id)
        assertEquals(12, settings.performanceLayoutCatalog.resolve("event-1", "set-1").item("tablet", "material")?.rows)
    }
}
