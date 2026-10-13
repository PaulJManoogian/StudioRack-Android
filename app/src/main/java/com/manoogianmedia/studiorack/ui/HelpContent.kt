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
                "Home: See workspace identity, studio value, upcoming work, care priorities, recent $agentName activity, and synchronized totals.",
                "Live: Open Songs & Charts, Set Lists, Schedule, Tuners, Live BPM, Live Settings, or $liveName from one menu.",
                "Gear: Open Equipment, Kits, or Maintenance. Search synchronized equipment, record field notes, review service history, complete maintenance, and export the visible list.",
                "People: Open People & Places or Sharing. Reports opens the reporting workspace directly; More contains members, Crew, modules, Help, synchronization, and settings.",
            )),
            HelpTopic("Items", listOf("Items are the main equipment records. Android displays synchronized identity, category/type, location, status, units, serials, category-specific details, images, notes, and maintenance. Use the canonical web application for complete item creation and comprehensive equipment-field editing.")),
            HelpTopic(agentName, listOf("$agentName watches care details, sends trackable reminders, reads replies, updates matching actions, and keeps an action history. Configure timing and quiet hours, test delivery, and teach enabled skills how your organization works.")),
            HelpTopic("Reports", listOf("Reports review equipment, value, care, scheduled work, and supporting notes. Filter first, then export the records currently in view.")),
        ),
    ),
    HelpSection(
        "equipment", "Equipment & Kits", "INVENTORY", "Equipment, assets, kits, and care.",
        "Build a useful record once, then use it for insurance, valuation, maintenance, packing, and performance preparation.",
        listOf(
            HelpTopic("Equipment On Android", points = listOf(
                "Open Gear and search by name, category, type, status, location, serial detail, or other synchronized text.",
                "Expand a strip to review details, units, specifications, open field notes, and maintenance history.",
                "Use Add Maintenance Note for a new issue, or Maintenance / History to record completed service and resolve reminders.",
                "Use the canonical web application for complete equipment creation, valuation, image/file management, and comprehensive field editing.",
            )),
            HelpTopic("Value And Depreciation", points = listOf(
                "Purchase value records the original transaction amount and date.",
                "Replacement value records the insurance basis for comparable equipment.",
                "Estimated value is the depreciated or curated value used by the dashboard and Overview report.",
                "Curated, irreplaceable, vintage, and out-of-production equipment can be identified separately.",
            )),
            HelpTopic("Maintenance Records", listOf("Open an item's maintenance page to record completed service. Each completion becomes a permanent history entry. Completing maintenance records the date, work, performer, and notes, dismisses resolved reminders, and can establish the next due date.")),
            HelpTopic("Kits", listOf("Kits are reusable equipment collections. Android can search synchronized kits, review location, notes, item assignments and quantities, and export the visible list. Create or comprehensively edit kit composition and images in the canonical web application. Events may include multiple kits, and printed event packets include kit checklists.")),
        ),
    ),
    HelpSection(
        "music", "Songs & Live", "MUSIC LIBRARY", "Songs, performance material, set lists, and $liveName.",
        "Keep rehearsal sources and stage-critical information together, then make it available offline.",
        listOf(
            HelpTopic("Songs", listOf("Add or browse songs in Library. Song records support title, artist, album, style, key, tempo, time signature, length, who starts, patch, notes, favorite status, and listening links."), listOf(
                "GetSongBPM can search metadata while preserving fields you already populated.",
                "Where possible, LRCLIB duration is combined with metadata results.",
            )),
            HelpTopic("Performance Material", listOf("A song may contain multiple Charts, Drum Charts, Lyrics, Tabs, Sheet Music, MusicXML Scores, or other materials. MusicXML and compressed MXL files preserve the portable source while Studio Leviathan renders notation for offline Live use, printing, and Audition. Attach PDFs and images, link a source, paste plain text, or store ChordPro, synchronized lyrics, Nashville drum charts, Nashville drum notation, and drum tabs directly. ChordPro section directives identify verses, choruses, bridges, and other parts. Add a Timing Mark immediately before a section when it should follow the Leviathan Live song clock. Saving timed ChordPro material creates or updates the matching shared Song Sections automatically.", "For written percussion, choose Nashville Drum Chart for a compact section-and-bar roadmap, Nashville Drum Notation for a song-form table plus conventional percussion staves, or Drum Tab for explicit instrument-line patterns. Start a section with [Verse] x8, add rows such as HH|x-x-x-x-| SD|--o---o-| BD|o---o---|, and use cue:, fill:, sticking:, or dynamics: for instructions. Each format remains separately editable and available offline. Use x for cymbals or hats, o for a normal hit, > for an accent, g for a ghost note, f for a flam, and - for an empty subdivision."), listOf(
                "Choose the material type and a clear display name.",
                "Mark the preferred $liveName default, print inclusion, and priority.",
                "Search LRCLIB by title, artist, and album, then review imported text before saving.",
                "Expand a song in the Songs list to open any synchronized or linked performance material directly. Use Night view for rendered PDF and image pages when a bright document would be distracting on stage.",
                "Synchronize attachments before a performance so $liveName can read local copies without cellular service.",
            )),
            HelpTopic("Written Drum Chart Formats", points = listOf(
                "Shared structure: optional facts use {title: Song}, {tempo: 120}, {time: 4/4}, and {feel: Straight 8ths}. Start sections with [Intro] x4, [Verse] 8 bars, or [Tag] bars=2. The section name and a prominent bar-count badge appear together before the pattern in Chart Mode.",
                "Nashville Drum Chart is the compact performance roadmap. Example: [Chorus] x8 followed by pattern: CR|>-------| HH|x-x-x-x-| SD|--o---o-| BD|o-o---o-| and fill: Toms across final bar. In Chart Mode, the metronome follows the declared sections and stops after the final bar. Choose Audition to hear recognized rows through the selected Audio Out. A selected MIDI destination also receives General MIDI percussion notes on channel 10.",
                "Drum Tab keeps every instrument row visible as a labeled grid. Four pattern characters represent quarter-note space, eight represent eighth-note space, and sixteen represent sixteenth-note space. Keep every row in one section the same length.",
                "Nashville Drum Notation uses the same text but renders a song-form overview and conventional five-line percussion staff. In Chart mode, every stave spans the available phone or tablet width, active abbreviations remain beside their staff positions, and one compact key above the sections gives the full instrument names without repeating large designation blocks.",
                "Symbols: x is a cymbal or hi-hat strike, o is a normal drum strike, > or A is an accent, g is a ghost note, f is a flam, and -, _, or . leaves the subdivision empty.",
                "Directions: cue: marks an entrance; fill: describes a transition; instruction: or note: adds general direction; sticking: records hands; dynamics: records musical intensity.",
                "Instrument labels: CR/CC/RC crash; HH/OH hi-hat; RD ride; T1/HT high tom; T2/MT middle tom; SD/SN/RS snare or rimshot; T3/LT low tom; FT floor tom; BD/KD/K bass drum or kick; HF foot hi-hat.",
                "The saved source is shared by web and Android. After synchronization, the chosen format remains editable and renders in previews, printed packets, and Leviathan Live Chart Mode while offline-capable Android retains its local copy.",
            )),
            HelpTopic("Set Lists", listOf("Create sets, search and add songs, then drag to reorder. Rename, copy, print, share, favorite, or export a list. Named medley and tribute groups indent only their songs and show each song's position in Chart mode. Song lengths calculate estimated set time.")),
            HelpTopic("Instrument And Drum Tuners", points = listOf(
                "Open Music & Live > Tuners. Tuned mode provides chromatic mode, common guitar/bass/ukulele/orchestral string standards, a string target, concert-pitch adjustment, and a reference tone.",
                "Percussion separates type from size or voice and supports snares, toms, bass drums, timpani, djembes, congas, timbales, bongos, and tabla. Measurements follow the instrument: only double-headed drums show a resonant head, while djembe and tabla use tension points.",
                "The drum map guides star or clockwise lug order. Capture a clear reading to mark a lug high, low, or in range; Lock Current Reading preserves a sound as the target. Saving retains the environment, order, and captured lugs with the Gear item.",
                "Typical comparisons include both note spans and frequencies. The Musical Range Map places every supported diameter in the selected drum family on one pitch scale and highlights the selected size.",
                "Listen requests microphone permission and analyzes audio only on this device. Center is in tune; left is flat; right is sharp. Weak, clipping, or uncertain states are not precise readings.",
                "Save tuning to... associates the target, measurement, zone, and notes with existing Gear. The change queues offline and synchronizes later.",
            )),
            HelpTopic("Live BPM", points = listOf(
                "Open Music & Live > Live BPM for a dedicated practice tempo display. Start Listening uses the device microphone; Tap Tempo works without microphone access and settles after several consistent taps.",
                "The history graph shows recent movement against the target line. The rush/drag meter moves right when the detected tempo is faster than target and left when it is slower. Green is within about 1 BPM, yellow is close, and red needs attention.",
                "Move the Target BPM slider for fast adjustment, or tap the number and enter an exact tempo. Choose Click when an audible reference is useful. The click and tracker are independent: the tracker measures what reaches the microphone, not the metronome's internal clock.",
                "Use ÷2 or ×2 when the musical pulse is being interpreted at half or double time. Low confidence is not a result; move closer to the rhythmic source, reduce background sound, or use Tap Tempo.",
                "In Leviathan Live Chart mode, the waveform button shows or hides a compact tracker whose target comes from the current song. Tempo analysis remains local and does not record or synchronize audio.",
            )),
            HelpTopic("Practice Metronome", points = listOf(
                "Open Music & Live > Metronome. Set tempo with the slider, exact number, step buttons, or Tap Tempo, then choose Start.",
                "Use Saved Rhythms to recall a complete setup. The floppy-disk icon names and saves tempo, meter, beat unit, subdivision, accents and rests, cross rhythm, trainer, silent bars, click sound, and volume with your account. The same list synchronizes with the web. Select a saved rhythm and choose the upward-arrow icon to load it; use the star to make it a Quick Start.",
                "Beat controls provide meters from 1 through 12 with half-, quarter-, eighth-, sixteenth-, thirty-second-, or sixty-fourth-note beat units. The stacked meter uses a proper fraction bar, and changing meter immediately removes or adds beat controls to match.",
                "Compact notation buttons select half, quarter, eighth, triplet, sixteenth, thirty-second, or sixty-fourth notes. Every beat cycles through Accent, Note, and Rest.",
                "Choose Studio, Wood, Clave, or Cowbell for the click sound and set its output volume without changing the visual pulse.",
                "The collapsible Live BPM panel sits inside the main metronome display above the animated beat cues. Keep it open to compare the graph and rushing-or-dragging meter with the click, or collapse it to reclaim space and release the microphone. Audio is not recorded or synchronized.",
                "Poly overlays two independently voiced pulse counts in one cycle. Begin with 3:2 and follow the two colored pulse rows.",
                "Practice mode can move toward a goal tempo after a chosen number of bars and can alternate audible bars with silent bars to test internal time.",
                "Full Screen removes only the Practice Tool and Metronome heading. The full metronome remains scrollable, including rhythm controls, practice settings, favorites, and saved-rhythm controls. Presets synchronize with your account but do not change songs, set lists, or Leviathan Live playback.",
            )),
            HelpTopic(liveName, points = listOf(
                "List mode provides a large, scannable running order.",
                "Chart mode shows stage-critical song facts and the selected chart, lyrics, tab, or other material.",
                "In Chart mode, open the text-size button beside Live Network to choose Single line, Small, Medium, or Large song details. Large is the original full-size presentation; the smaller choices preserve progressively more room for the chart and remain selected between performances.",
                "A complete PDF or image page is fitted inside the available chart area instead of being enlarged beyond the screen. Nashville drum notation uses full-width staves with compact vertical spacing and one shared instrument key; other written drum formats fit as complete performance pages so the arrangement remains visible without hand scrolling. Multi-page documents retain explicit page controls.",
                "Playback audio is independent of charts, lyrics, PDFs, ChordPro, and other performance material. In chart view, one button enables or disables playback and the adjacent button arms or disarms automatic start. A song's only playback track is used automatically; choose the intended track in the set list when a song has more than one.",
                "The first song always waits for a manual start. With playback and autoplay both active, moving to another song starts its selected audio automatically. Autoplay never runs while playback is disabled. The playback clock is the foundation for timed lyrics, document movement, markers, MIDI, and lighting cues as those capabilities become available.",
                "Song Timeline uses the canonical web-authored Song Sections and synchronized lyrics. During local playback, Android shows the current Section, current lyric, and next lyric from the same offline-ready records used by the web application.",
                "Edit mode deliberately enables immediate add, edit, reorder, and swipe-delete changes.",
                "A paired Wear OS companion shows the current and next song, set and medley position, controls previous or next, and runs a synchronized haptic metronome while the phone or tablet remains the performance authority. Open Sessions > Leviathan Live > Settings > Wear OS to check the connection or install the companion on a connected watch.",
                "Local Host synchronizes participating devices on shared Wi-Fi when internet service disappears.",
                "Bluetooth page turners use only configured performance actions. Clock, elapsed time, and set time remaining can each be shown or hidden.",
            )),
            HelpTopic("Live Buttons And Mixer", points = listOf(
                "Back returns to the event; Live Network opens Local Live status; Song Detail Size chooses Single line, Small, Medium, or Large chart-header density; Edit opens permitted set-list editing; List/Chart toggles the two performance views; X returns from a chart to the running order.",
                "Phone Format changes responsive density; Sun/Moon changes material appearance; Performance Audio enables playback tools; Autoplay arms later song starts; Live Hardware opens audio output, routing profile, MIDI, and show-control choices.",
                "Previous and Next move through the set list. Metronome starts or stops timing; Metronome Sound mutes the click without stopping its pulse. Arrangement Play/Pause and Stop control every stem together.",
                "Tracks shows every stem. Its title uses the short Stem Role, such as Vocals, Drums, Guitar, Bass, Click, or Guide, rather than the uploaded filename. The Bus button changes that track's logical destination.",
                "Buses shows every available logical bus. A bus strip controls shared gain and Mute for all assigned tracks. Routing selects how buses reach numbered hardware outputs.",
                "Audio Output selects the device-local destination. Routing selects a compatible saved profile. Song Master applies final arrangement gain after track and bus gain.",
                "Meters show measured signal activity. Gain faders change level. Mute silences while preserving settings; Solo isolates selected source tracks. A moving meter does not prove that external cabling or mixer channels are correct.",
            )),
        ),
    ),
    HelpSection(
        "schedule", "Schedule", "SESSIONS", "Performances, rehearsals, studio sessions, and other work.",
        "Use the visual month calendar or chronological agenda for performances, rehearsals, studio sessions, travel, and other work.",
        listOf(
            HelpTopic("Creating An Event", points = listOf(
                "Choose Performance, Rehearsal, Studio Session, or Other.",
                "Enter title, start and required end date/time, or choose All-day. Multi-day work must use its actual final date.",
                "Choose calendar color, importance, and the Private designation. Private is a classification; workspace permissions remain the access boundary.",
                "Select a reusable venue when possible, then add room, entrance, directions, and one-off location notes.",
                "Attach a set list, select kits and gear, and associate groups or individual contacts.",
                "Configure the $agentName reminder and save. Copy an existing event when most details repeat.",
            )),
            HelpTopic("Finding And Using Events", listOf("Month view provides Previous, Today, and Next navigation plus a Sunday/Monday week-start preference. Select a day to add an event. Agenda view can search and filter by event details, type, and status. Event actions provide details, directions, $liveName, People, calendar-file delivery, editing, copying, sharing, and deletion according to permission. People can text or email direct participants, a selected band/group, venue contacts, everyone, or a custom selection; sliders include or exclude each person.")),
            HelpTopic("Event Readiness", listOf(
                "Open Event Readiness from an event to prepare people, music, gear, technical needs, logistics, and business details in one focused board. Start with Local, Solo, Band, or Custom; the selected template becomes that event's plan and does not replace the event's existing records.",
                "Critical and required items determine Ready, At Risk, In Progress, or Not Started. Advisory items stay visible without blocking the event. Assign responsibility, record concise evidence, and give a reason whenever an item is not applicable or deliberately overridden.",
                "A synchronized board remains readable offline. Status, responsibility, evidence, and exception changes queue on the device and synchronize later. Creating a plan or immutable readiness snapshot requires a connection.",
            )),
            HelpTopic("Production Advance", listOf(
                "Open Production Advance from a scheduled event to carry the current stage plot, signal paths, input list, technical correspondence, festival acts, hardware configuration, and equipment custody into the field.",
                "Android presents the synchronized current revision as a focused offline reference. Build, revise, publish, acknowledge, issue festival intake links, and edit hardware configuration in the web workspace; synchronize before leaving reliable connectivity.",
                "A published technical plan remains stable. Later changes appear as a new numbered revision so the view always identifies the plan people accepted.",
            )),
            HelpTopic("Calendar Files", listOf("The event calendar action creates an .ics file from the event's dates, times, location, and supported details. Choose recipients and email or text from the event. Android opens the selected messaging application for review; some texting applications cannot attach calendar files, so email or the Android share sheet may be required.")),
            HelpTopic("Printed Event Packets", listOf("Packets include event and venue details, a concise readiness summary, directions, people in readable columns, kit checklists, set and medley headings, song order and metadata, notes, and selected printable performance material.")),
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
            HelpTopic("Post-Performance Check-In", listOf("After a newly scheduled performance is complete, Crew can check in after a configurable delay. Every ended performance with an unfinished closeout also remains visible in dashboard Care Readiness until the work is complete. Reply naturally with anything damaged or missing, maintenance that may be needed, or a follow-up that should not be forgotten.", "Crew Behavior can turn this workflow off, set its delay and unanswered closeout period, or allow one additional follow-up. Turning Crew off stops new work and dismisses queued work; replies to earlier Crew messages are retained but are not interpreted or allowed to change records. Crew separates reported equipment issues, proposes matches from the workspace's equipment and kits, and asks for confirmation. Maintenance records are created only after those matches are confirmed; ambiguous equipment is never guessed.")),
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
                "Mileage records business trips, optional Event links, distance, purpose, vehicle, costs, and reimbursement. Entries synchronize with the web report and can be created or edited offline.",
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
            HelpTopic("Offline Changes", listOf("Supported Android editors can save songs, written performance material, set lists, events, Event Readiness requirement updates, mileage, directory records, maintenance notes, and service completions locally. Changes enter a queue and push when connectivity returns. Creating a readiness plan or snapshot, secure invitations, billing, share-token creation/revocation, provider-file import, provider activation, and server AI require a connection. Complete equipment and kit creation remain web management workflows.")),
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
            HelpTopic("Appearance", listOf("Open More > Settings and use the Appearance card to choose Leviathan Dark, Stage Black, Studio Light, Graphite, or Follow Device. Tap Save Appearance to cache the choice offline and synchronize it with your personal account.", "Leviathan Live can follow the application theme or always use Stage Black during a performance. This saved preference is separate from the temporary sun/moon material control. Changing appearance does not restart audio, reload charts, clear mixer values, reset the metronome, or move any controls.")),
            HelpTopic("Workspace Modules", listOf("Open More > Modules to see the workspace's operating profile and effective modules. Owners and Account Managers can select a different focus, such as Individual Musician, Band or Ensemble, Recording Studio, Live Production Company, or Worship Organization.", "A workspace focus does not automatically grant separately priced modules. Workspace Owners can open Manage Module Billing to choose available specialized modules; access appears on Android after synchronization.", "Ending a paid module never deletes its records. Those records remain retained on the server but are removed from equipment, kits, maintenance, reports, printing, export, Crew, APIs, and mobile synchronization until the module is restored. Exports created before deactivation remain yours.", "Open More > Module Data to review workspace-wide choices and each enabled module's categories, equipment types, and specialized options.")),
            HelpTopic("$liveName Settings", points = listOf(
                "Performance Material controls preferred attachment types and priority.",
                "Metronome controls autostart, mute state, tempo behavior, and sound.",
                "Bluetooth Page Turner preserves the classic previous, next, metronome, and mute defaults. Configure the canonical 2-, 4-, or 6-button layout and learned actions in the web application; Android synchronizes and applies them locally in Leviathan Live.",
                "Assignable actions include song and page navigation, manual scrolling, playback and autoplay toggles, current-audio play/pause or stop, and metronome controls. Pedal presses are handled locally without database or network work.",
                "Clocks independently control wall time, elapsed performance time, and estimated set time remaining.",
                "Wear OS reports companion status and can open the shared Play listing on a connected watch. The watch remains a companion to the phone/tablet performance authority.",
                "Audio Routing maps named virtual buses to valid mono or stereo physical outputs. Each profile is collapsible, each route can be muted, and removing a profile does not delete buses or audio files.",
                "Performance layout templates are visually authored and published on the web. Android applies the published phone/tablet layout with device-safe constraints.",
            )),
            HelpTopic("Access And Security", listOf("Start with an approved $productName membership. Accept the invitation, then sign in with the approved email, access code, and authenticator verification. Google, Microsoft, and passkeys are optional additional sign-in methods, not separate $productName accounts.", "To connect Google or Microsoft, open Settings > Sign-in & Security and choose Manage Connected Accounts. Complete the connection in the secure web page. Provider email addresses may differ from the $productName login email, but they must be deliberately linked while signed in.", "To add a passkey, open Settings > Sign-in & Security and choose Add Passkey. Android can then use fingerprint, face recognition, or the device screen lock for later sign-ins. The existing access code and authenticator remain available for recovery.", "An unlinked Google or Microsoft account, or an unknown passkey, cannot create or enter a $productName account. Connecting a method does not change the workspace, licensed seat, role, or permissions.", "Each workspace owns its records. The Owner controls billing, membership, and ownership transfer. Account Managers may invite and manage Editors and Viewers; Editors change operational records; Viewers have read-only access. Each role applies only within that workspace.", "A pending invitation reserves a licensed member seat. Verification activates the login. Revoking access releases the seat and disconnects that member's synchronized devices. Guest sharing does not consume seats.", "Never share passwords, API keys, MFA secrets, or private guest tokens.")),
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
            HelpTopic("Alpha And Beta Feedback", listOf("Eligible tester accounts see a circular music-themed feedback button in the top bar. Choose the issue type, product area, impact, summary, and reproducible detail. Feedback is for product behavior and ideas; use support for account access, security, or urgent recovery. Never include access codes, authenticator secrets, private client information, API keys, or guest tokens.")),
        ),
    ),
)
