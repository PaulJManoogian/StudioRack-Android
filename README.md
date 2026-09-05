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
- Offline dashboard with studio totals, schedule, maintenance, and Studio Buddy activity.
- Searchable equipment and kit browsing with synchronized details and membership.
- Searchable song and set-list library.
- Offline reports, Studio Buddy skills/history, reference data, and account details.
- Offline-first song and scheduled-event creation, editing, and deletion.
- Durable queued mutations with explicit server/device conflict resolution.
- Bundled Inter typography and StudioRack-branded navigation.

## Build

Create `local.properties` with the Android SDK path, then run:

```text
./gradlew assembleDebug test
```

The production API root is configured as:

```text
https://www.manoogianmedia.com/studiorack/api/v1
```

## Alpha install

Download the current Android alpha directly from the StudioRack server:

https://www.manoogianmedia.com/studiorack/StudioRack-alpha.apk

Android may require the browser or Files app to be allowed to install unknown apps. The initial sign-in and synchronization require an internet connection; synchronized content remains available offline afterward.

The sync protocol is documented in the StudioRack web repository at `drumdb/docs/mobile-sync-api.md`.
