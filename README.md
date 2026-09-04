# StudioRack Android

Native offline-first Android client for StudioRack.

## Current foundation

- Kotlin and Jetpack Compose interface.
- Room-backed account cache used as the UI source of truth.
- MFA device sign-in against the StudioRack `/api/v1` service.
- Device bearer token encrypted with Android Keystore AES-GCM.
- WorkManager synchronization when network connectivity returns.
- Full snapshot and incremental cursor processing.
- Queued mutation, conflict, revision, and tombstone storage.
- Upcoming sessions dashboard.
- Offline Gig Mode list for synchronized event set lists.
- Authenticated, checksum-backed offline attachment cache.
- Native full-screen image and paged PDF viewing in Gig Mode.
- Per-event offline packet readiness on the sessions dashboard.
- Native metronome with tempo, downbeat, mute, auto-start, and four sound profiles.
- Configurable four-pedal Bluetooth keyboard control matching web Gig Mode settings.

## Build

Create `local.properties` with the Android SDK path, then run:

```text
./gradlew assembleDebug test
```

The production API root is configured as:

```text
https://www.manoogianmedia.com/studiorack/api/v1
```

The sync protocol is documented in the StudioRack web repository at `drumdb/docs/mobile-sync-api.md`.
