# Theme System

Studio Leviathan Android follows the canonical cross-platform theme contract maintained with the Web application.

The planned initial choices are **Leviathan Dark**, **Stage Black**, **Studio Light**, **Graphite**, and **Follow Device**. Theme choice is a synchronized personal preference cached for offline startup. It is not tenant branding and does not change navigation, control placement, touch-target size, responsive behavior, or feature access.

Android components consume semantic theme roles for surfaces, text, actions, status, switches, focus, charts, and audio meters. Hard-coded product colors must move behind those roles as theme support is implemented. Mute, Solo, transport, destructive actions, synchronization, and meter ranges remain identifiable by label/icon and state rather than color alone.

Changing theme must not recreate or interrupt the native audio engine, metronome, chart state, Local Live session, page-turner handling, or song navigation. The temporary performance-material appearance control remains separate from the saved application theme, and Leviathan Live may use a persistent Stage Black override.

Web, Android, and future iOS clients share theme identifiers, preference and fallback behavior, accessibility requirements, and Live override semantics. Platform-native rendering may differ without changing meaning.
