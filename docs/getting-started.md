# Getting Started

## What you need

Before connecting an Android device, confirm that you have:

- an approved Studio Leviathan account or workspace invitation;
- the email address assigned to that membership;
- the Studio Leviathan access code supplied during activation;
- access to the configured authenticator or an already registered passkey;
- an internet connection for the first sign-in and synchronization;
- enough device storage for any charts, PDFs, images, lyrics, and audio needed offline.

If the workspace invitation has not been accepted or the seat has been revoked, the Android application cannot create a separate account. Account registration and approval are controlled by Studio Leviathan.

## Install the closed beta

1. Open the Google Play testing invitation supplied by the Studio Leviathan team.
2. Join the closed test with the Google account used by Play on the device.
3. Open the Play listing and install **Studio Leviathan**.
4. Allow notifications when prompted. Notifications are used for scheduled work, maintenance, Crew activity, and account alerts.
5. Grant contacts access only if you choose to import a person from the device directory. Studio Leviathan does not require continuous address-book access.

The Wear OS companion is installed separately after the phone/tablet application is working. See [Wear OS Companion](wear-os.md).

## Sign in

### Existing login

1. Enter the approved account email.
2. Enter the Studio Leviathan access code.
3. Enter the current authenticator code.
4. Select **Connect this device**.
5. Leave the app open until the first synchronization completes.

Email, access code, and authenticator verification identify an existing Studio Leviathan membership. They do not create a new workspace.

### Passkey

Select **Sign in with a passkey** when a passkey has already been registered for the account. Android may use a fingerprint, face, device PIN, pattern, or password depending on the device configuration.

A passkey is an additional sign-in method. It does not change the workspace, role, licensed seat, or account email. If no matching passkey exists, use the existing login and add a passkey later from **More > Settings > Sign-in & Security**.

### Google or Microsoft

Google and Microsoft accounts must first be linked while signed in through Studio Leviathan's secure account settings. An unlinked provider identity cannot enter or create a Studio Leviathan workspace. Open **More > Settings > Sign-in & Security > Manage Connected Accounts** to manage those links on the secure web page.

## Understand the top bar

The top bar remains visible throughout the main Android application.

- **ONLINE** means the device currently has network connectivity. It does not guarantee that every queued change has already reached the server.
- **OFFLINE READY** means synchronized records remain available locally even though the server cannot currently be reached.
- **SYNCING** means the device is exchanging queued and server-side changes.
- **CHANGES QUEUED** reports local work waiting to be uploaded.
- **ATTENTION** or a red error state means synchronization needs review.
- **CONFLICTS** means the same record changed on the device and server from incompatible revisions.
- The **alerts pill** opens the alert list. It is a button, not a passive count.
- Alpha and beta testers see a circular music-themed feedback button.

## Understand the main navigation

The six main areas are:

- **Home** - workspace overview, upcoming activity, care priorities, and recent Crew information.
- **Gear** - searchable equipment, specifications, units, maintenance notes, maintenance history, and exports.
- **Kits** - reusable equipment collections and their contents.
- **Sessions** - month/agenda scheduling, event actions, Local Live, and Leviathan Live settings.
- **Library** - songs, performance material, and set lists.
- **More** - reports, sharing, people, members, Crew, module information, help, sync, and settings.

On a tablet these destinations normally fit on one row. On a phone they are arranged as two rows of three large controls so that touch targets remain usable.

## Perform the first synchronization

1. Connect to a dependable network.
2. Open **More > Sync**.
3. Select **Synchronize**.
4. Wait until the top bar no longer says `SYNCING`.
5. Review queued changes and conflicts if either count is nonzero.
6. Open **Library**, **Sessions**, and any critical event to confirm the expected workspace content exists.

The first sync can take longer when the workspace contains many images, charts, audio files, or other attachments. Records can finish before every attachment is ready.

## Prepare for offline use

Before leaving dependable internet access:

1. Synchronize the device.
2. Open the scheduled event and confirm the correct set list, group, location, and people.
3. Open the set list in Leviathan Live.
4. Open each critical chart, lyric, PDF, image, or text attachment at least once.
5. Confirm playback files and stems required by the show are downloaded.
6. Test the metronome, Bluetooth pedal, MIDI destination, and audio interface.
7. If several devices will work together without internet, rehearse Local Live on the actual Wi-Fi equipment.
8. Keep a printed or otherwise independent fallback for a performance where failure would stop the show.

Continue with the [Android User Guide](android-user-guide.md).
