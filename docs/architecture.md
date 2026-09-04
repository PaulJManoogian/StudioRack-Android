# StudioRack Android Architecture

## Runtime flow

The Android app is offline-first. Compose screens observe Room and never depend directly on a live HTTP response. After device sign-in, the repository performs this sequence:

1. Push locally queued mutations in creation order.
2. Record applied server revisions or preserve conflicts for user review.
3. Pull changes after the last committed account cursor.
4. Apply each page to Room.
5. Continue until the server reports `has_more: false`.
6. Reconcile the attachment manifest, download changed local files atomically, and remove stale files.

WorkManager repeats this process when a network is available. A manual Sync Now action runs the same repository path.

## Packages

- `data/LocalData.kt`: Room entities, DAO, database, mutation queue, and conflict storage.
- `data/TokenStore.kt`: Android Keystore AES-GCM protection for the bearer token.
- `data/SyncClient.kt`: small JSON HTTPS client for the versioned StudioRack API.
- `data/StudioRackRepository.kt`: push/pull orchestration and transactional cache updates.
- `data/SyncWorker.kt`: retryable connectivity-aware background work.
- `ui/StudioRackViewModel.kt`: observable application state.
- `ui/StudioRackUi.kt`: device sign-in, upcoming sessions, and offline Gig Mode.

## Offline attachments

Synchronized attachment metadata is stored with the rest of the account data. A separate Room manifest records the local path, server revision, MIME type, byte count, SHA-256 checksum, and cache status for each file. Files are written to app-private storage through a temporary file and renamed only after a complete authenticated download. Removed attachment records also remove their local files.

External media URLs remain marked as online-only. StudioRack-hosted PDF and image attachments are cached automatically after each successful synchronization. The dashboard compares each event's chosen or default performance attachments with the manifest and reports whether its offline packet is ready. Gig Mode reads only local files and uses Android `PdfRenderer` for paged PDFs.

## Security boundary

The app receives a device token only after email, StudioRack access code, and authenticator code verification. The token is encrypted at rest and is scoped by the server to one registration and one account. Studio Buddy provider settings, API credentials, administrator records, and other tenants are outside the mobile protocol.

## Next functional layers

1. Bluetooth page-turner key mapping and the existing StudioRack metronome behavior.
2. Offline song, set-list, and schedule editors backed by the mutation queue.
3. Conflict resolution UI, especially for concurrent set-list reordering.
4. Selective download controls and storage-budget management for very large libraries.
5. Signed release builds, tablet/phone visual QA, and Play Store internal testing.
