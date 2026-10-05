# Performance Mixer UI

The Android mixer follows the Studio Leviathan cross-platform mixer contract while retaining Android's native playback implementation.

## Non-Regression Boundary

- Mixer composables only display state and invoke the existing gain, Mute, Solo, routing, and transport callbacks.
- Do not move song navigation, Bluetooth page-turn commands, metronome timing, ChordPro parsing, section navigation, or multichannel playback into a mixer composable.
- Visual work may be throttled or dropped under load. It must never delay playback, page-turn input, or song changes.
- Signal meters must use measured engine data. Do not simulate activity from gain values or playback state.

## Responsive Contract

- At 600 dp and wider, each master, track, or bus is a compact horizontal strip with a color rail, identity, dB control, readout, and actions.
- Below 600 dp, identity and actions remain on the first row and the gain control receives a full-width second row.
- Mute uses orange, Solo and active routing use cyan, and inactive controls use the Studio Leviathan gray palette.
- Transport and previous/next song controls always have layout and input priority over mixer presentation.

## Release Checks

Run unit and application builds, then verify touch and Bluetooth previous/next actions, ChordPro timing, metronome operation, multitrack synchronization, Mute/Solo behavior, bus routing, and connected output selection on a tablet before release.
