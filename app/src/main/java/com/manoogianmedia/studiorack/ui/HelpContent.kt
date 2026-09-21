package com.manoogianmedia.studiorack.ui

internal data class HelpTopic(
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val points: List<String> = emptyList(),
)

internal data class HelpSection(
    val id: String,
    val label: String,
    val eyebrow: String,
    val title: String,
    val introduction: String,
    val topics: List<HelpTopic>,
)

internal fun studioLeviathanHelpSections(
    productName: String,
    agentName: String,
    liveName: String,
): List<HelpSection> = listOf(
    HelpSection(
        "quick", "Quick Help", "QUICK HELP", "Get oriented quickly.",
        "Everyday instructions collected in one place for fast reference.",
        listOf(
            HelpTopic("First Steps", points = listOf(
                "Dashboard: See studio value, upcoming work, care priorities, recent $agentName activity, equipment flow, and curation totals.",
                "Create Item: Choose the category first. $productName changes the fields to fit the equipment.",
                "Create Kit: Group equipment into drum sets, travel rigs, stage packs, session bundles, or other reusable collections.",
            )),
            HelpTopic("Items", listOf("Items are the main equipment records. Record identity, asset and serial numbers, status, location, value, care details, photos, and files. Category-specific fields cover details such as drum heads, tuning, software licenses, cable length, and computer specifications.")),
            HelpTopic(agentName, listOf("$agentName watches care details, sends trackable reminders, reads replies, updates matching actions, and keeps an action history. Configure timing and quiet hours, test delivery, and teach enabled skills how your organization works.")),
            HelpTopic("Reports", listOf("Reports review equipment, value, care, scheduled work, and supporting notes. Filter first, then export the records currently in view.")),
        ),
    ),
    HelpSection(
        "equipment", "Equipment & Kits", "INVENTORY", "Equipment, assets, kits, and care.",
        "Build a useful record once, then use it for insurance, valuation, maintenance, packing, and performance preparation.",
        listOf(
            HelpTopic("Creating And Editing Items", points = listOf(
                "Open Create Item or select an item from Reports.",
                "Choose its category and equipment type so the appropriate fields appear.",
                "Record identity, asset code, serial number, purchase details, values, location, condition, and status.",
                "Add photos and supporting files that help identify, insure, or service the item.",
                "Use Save and Continue when entering several items.",
            )),
            HelpTopic("Value And Depreciation", points = listOf(
                "Purchase value records the original transaction amount and date.",
                "Replacement value records the insurance basis for comparable equipment.",
                "Estimated value is the depreciated or curated value used by the dashboard and Overview report.",
                "Curated, irreplaceable, vintage, and out-of-production equipment can be identified separately.",
            )),
            HelpTopic("Maintenance Records", listOf("Open an item's maintenance page to record completed service. Each completion becomes a permanent history entry. Completing maintenance records the date, work, performer, and notes, dismisses resolved reminders, and can establish the next due date.")),
            HelpTopic("Kits", listOf("Kits are reusable equipment collections. Search and assign items, set designation and location, and add a photo. Events may include multiple kits, and printed event packets include kit checklists.")),
        ),
    ),
    HelpSection(
        "music", "Songs & Live", "MUSIC LIBRARY", "Songs, performance material, set lists, and $liveName.",
        "Keep rehearsal sources and stage-critical information together, then make it available offline.",
        listOf(
            HelpTopic("Songs", listOf("Add or browse songs in Sessions. Song records support title, artist, album, style, key, tempo, time signature, length, who starts, patch, notes, favorite status, and listening links."), listOf(
                "GetSongBPM can search metadata while preserving fields you already populated.",
                "Where possible, LRCLIB duration is combined with metadata results.",
            )),
            HelpTopic("Performance Material", listOf("A song may contain multiple Charts, Lyrics, Tabs, Sheet Music, or other materials. Attach PDFs and images, link a source, paste plain text, or store ChordPro and synchronized lyrics directly."), listOf(
                "Choose the material type and a clear display name.",
                "Mark the preferred $liveName default, print inclusion, and priority.",
                "Search LRCLIB by title, artist, and album, then review imported text before saving.",
                "Expand a song in the Songs list to open any synchronized or linked performance material directly. Use Night view for rendered PDF and image pages when a bright document would be distracting on stage.",
                "Synchronize attachments before a performance so $liveName can read local copies without cellular service.",
            )),
            HelpTopic("Set Lists", listOf("Create sets, search and add songs, then drag to reorder. Rename, copy, print, share, favorite, or export a list. Named medley and tribute groups indent only their songs and show each song's position in Chart mode. Song lengths calculate estimated set time.")),
            HelpTopic(liveName, points = listOf(
                "List mode provides a large, scannable running order.",
                "Chart mode shows stage-critical song facts and the selected chart, lyrics, tab, or other material.",
                "Playback audio is independent of charts, lyrics, PDFs, ChordPro, and other performance material. Attached audio provides manual controls; automatic start requires an explicit track selection in the set list.",
                "Playback can be stopped or disabled during a live session. The playback clock is the foundation for timed lyrics, document movement, markers, MIDI, and lighting cues as those capabilities become available.",
                "Edit mode deliberately enables immediate add, edit, reorder, and swipe-delete changes.",
                "A paired Wear OS companion shows the current and next song, set and medley position, controls previous or next, and runs a synchronized haptic metronome while the phone or tablet remains the performance authority. Open Sessions > Leviathan Live > Settings > Wear OS to check the connection or install the companion on a connected watch.",
                "Local Host synchronizes participating devices on shared Wi-Fi when internet service disappears.",
                "Bluetooth page turners use only configured performance actions. Clock, elapsed time, and set time remaining can each be shown or hidden.",
            )),
        ),
    ),
    HelpSection(
        "schedule", "Schedule", "SESSIONS", "Performances, rehearsals, studio sessions, and other work.",
        "All scheduled work uses one searchable list, with Type distinguishing the event.",
        listOf(
            HelpTopic("Creating An Event", points = listOf(
                "Choose Performance, Rehearsal, Studio Session, or Other.",
                "Enter start and required end date/time, title, venue or location, directions, and notes.",
                "Attach a set list, select kits and gear, and associate groups or individual contacts.",
                "Configure the $agentName reminder and save. Copy an existing event when most details repeat.",
            )),
            HelpTopic("Finding And Using Events", listOf("Search and filter by event details, type, and status. Tap an event to review its session information, open directions in a map or navigation app, or open its set list for the performance. Event actions also provide $liveName, People, editing, copying, printing, sharing, and deletion. People can text or email direct participants, a selected band or group, venue contacts, everyone, or a custom selection, and still provides individual call, text, email, and profile actions.")),
            HelpTopic("Printed Event Packets", listOf("Packets include event and venue details, directions, people in readable columns, kit checklists, set and medley headings, song order and metadata, notes, and selected printable performance material.")),
        ),
    ),
    HelpSection(
        "people", "People & Sharing", "DIRECTORY", "Venues, contacts, groups, and controlled sharing.",
        "Normalize people and places once, connect them where needed, and expose only appropriate information.",
        listOf(
            HelpTopic("Venues", listOf("Store venue identity, photo, address, map link, phone, email, website, and private operational notes. Connect multiple contacts with roles such as booking agent, production manager, or client.")),
            HelpTopic("Contacts And Groups", listOf("Contacts support photos, organization, title, private notes, multiple labeled phone numbers and email addresses, and a preferred method. Android can import a selected device contact. Groups have a photo or logo and searchable membership; contact the group or open an individual profile.")),
            HelpTopic("Sharing", points = listOf(
                "Guest Link creates temporary access for someone without an account and can be copied or emailed again.",
                "Registered Share appears under Sharing for a matching account and supports the granted list or live experience.",
                "Choose recipient, role, expiration, and scopes independently. Private directory and venue notes are not exposed.",
                "Owners can edit an active share, control whether a copy may be kept, or revoke access immediately.",
            )),
        ),
    ),
    HelpSection(
        "crew", "Crew", "AGENTIC ASSISTANCE", "$agentName learns how your studio needs to work.",
        "Use skills, reminders, replies, and controlled AI actions as an operational assistant rather than a simple alarm clock.",
        listOf(
            HelpTopic("Settings And Reminders", listOf("Enable $agentName, name the assistant, and configure look-ahead, send time, timezone, notification windows, quiet hours, nights, weekends, and holidays. Web and Android notifications arrive after synchronization. Android can restrict scheduled background notification synchronization to Wi-Fi or another unmetered network while keeping manual synchronization available on mobile data. Reply naturally to complete, postpone, clarify, or teach an action.")),
            HelpTopic("Skills", listOf("Enable supplied or account-created responsibilities and add natural-language instructions. Fill Missing Song Lengths searches only songs without a duration and never overwrites user-entered values. Other skills monitor care, depreciation, upcoming work, and patterns learned through responses.")),
            HelpTopic("Ask Crew", listOf("Open Crew > Ask Crew to find songs or equipment with an ordinary question. Songs also include a contextual question box near Browse Songs. Try requests such as \"Show favorite rock songs in E\" or \"Find active microphones at the rehearsal space.\"", "Crew converts the question into approved read-only filters. It cannot run arbitrary database commands or change records. Results remain limited to your workspace, your permissions, and currently available modules, and recent questions can be reused.")),
            HelpTopic("Post-Performance Check-In", listOf("After a newly scheduled performance is complete, Crew can check in after a configurable delay. Reply naturally with anything damaged or missing, maintenance that may be needed, or a follow-up that should not be forgotten.", "Crew Behavior can turn this workflow off, set its delay and unanswered closeout period, or allow one additional follow-up. Turning Crew off stops new work and dismisses queued work; replies to earlier Crew messages are retained but are not interpreted or allowed to change records. Crew separates reported equipment issues, proposes matches from the workspace's equipment and kits, and asks for confirmation. Maintenance records are created only after those matches are confirmed; ambiguous equipment is never guessed.")),
            HelpTopic("Actions And History", listOf("The dashboard shows recent activity. Action History retains priority, subject, status, recipient, due time, delivery, replies, and drafts. Clearing a notification does not delete its action history.")),
        ),
    ),
    HelpSection(
        "reports", "Reports & Data", "REPORTS AND PORTABLE DATA", "Inspect, filter, ask, export, and import.",
        "Reports use synchronized account data, and exports stay beside the list they describe.",
        listOf(
            HelpTopic("Report Areas", points = listOf(
                "Items and Kits provide complete lists and record actions.",
                "Equipment filters category, type, status, location, assignment, maintenance, curation, age, and value.",
                "Maintenance and Schedule provide targeted operational filters.",
                "AI Report turns natural-language questions into controlled criteria and exportable rows while online.",
                "Overview summarizes totals, current studio value, care, assignments, categories, and activity.",
            )),
            HelpTopic("Imports And Exports", listOf("Supported formats include CSV, XLS, JSON, and XML where appropriate. Imports merge by meaningful identities and report unresolved rows. Exports omit internal record_json and are initiated from the corresponding section. Set-list exports include sets, medleys, song order, and metadata, but not chart attachments. Android can create equivalent exports offline.")),
        ),
    ),
    HelpSection(
        "mobile", "Mobile & Offline", "ANDROID", "Work online, synchronized, or fully offline.",
        "The Android application keeps an operational local database rather than acting as a thin web viewer.",
        listOf(
            HelpTopic("Connection States", points = listOf(
                "Online: Connected and exchanging changes with the canonical web database.",
                "Offline ready: Previously synchronized records and attachments remain available locally.",
                "Attention: A sync error or conflicting revision needs review.",
            )),
            HelpTopic("Offline Changes", listOf("Create and edit equipment, kits, songs, performance material, set lists, events, contacts, groups, venues, shares, and maintenance notes locally. Changes enter a queue and push when connectivity returns. Dictation appears where it improves a real workflow.")),
            HelpTopic("Performance Preparation", points = listOf(
                "Synchronize before leaving for the venue.",
                "Confirm the event's offline packet and attachments are ready.",
                "Open $liveName, verify the set list and preferred material, and choose Night view for bright PDF or image charts.",
                "Use Local Host when devices share Wi-Fi but cannot reach the internet.",
            )),
        ),
    ),
    HelpSection(
        "settings", "Settings & Access", "CONFIGURATION", "Studio identity, performance behavior, and account access.",
        "Keep organization metadata and operational preferences out of performance screens while making them easy to maintain.",
        listOf(
            HelpTopic("Studio Profile", listOf("Store organization and owner details, address, contact methods, map link, and business metadata. Upload a studio logo, resize it by percentage, and drag it into position. The saved logo treatment is synchronized and displayed without editing controls on the dashboard.")),
            HelpTopic("Workspace Modules", listOf("Open More > Modules to see the workspace's operating profile and effective modules. Owners and Account Managers can select a different focus, such as Individual Musician, Band or Ensemble, Recording Studio, Live Production Company, or Worship Organization.", "A workspace focus does not automatically grant separately priced modules. Workspace Owners can open Manage Module Billing to choose available specialized modules; access appears on Android after synchronization.", "Ending a paid module never deletes its records. Those records remain retained on the server but are removed from equipment, kits, maintenance, reports, printing, export, Crew, APIs, and mobile synchronization until the module is restored. Exports created before deactivation remain yours.", "Open More > Module Data to review workspace-wide choices and each enabled module's categories, equipment types, and specialized options.")),
            HelpTopic("$liveName Settings", points = listOf(
                "Performance Material controls preferred attachment types and priority.",
                "Metronome controls autostart, mute state, tempo behavior, and sound.",
                "Bluetooth Page Turner controls enablement, mode, direction, scroll amount, and exact action mappings.",
                "Clocks independently control wall time, elapsed performance time, and estimated set time remaining.",
            )),
            HelpTopic("Access And Security", listOf("Start with an approved Studio Leviathan membership. Accept the invitation, then sign in with the approved email, access code, and authenticator verification. Google, Microsoft, and passkeys are optional additional sign-in methods, not separate Studio Leviathan accounts.", "To connect Google or Microsoft, open Settings > Sign-in & Security and choose Manage Connected Accounts. Complete the connection in the secure web page. Provider email addresses may differ from the Studio Leviathan login email, but they must be deliberately linked while signed in.", "To add a passkey, open Settings > Sign-in & Security and choose Add Passkey. Android can then use fingerprint, face recognition, or the device screen lock for later sign-ins. The existing access code and authenticator remain available for recovery.", "An unlinked Google or Microsoft account, or an unknown passkey, cannot create or enter a Studio Leviathan account. Connecting a method does not change the workspace, licensed seat, role, or permissions.", "Each workspace owns its records. The Owner controls billing, membership, and ownership transfer. Account Managers may invite and manage Editors and Viewers; Editors change operational records; Viewers have read-only access. Each role applies only within that workspace.", "A pending invitation reserves a licensed member seat. Verification activates the login. Revoking access releases the seat and disconnects that member's synchronized devices. Guest sharing does not consume seats.", "Never share passwords, API keys, MFA secrets, or private guest tokens.")),
        ),
    ),
    HelpSection(
        "troubleshooting", "Troubleshooting", "TROUBLESHOOTING", "Fast checks when something does not behave as expected.",
        "Preserve local data first, identify whether the problem is display, connectivity, synchronization, or source-provider related, then retry deliberately.",
        listOf(
            HelpTopic("Common Checks", points = listOf(
                "Missing Android record: Confirm the connection banner, run Sync, check queued changes or conflicts, and verify the same account is used.",
                "Attachment unavailable offline: Reconnect, synchronize the event packet, and confirm attachment readiness.",
                "LRCLIB rejects or finds no lyrics: Verify title and artist, try without album, and remember the provider may lack the recording or temporarily reject requests.",
                "Song metadata has several matches: Select the correct artist and album result; existing values are not overwritten automatically.",
                "Bluetooth pedal moves through controls: Enable it in $liveName Settings and verify the pedal mode and exact key mapping.",
                "Share cannot open: Check expiration, revocation, recipient email, role, and granted scopes.",
                "$agentName uses a placeholder: Populate Owner Name and verify the account assistant configuration.",
            )),
            HelpTopic("Before A Performance", points = listOf(
                "Synchronize every device and verify event people, venue, set list, kits, and offline packet.",
                "Inspect both List and Chart modes and every critical attachment.",
                "Test the page turner, metronome, audio routing, Local Host, and device power.",
                "Print a packet when the performance needs a physical fallback.",
            )),
            HelpTopic("Getting Help", listOf("Include the device, page, approximate time, connection state, record name, and action taken. Do not send passwords, API keys, MFA secrets, or private guest tokens in ordinary support messages.")),
        ),
    ),
)
