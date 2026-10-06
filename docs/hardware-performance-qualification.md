# Hardware Performance Qualification

Studio Leviathan separates Android installation compatibility from hardware approved for live performance. `minSdk` means the application can be installed on that Android version; it is not a promise that every device on that version can render large charts or run multitrack audio safely.

The canonical benchmark protocol lives with the Studio Leviathan server documentation. Android qualification must cover these native workflows:

- cold launch and opening an offline event;
- incremental and media-heavy synchronization;
- 500-song library and 100-song set-list navigation;
- ChordPro, high-resolution image, and 50-page PDF rendering;
- 1,000 Bluetooth page-turner commands;
- single-track, four-stem, eight-stem, and PCM WAV playback;
- gain, Mute, Solo, bus assignment, seek, pause, stop, and song switching during playback;
- built-in stereo output and a real multichannel USB interface;
- two-hour offline performance soak with battery and thermal observations;
- forced interruption, background/foreground return, low-storage handling, and interface disconnect recovery.

Every result records application version, Android build, manufacturer/model, SoC, RAM, free storage, display, battery state, thermal state, codec/sample rate, audio interface, output count, and the median, 95th-percentile, worst, and failure measurements.

Public support profiles are:

1. **Minimum for general use** - library, calendar, synchronization, editing, and ordinary charts.
2. **Minimum for Leviathan Live** - responsive chart mode, song switching, page turner, metronome, offline use, and the published baseline playback workload.
3. **Recommended for multitrack performance** - the published stem workload and two-hour soak with no dropout, crash, unintended stop, or sustained drift.

Stage-critical qualification requires three clean workflow runs and one clean two-hour soak. Synthetic processing checks remain advisory and cannot promote a device into a supported tier.

No Android hardware minimum is published until physical-device results populate the matrix. Test results must identify whether they used the native multichannel PCM engine or Media3 stereo fallback because those paths have different capabilities and performance characteristics.

## Initial Android Hardware

The first physical-device evidence comes from the product owner's real Teclast Android tablet and Samsung Galaxy S26 Ultra. Feedback from those devices is field evidence, not emulator or synthetic-test evidence. Both are initial qualification targets.

- **Teclast Android tablet:** active tablet testing covers synchronization, chart mode, performance controls, multitrack mixer, buses, routing profiles, and touch usability. The exact model, Android build, SoC, and RAM must be captured with the formal runs.
- **Samsung Galaxy S26 Ultra:** active phone testing covers native behavior and Web/Android parity. The Android build and repeatable workflow results must be captured with the formal runs.

Existing observations must not be discarded when the formal suite begins. Record them as **Field Observation**, then promote a device only after the applicable repeat runs and soak test pass.
