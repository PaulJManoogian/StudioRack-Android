# Studio Leviathan Android Architecture

This document describes the implementation boundary behind the user workflows documented in [`index.md`](index.md). Compatibility-sensitive internal identifiers can retain the historical `studiorack` name; the public product identity is Studio Leviathan.

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
- `data/SyncClient.kt`: JSON HTTPS client for the versioned Studio Leviathan mobile API.
- `data/StudioRackRepository.kt`: push/pull orchestration and transactional cache updates.
- `data/SyncWorker.kt`: retryable connectivity-aware background work.
- `data/LocalLiveCoordinator.kt`: host/join discovery, local authorization, and host-authoritative performance exchange.
- `data/LocalExchangeExporter.kt`: contextual Android exports for supported record families and formats.
- `data/StudioRackNotifications.kt`: Android notification channels, local receipts, badges, and deep links.
- `performance/MultichannelPcmEngine.kt`: local multitrack playback and virtual-bus routing.
- `performance/MidiOutputRouter.kt`: Android MIDI destination discovery and controlled message delivery.
- `wear/LiveWearBridge.kt`: state and command bridge between Leviathan Live and Wear OS.
- `ui/StudioRackViewModel.kt`: observable application state.
- `ui/StudioRackUi.kt`: authentication, main navigation, calendar, library, sharing, reports, settings, and Leviathan Live composition.
- `ui/SetListEditor.kt`: compact set-list creation, reordering, attachment selection, manual entries, and deletion gestures.
- `ui/HelpContent.kt`: user-visible quick reference kept aligned with the detailed guides.

## Offline attachments

Synchronized attachment metadata is stored with the rest of the account data. A separate Room manifest records the local path, server revision, MIME type, byte count, SHA-256 checksum, and cache status for each file. Files are written to app-private storage through a temporary file and renamed only after a complete authenticated download. Removed attachment records also remove their local files.

External media URLs remain marked as online-only. StudioRack-hosted PDF and image attachments are cached automatically after each successful synchronization. The dashboard compares each event's chosen or default performance attachments with the manifest and reports whether its offline packet is ready. Gig Mode reads only local files and uses Android `PdfRenderer` for paged PDFs.

## Native performance controls

The mobile sync response includes the account's existing Gig Mode settings. The Android activity captures directional key events only while enabled Gig Mode pedal support is active, then sends those events to Compose. The configured previous, next, metronome, and mute assignments therefore work with Bluetooth pedals that present themselves as keyboards, including the Donner DBM-50 and AirTurn BT500 S-4 modes that emit arrow keys.

The native metronome synthesizes its click through `AudioTrack`, so it remains available without a network connection. Tempo and time signature come from the active song. Tempo-only and emphasized-downbeat modes, mute with continued visual pulse, auto-start, and tone, clave, woodblock, and cowbell profiles mirror the web controls.

Leviathan Live uses responsive phone/tablet composition plus a synchronized published layout definition. The Android renderer applies minimum usable widths, ordering, visibility, and row limits rather than trusting arbitrary pixel coordinates. Timed song sections and synchronized lyrics use the same canonical records as the web application.

Multitrack playback separates stems, virtual buses, and hardware routing profiles. A route stores output start channel, mono/stereo width, and mute state. The editor constrains output choices to the configured device capacity. Hardware detection reflects devices and channel counts exposed by Android; it cannot bypass an interface driver's stereo-only presentation.

## Security boundary

The app receives a device token only after approved account verification through access-code/MFA, passkey, or an already linked provider path. The token is encrypted at rest and scoped by the server to one registration and one account. Crew provider settings, platform API credentials, administrator records, billing operations, access approvals, and other tenants are outside the mobile protocol.

The Android app never becomes the canonical authorization source. Workspace role, module access, membership, material scopes, and record revisions are server decisions. Local UI checks improve clarity and offline behavior but do not replace API enforcement.

## People, places, and sessions

Venues, contacts, bands/groups, and their many-to-many relationship rows use the same generic Room record cache and queued-mutation pipeline as songs and events. They can be created and edited offline from the People directory. Event saves atomically queue the event, its selected groups, and its selected individual contacts.

The event stores a reusable `venue_id` separately from room, stage, entrance, or one-off location details. Calendar fields (`end_date`, `end_time`, `all_day`, `calendar_color`, `importance`, `is_private`, `calendar_uid`, and `calendar_revision`) remain in the canonical `studio_event` record and travel through the same queued sync contract as the web application. Schedule cards, the month calendar, calendar-file exports, reminders, and Gig Mode all read the same cached event. Access-grant tokens remain server-only: creating or revoking a temporary share requires connectivity so the server can mint, hash, scope, expire, and audit the token.

Calendar recipient resolution combines direct event contacts, event groups and their active members, and venue contacts. Android launches user-visible email, text, map, and share applications; it does not silently send from a device identity. Server-managed reminder delivery remains separate from generated `.ics` files.

## Membership and shared material

Workspace membership, band/group membership, and directory contacts are separate many-to-many domains. Android receives only the relationships allowed for the signed-in membership. Material access combines group defaults, performance role, member-level exceptions, object scope, and active membership. Member overlays remain separate from canonical shared material.

Guest and registered shares remain server-audited. Android can display synchronized outgoing and incoming share state, but creation/revocation of access tokens requires connectivity.

## Wear OS

The Wear module uses the same application ID and upload identity as mobile but has its own high version-code sequence and a dedicated Play form-factor track. It is declared non-standalone. The phone/tablet publishes current set/song/metronome state and receives acknowledged commands. Cached state supports brief disconnects without promoting the watch to performance authority.

## Release and signing

- Mobile targets API 36; Wear OS targets API 35 for the current closed-beta release.
- Both release bundles use one protected Studio Leviathan upload key.
- Private signing material and generated upload bundles are ignored by Git.
- The public certificate, artifact hashes, Play listing, reviewer template, Data safety working notes, artwork, and screenshots live under `play-store/`.
- Android release builds are local. This repository intentionally has no GitHub Android build workflow.
- See [`../play-store/release-checklist.md`](../play-store/release-checklist.md) for the account-side Play Console procedure.

## Current product guarantees

1. The web application remains canonical and contains platform administration.
2. Android remains useful with previously synchronized records when the internet is unavailable.
3. Queued mutations and conflicts are preserved rather than silently discarded.
4. Admin-only records and internal provider/database details are excluded from the mobile protocol.
5. Phone, tablet, web, and Wear parity is evaluated by workflow and shared data contracts, while controls remain appropriate to each form factor.
6. User-facing feature work must update the in-app help, detailed guide, and relevant release documentation in the same change.
