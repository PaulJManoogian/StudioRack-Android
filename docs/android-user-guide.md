# Android User Guide

## Home

Home is an operational summary, not a substitute for detailed reports.

The workspace card can show the studio or organization identity, location, logo, estimated value, and most recent synchronization time. The metric strips summarize synchronized items, kits, events, Crew activity, and care records. Upcoming sessions and care priorities appear below when the local database contains them.

Use **Sync now** when you need an immediate exchange with the server. Automatic background synchronization still follows Android network and battery restrictions.

The Home values are calculated from records the current role and active modules are allowed to receive. A different count on two devices usually means one device has not synchronized, has a different account, or is operating under different module/role access.

## Gear

Open **Gear** to browse synchronized equipment.

### Find an item

Type or dictate a name, category, type, status, location, serial detail, or other synchronized text in **Find an item**. Clear the query to restore the full list.

Select an item strip to expand it. Depending on the record, the details can include:

- category and equipment type;
- default location and usage status;
- notes and category-specific specifications;
- quantity and individually tracked units;
- serial numbers and unit status;
- open field notes;
- completed maintenance history.

Full equipment creation and comprehensive field editing remain available in the canonical web application. Android focuses on browsing, field notes, care completion, and stage/studio operations.

### Add a maintenance note

1. Select **Add Maintenance Note**.
2. Search for the affected item.
3. Select the item strip.
4. Describe what needs attention.
5. Save.

The note enters the local change queue and is uploaded during synchronization. It can be created without internet after the item itself has been synchronized.

### Complete service

1. Expand the equipment item.
2. Select **Maintenance / History**.
3. Choose **Complete Service**.
4. Enter the completion date and work performed.
5. Optionally record who performed the work and a next service date.
6. Select the open reminders resolved by this service.
7. Save.

The completion becomes permanent history. A pending local completion is marked until the server confirms it.

### Export equipment

Search first if only a subset is needed, then select **Export visible equipment**. Choose the offered format and Android destination. The export represents the currently visible records, not necessarily the entire workspace.

## Kits

Kits are reusable collections such as drum sets, travel rigs, stage packages, recording bundles, or production packs.

Open **Kits**, search by name/location/notes, and expand a kit to see its assigned item types and quantities. Use **Export visible kits** after filtering when a portable copy is needed.

Detailed kit assembly and image management remain available on the web. Android uses synchronized kit membership in events, reports, and preparation workflows.

## Sessions

Sessions contains two primary modes:

- **Schedule** - month and agenda views, event creation/editing, participant communication, calendar files, Local Live, and event launch actions.
- **Leviathan Live** - performance settings such as material priority, clocks, pedals, Wear OS, and audio routing.

See [Calendar and Events](calendar-and-events.md) and [Leviathan Live](leviathan-live.md).

## Library

Library contains **Songs** and **Set Lists**.

### Browse songs

Use **Find a song**, then narrow the list with **All/Favorites** and **A-Z/Z-A**. Expand a song to see metadata and available performance material. Downloaded attachments can be opened directly on the device.

### Add or edit a song

1. Select **Add Song**, or expand an existing song and choose its edit action.
2. Enter title and the available musical metadata: artist, album, style, key, tempo, time signature, length, start cue, patch, notes, favorite status, and related links as applicable.
3. Add files or written material.
4. Save the song.

Files selected on Android are copied into app-controlled storage immediately and queued for upload. Do not delete the original source until synchronization is confirmed when it is the only other copy.

### Add lyrics and performance material

Material types include Chart, Lyrics, Tab, Sheet Music, MIDI, DMX-MIDI, and Other.

Available paths include:

- select a PDF, image, audio/control file, or other supported document;
- paste or write plain lyrics;
- write ChordPro using section and chord controls;
- search LRCLIB and review a matching recording;
- import plain, synchronized, or generated ChordPro text;
- ask Crew to identify a likely section structure while online.

Always review imported lyrics. Providers can contain the wrong recording, spelling, section boundaries, or timing.

### ChordPro timing

Use section directives such as `{start_of_verse}` and `{end_of_verse}` to identify structure. A Leviathan timing directive immediately before the section can assign a performance time, for example:

```text
{x_leviathan_time: 0:45}
{start_of_chorus: Chorus 1}
...
{end_of_chorus}
```

Saving timed ChordPro material creates or updates matching shared song sections. Leviathan Live uses those sections for the marker strip and timed navigation. Keep times increasing and within the expected song duration.

### Create a set list

1. Select **Set Lists**.
2. Select **Create Set List**.
3. Enter the name and optional band/group association.
4. Add one or more named sets.
5. Search and add songs to each set.
6. Add manual entries for announcements, breaks, introductions, or material not yet represented by a song.
7. Reorder entries with the drag handle.
8. Choose the desired performance attachment when a song has several.
9. Add per-entry notes where the performance requires them.
10. Save.

Compact strips keep long set lists manageable. Swipe an entry toward its delete action when removal is intended, then confirm where prompted. Grouping and medley information travels with the saved order.

Set-list actions can include edit, copy, rename, favorite, export, and deletion. Copy before making substantial show-specific changes to a reusable master list.

## More

More uses pill tabs for these areas:

### Reports

Review overview, equipment, maintenance, schedule, and controlled Crew/AI reports. Filters determine visible rows and therefore what contextual exports contain.

### Sharing

Review material shared with the account, outgoing guest/registered shares, and joined-band material. See [People, Membership, and Sharing](people-membership-sharing.md).

### People

Browse and maintain contacts, venues, and bands/groups. Android can import one deliberately selected device contact. It does not silently ingest the address book.

### Members

Workspace Owners and Account Managers can review licensed seats and invite or manage members according to role. Invitations require a connection.

### Crew

Review Crew behavior, activities, skills, and controlled assistance. AI-backed requests require connectivity to the main server; Android does not run private platform AI tasks locally.

### Module Data and Modules

Module Data shows synchronized categories, types, and choices. Modules shows the workspace focus and effective access. Owners can open billing in the secure web application. Inactive paid-module records remain retained server-side but are not delivered into operational Android views until access is restored.

### Help

Help provides the in-app quick reference. Use this documentation for complete procedures.

### Sync

Run synchronization, review connection behavior, queued changes, and conflicts. Enable Wi-Fi-only background synchronization when mobile-data usage matters; manual synchronization remains available when online.

### Settings

Settings includes account/security links, synchronization behavior, privacy, terms, and sign-out. Signing out removes the connected session from the device; confirm important local changes have synchronized first.

## Tester feedback

Alpha and beta testers see a circular feedback control in the top bar.

1. Select the feedback control.
2. Choose Bug, Improvement, Idea, or Question.
3. Choose the affected product area and impact.
4. Write a short summary and enough detail to reproduce or understand the request.
5. Choose whether the team may contact you.
6. Send.

Do not put passwords, access codes, authenticator secrets, private guest tokens, or sensitive client data into feedback.
