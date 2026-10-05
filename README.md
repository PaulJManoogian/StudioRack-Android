# Studio Leviathan Android

Native offline-first Android client for Studio Leviathan.

Complete user documentation begins at [`docs/index.md`](docs/index.md). It covers installation, sign-in, every Android navigation area, calendar and event delivery, Leviathan Live, multitrack routing, offline synchronization, membership and sharing, Wear OS, troubleshooting, and product terminology.

User-facing product and assistant identity is isolated behind Android string resources. See `docs/branding-firewall.md`. Compatibility-sensitive package, database, API, notification, and synchronization identifiers intentionally retain their established names.

## Current foundation

- Kotlin and Jetpack Compose interface.
- Room-backed account cache used as the UI source of truth.
- MFA device sign-in against the Studio Leviathan `/api/v1` service.
- Device bearer token encrypted with Android Keystore AES-GCM.
- WorkManager synchronization when network connectivity returns.
- Full snapshot and incremental cursor processing.
- Queued mutation, conflict, revision, and tombstone storage.
- Upcoming sessions dashboard.
- Offline Leviathan Live list for synchronized event set lists.
- Authenticated, checksum-backed offline attachment cache.
- Native full-screen image and paged PDF viewing in Leviathan Live.
- Per-event offline packet readiness on the sessions dashboard.
- Native metronome with tempo, downbeat, mute, auto-start, and four sound profiles.
- Configurable four-pedal Bluetooth keyboard control matching web Leviathan Live settings.
- Offline dashboard with studio totals, schedule, maintenance, and Crew activity.
- Full Month and Agenda scheduling with multi-day/all-day events, Sunday/Monday week starts, color, importance, private designation, participant communication, reminders, and ICS sharing.
- Searchable equipment and kit browsing with synchronized details and membership.
- Searchable song and set-list library.
- Offline reports, Crew skills/history, reference data, and account details.
- Offline-first song and scheduled-event creation, editing, and deletion.
- Offline set-list creation and editing, including named sets, ordered songs, manual entries, per-entry notes, performance attachment choices, and deletion.
- Durable queued mutations with explicit server/device conflict resolution.
- Studio Leviathan palette and typography throughout, with a phone/tablet Leviathan Live presentation aligned to the web experience.
- Song-length metadata, set-list duration estimates, and compact configurable Leviathan Live clock, elapsed, and set-remaining displays.
- Authenticated CSV, XLS, JSON, and XML exchange for songs, complete set lists, items, and kits through Android's document picker.
- Native Android maintenance, session, and Crew notifications with local unread tracking, app-icon counts, notification deep links, and scheduled event alarms.
- Workspace member invitations and role management with licensed-seat awareness.
- Joined-band material, role-based material access, and member-specific overlays.
- Offline Sharing with separate Shared With Me and My Shares views, including outgoing recipient, delivery, permissions, status, expiration, and last-access details. When connected, owners can edit access, copy or email guest links again, and revoke shares directly from Android.
- Multitrack playback with virtual buses, detected audio devices, mono/stereo routing, per-route mute, reusable interface profiles, and performance guidance.
- Wear OS companion state and controls for active Leviathan Live performances.
- Alpha/beta tester feedback available only to eligible tester tiers.

## Build

Create `local.properties` with the Android SDK path, then run:

```text
./gradlew assembleDebug test
```

Google Play release preparation, listing copy, store assets, and the local signed-bundle process are documented in `play-store/README.md` and `play-store/release-checklist.md`. Android releases are built locally; this repository intentionally does not use a GitHub Android build workflow.

The production API root is configured as:

```text
https://www.manoogianmedia.com/leviathan/api/v1
```

## Closed beta distribution

The current phone/tablet closed-beta release is `0.28.0 (117)`. The Wear OS companion release is `0.22.3 (78002)`. Both use the same Play application ID and upload identity, with separate phone/tablet and Wear OS testing tracks.

Upload-ready local bundles, store artwork, listing copy, screenshots, Data safety working notes, reviewer instructions, hashes, and the complete Console procedure are in [`play-store/`](play-store/README.md). Private signing material and generated bundles are intentionally excluded from Git.

Closed testers install from the Google Play opt-in link supplied by the Studio Leviathan team. Initial sign-in and synchronization require internet access; synchronized operational records and downloaded material remain available offline according to account permissions.

Local debug builds remain available for development and device testing, but debug-signed APKs must never be uploaded to Play Console.

The sync protocol is documented in the web repository at `drumdb/docs/mobile-sync-api.md`.
