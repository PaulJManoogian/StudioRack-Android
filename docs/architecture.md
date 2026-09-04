# StudioRack Android Architecture

## Runtime flow

The Android app is offline-first. Compose screens observe Room and never depend directly on a live HTTP response. After device sign-in, the repository performs this sequence:

1. Push locally queued mutations in creation order.
2. Record applied server revisions or preserve conflicts for user review.
3. Pull changes after the last committed account cursor.
4. Apply each page to Room.
5. Continue until the server reports `has_more: false`.

WorkManager repeats this process when a network is available. A manual Sync Now action runs the same repository path.

## Packages

- `data/LocalData.kt`: Room entities, DAO, database, mutation queue, and conflict storage.
- `data/TokenStore.kt`: Android Keystore AES-GCM protection for the bearer token.
- `data/SyncClient.kt`: small JSON HTTPS client for the versioned StudioRack API.
- `data/StudioRackRepository.kt`: push/pull orchestration and transactional cache updates.
- `data/SyncWorker.kt`: retryable connectivity-aware background work.
- `ui/StudioRackViewModel.kt`: observable application state.
- `ui/StudioRackUi.kt`: device sign-in, upcoming sessions, and offline Gig Mode.

## Security boundary

The app receives a device token only after email, StudioRack access code, and authenticator code verification. The token is encrypted at rest and is scoped by the server to one registration and one account. Studio Buddy provider settings, API credentials, administrator records, and other tenants are outside the mobile protocol.

## Next functional layers

1. Download manifests and checksum-backed storage for offline charts, lyrics, sheet music, and tabs.
2. Full-screen PDF/image chart mode using Android `PdfRenderer` and bitmap paging.
3. Bluetooth page-turner key mapping and the existing StudioRack metronome behavior.
4. Offline song, set-list, and schedule editors backed by the mutation queue.
5. Conflict resolution UI, especially for concurrent set-list reordering.
6. Signed release builds, tablet/phone visual QA, and Play Store internal testing.

