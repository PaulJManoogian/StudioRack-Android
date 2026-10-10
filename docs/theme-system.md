# Theme System

Studio Leviathan Android follows the canonical cross-platform theme contract maintained with the Web application.

The available choices are **Leviathan Dark**, **Stage Black**, **Studio Light**, **Graphite**, and **Follow Device**. Theme choice is a synchronized personal preference cached in the offline library. It is not tenant branding and does not change navigation, control placement, touch-target size, responsive behavior, or feature access.

Android components consume semantic theme roles for surfaces, text, actions, status, switches, focus, charts, and audio meters. Hard-coded product colors must move behind those roles as theme support is implemented. Mute, Solo, transport, destructive actions, synchronization, and meter ranges remain identifiable by label/icon and state rather than color alone.

Changing theme must not recreate or interrupt the native audio engine, metronome, chart state, Local Live session, page-turner handling, or song navigation. The temporary performance-material appearance control remains separate from the saved application theme, and Leviathan Live may use a persistent Stage Black override.

Web, Android, and future iOS clients share theme identifiers, preference and fallback behavior, accessibility requirements, and Live override semantics. Platform-native rendering may differ without changing meaning.

## Change Appearance

1. Open **More > Settings**.
2. In **Appearance**, choose an application theme.
3. Choose whether Leviathan Live uses that application theme or always uses Stage Black.
4. Tap **Save Appearance**. The Android interface changes from the locally cached preference and synchronizes the choice when connected.

**Follow Device** uses Android's current light or dark setting. **Always use Stage Black** applies only while a Leviathan Live performance is open. It does not replace the temporary sun/moon control, which continues to change the presentation of performance material on the current device.

Theme changes recompose visual surfaces only. They do not rebuild the audio engine, reload a chart, restart the metronome, clear mixer values, disconnect Local Live, or reset the selected song.
