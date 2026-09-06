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
- Offline set-list creation and editing, including named sets, ordered songs, manual entries, per-entry notes, performance attachment choices, and deletion.
- Durable queued mutations with explicit server/device conflict resolution.
- StudioRack palette and typography throughout, with a phone/tablet Gig Mode presentation aligned to the web experience.
- Song-length metadata, set-list duration estimates, and compact configurable Gig Mode clock, elapsed, and set-remaining displays.
- Authenticated CSV, XLS, JSON, and XML exchange for songs, complete set lists, items, and kits through Android's document picker.

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

Version 0.9.6 adds contextual exports for visible songs, favorite and filtered set lists, individual set lists, and filtered scheduled items. Import remains centralized under Reports, while exports preserve the order of the current view and can be saved to any document provider installed on Android. This release also includes song lengths, set-list duration estimates, and compact live timing in Gig Mode.

Download the current Android alpha directly from the StudioRack server:

https://www.manoogianmedia.com/studiorack/StudioRack-alpha.apk

Android may require the browser or Files app to be allowed to install unknown apps. The initial sign-in and synchronization require an internet connection; synchronized content remains available offline afterward.

The sync protocol is documented in the StudioRack web repository at `drumdb/docs/mobile-sync-api.md`.
