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

## Native performance controls

The mobile sync response includes the account's existing Gig Mode settings. The Android activity captures directional key events only while enabled Gig Mode pedal support is active, then sends those events to Compose. The configured previous, next, metronome, and mute assignments therefore work with Bluetooth pedals that present themselves as keyboards, including the Donner DBM-50 and AirTurn BT500 S-4 modes that emit arrow keys.

The native metronome synthesizes its click through `AudioTrack`, so it remains available without a network connection. Tempo and time signature come from the active song. Tempo-only and emphasized-downbeat modes, mute with continued visual pulse, auto-start, and tone, clave, woodblock, and cowbell profiles mirror the web controls.

## Security boundary

The app receives a device token only after email, StudioRack access code, and authenticator code verification. The token is encrypted at rest and is scoped by the server to one registration and one account. Studio Buddy provider settings, API credentials, administrator records, and other tenants are outside the mobile protocol.

## People, places, and sessions

Venues, contacts, bands/groups, and their many-to-many relationship rows use the same generic Room record cache and queued-mutation pipeline as songs and events. They can be created and edited offline from the People directory. Event saves atomically queue the event, its selected groups, and its selected individual contacts.

The event stores a reusable `venue_id` separately from room, stage, entrance, or one-off location details. Schedule cards and Gig Mode resolve the venue name from the local cache. Access-grant tokens remain server-only: creating or revoking a temporary share requires connectivity so the server can mint, hash, scope, expire, and audit the token.

## Next functional layers

1. Online mobile controls for creating and revoking expiring object-share links.
2. Role and primary-contact editing for venue and band relationships.
3. Selective download controls and storage-budget management for very large libraries.
4. Signed release builds, tablet/phone visual QA, and Play Store internal testing.
