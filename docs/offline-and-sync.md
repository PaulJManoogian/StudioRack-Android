# Offline Work and Synchronization

## The core rule

The Studio Leviathan cloud workspace is canonical. Android maintains an operational local database so the application remains useful without internet. Synchronization reconciles the two; it is not a simple screen refresh.

## What synchronization does

In broad terms, Android:

1. sends queued local changes in creation order;
2. records accepted server revisions or preserves conflicts;
3. requests server changes after the last committed cursor;
4. applies those changes transactionally to the local database;
5. reconciles the attachment manifest;
6. downloads changed eligible files and removes stale local files;
7. schedules later background work according to Android/network settings.

Closing the screen does not normally discard a queued change. Do not clear application storage or uninstall while unsynchronized work exists.

## Connection states

- **Online** - a network is available.
- **Syncing** - the repository is exchanging changes.
- **Online - N changes queued** - the device has a network but local work remains pending.
- **Offline ready** - local synchronized data remains usable without the server.
- **Attention** - the most recent sync encountered an error.
- **System maintenance** - the service is temporarily unavailable; the local copy remains available.
- **Conflict** - a record cannot be merged automatically.

Online does not mean fully synchronized. Always read the queued count.

## Work that can be queued offline

Depending on the current Android workflow and permissions, offline operations can include songs and text material, set lists, scheduled events, maintenance notes/completions, directory changes, and other operational mutations.

Operations that require the server cannot complete offline, including secure invitation delivery, new/revoked share tokens, linked-provider authentication, server AI processing, billing, and account administration.

## Attachments

Attachment metadata and attachment bytes are separate.

- A song can appear before its PDF, image, or audio has finished downloading.
- A linked external URL remains online-only unless Studio Leviathan has a cached copy.
- Cached files are stored in app-private storage and validated against revision/checksum information.
- Managed performance audio also carries an account offline lease. A successful authorized sync renews the lease; the app must warn before expiration and must not present expired managed audio as performance-ready.
- Files connected from customer-controlled storage remain dependent on that provider until prepared locally. Never rely on live cloud streaming for multitrack performance.
- A file is replaced only after a complete authenticated download.

Open critical attachments before the show. A title in the list is not proof that the file bytes are ready.

## Resolve a conflict

A conflict means the device edited an older revision after the server had already changed.

1. Stop editing that record on other devices.
2. Open **More > Sync** and review the conflict.
3. Compare the device and server versions.
4. Choose the version that should survive, or manually recreate the intended combined result where offered.
5. Synchronize again.
6. Verify the record on the web and Android.

Do not repeatedly press Sync without deciding the conflict; that cannot determine user intent.

## Background synchronization

Android controls background execution based on network, power, and manufacturer policies. Studio Leviathan requests retryable work, but the operating system can delay it.

Use **Wi-Fi-only background synchronization** when mobile data should be protected. Manual synchronization can still use available internet. Before a performance, use a deliberate foreground Sync rather than relying only on background work.

## Safe sign-out and reinstall

Before signing out, clearing app data, changing devices, or uninstalling:

1. connect to the server;
2. synchronize;
3. confirm zero queued changes;
4. resolve conflicts;
5. confirm recent work in the web application;
6. confirm necessary source files exist outside app-private storage.

Uninstalling or clearing storage can destroy unsynchronized local records and cached attachments.

## Local Live versus cloud sync

Local Live allows nearby devices to coordinate a performance without internet. It does not make every participating device canonical. The host keeps authoritative local changes and later synchronizes them to the cloud. Sync the host first after the show.
