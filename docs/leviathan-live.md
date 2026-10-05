# Leviathan Live

Leviathan Live is the stage-focused set-list and performance-material environment. It is designed for large touch targets, fast song movement, offline material, timing, playback, hardware control, and coordinated devices.

## Prepare a show

Do this before arriving at the stage:

1. Confirm the event uses the intended set list.
2. Review the song order, named sets, medleys, manual entries, and per-entry notes.
3. Confirm every song has the correct key, tempo, time signature, duration, starts cue, patch, and preferred material.
4. Select the intended attachment for set-list entries where several charts or lyric versions exist.
5. Synchronize the Android device.
6. Open every critical attachment and playback file.
7. Test audio routing with the actual interface and cabling.
8. Test the Bluetooth pedal, MIDI output, metronome, and Wear companion.
9. Rehearse Local Live when multiple devices will participate.
10. Connect device power and disable unrelated interruptions according to venue policy.

## Open Leviathan Live

From an event, choose its Leviathan Live action. The event supplies the set list, location, date/time, and related performance context.

Leviathan Live provides two primary views:

- **List view** - a compact, scannable running order optimized for selecting and reviewing songs.
- **Chart view** - the current-song performance screen with material, song facts, timing, playback, markers, and controls.

The list/chart control is a single toggle. Its icon indicates the view you will open next.

## Header controls

Depending on width and configuration, the header can provide:

- back to Schedule;
- LIVE/local connection status;
- Local Live hosting or joining;
- edit the active set list;
- toggle List/Chart view;
- phone/standard format override on larger devices;
- light/night document presentation;
- playback enablement and autoplay;
- metronome and metronome mute;
- live hardware and audio routing.

On a phone, controls become a horizontally scrollable tool row and current-song information is moved earlier so the performer does not lose it below a large empty performance area. On a narrow screen, phone format is automatic.

## Current and next song

The current song uses the stronger amber/orange emphasis. The next-song information is intentionally quieter. Previous and next buttons change the selected song. Manual entries remain in the running order but may not have full song metadata or performance material.

The displayed position reflects the complete performance order. Songs inside a named medley or tribute group preserve their grouping and internal position.

## Performance material

Chart view can display locally available:

- rendered lyrics and ChordPro;
- synchronized lyric lines;
- PDFs with page navigation;
- images;
- linked or cached charts, tabs, and sheet music;
- supported control and playback assets.

### Night view

The sun/moon button changes document presentation for stage lighting. It is a button rather than a slider. Use it when a white PDF or image is too bright. The result depends on material type; transformed document color is a viewing aid and does not alter the source file.

### Phone/standard format

The phone/window icon switches the layout density on a larger device. A genuinely narrow phone uses phone format automatically. This setting changes arrangement, not the underlying set list or selected material.

## Timed song guide and section markers

When a song contains timed sections, Chart view shows:

- current playback time;
- expected song duration;
- a progress control;
- compact play/pause and stop controls;
- color-coded section strips with section names and start times.

Selecting a section requests its time position and scrolls matching material toward the start of that section. The scroll destination leaves useful context near the top instead of placing the heading under fixed controls.

Timed sections can come from canonical Song Sections and timed ChordPro directives. Keep section names and times consistent so web and Android render the same structure.

## Metronome and count-in

The native metronome runs locally through Android audio. It does not need internet.

Settings can include:

- start automatically for a song;
- continue visual pulse while muted;
- emphasize the first beat;
- choose a sound profile;
- apply the song tempo and time signature;
- use a configured count-in before the song timeline begins.

The metronome icon represents the metronome directly. The mute control also uses metronome-specific visual language so it cannot be confused with general media volume.

## Playback and autoplay

Playback and autoplay are separate decisions:

- **Playback enabled** permits the selected song audio to play.
- **Autoplay armed** allows moving to a later song to start its selected audio automatically.

The first song always waits for a deliberate start. Autoplay does nothing while playback is disabled. A song with one eligible playback track can use it automatically; when several are available, select the intended one in the set list.

The playback clock drives timed sections, lyric timing, and cue alignment. Stop resets the current playback position.

## Multitrack playback

Multitrack songs can contain stems assigned to virtual buses. The bus model separates musical intent from specific hardware:

- a stem points to a virtual bus such as Main Mix, Click, Guide, or Backing;
- a routing profile maps each virtual bus to physical outputs on an audio device;
- mono/stereo is a single toggle-style control;
- each route has an independent MUTE button;
- the output selector only offers valid channels for the detected/configured hardware capacity.

### Detect hardware

Open Live hardware/audio routing and choose detection. Android reports compatible output devices visible to the platform. Some USB interfaces require an OTG-capable cable, device permission, external power, or a vendor-supported Android mode.

Detection reports what Android exposes. It cannot invent separate physical outputs when the Android driver presents only a stereo pair.

### Create a routing profile

1. Connect the intended audio interface.
2. Open the audio-routing profile area.
3. Choose **Use device** when detection identifies the hardware, or enter a descriptive profile name and confirmed output count.
4. Mark the profile preferred when it should win for compatible hardware.
5. For each bus, choose a valid output or output pair.
6. Toggle MONO/STEREO as required.
7. Mute any bus that should be silent in this profile.
8. Save the profile.

Removing a routing profile removes only that hardware mapping. It does not delete buses or audio files.

### Performance check

The suggested bus count is a planning aid, not a guarantee. Device throughput depends on sample rate, channel count, USB implementation, buffer behavior, concurrent processing, and other applications. Run the available performance check with the actual interface, files, and cabling, then rehearse for longer than the longest expected set.

## Bluetooth pedal controls

Android handles configured pedal keys locally while Leviathan Live is active. Supported actions can include:

- previous/next song;
- previous/next page;
- scroll up/down by the configured amount;
- toggle List/Chart view;
- playback and autoplay controls;
- play/pause or stop current audio;
- start/stop or mute the metronome.

Configure the canonical 2-, 4-, or 6-button mapping in the web application. Synchronize afterward. If a pedal moves keyboard focus instead of the performance, verify its operating mode and exact key codes.

## MIDI and control files

The live hardware area exposes supported MIDI destinations. A song can carry MIDI or DMX-MIDI control material. Test the complete receiving chain before performance; Android can report a connected destination while downstream hardware, channel, program, or cabling is still wrong.

## Local Live

Local Live coordinates devices on shared Wi-Fi when the internet is unavailable.

### Host

1. Open **Sessions > Local Live Network** or the Live header control.
2. Select the event and choose Host.
3. Share the temporary join code with participating devices.
4. Keep the host device connected and powered.

The host is authoritative for the local workspace and keeps changes queued for later cloud synchronization.

### Join

1. Connect to the same Wi-Fi network.
2. Open Local Live.
3. Enter the host's current join code.
4. Confirm the event and set list received from the host.

Local Live is not a public-internet service and is not a substitute for cloud synchronization. Synchronize the host after internet service returns.

## Performance layout templates

Performance layouts are created and previewed with the visual editor in the canonical web application. Layouts define responsive sections and relative sizing for phone, tablet, and desktop formats. Android consumes the published layout and applies device-safe constraints.

If a custom template produces an unusable view, switch back to a supplied template on the web, publish it, and synchronize Android. Do not attempt to repair a layout during a live song unless a fallback is already visible.

## End of show

1. Stop playback and metronome.
2. Return to the set list and note any changes needed.
3. Record equipment issues through Gear maintenance notes.
4. Allow Crew's post-performance check-in when enabled.
5. Synchronize the host and participating devices on a dependable network.
6. Resolve conflicts before editing the same records again elsewhere.
