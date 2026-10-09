# Studio Leviathan Android Documentation

This documentation describes the Android phone, Android tablet, and Wear OS applications as they exist in the current closed beta. It is written for musicians, band members, studio staff, production teams, workspace owners, and beta testers.

## Start here

1. [Getting Started](getting-started.md) - install, sign in, understand synchronization, and prepare the first device.
2. [Android User Guide](android-user-guide.md) - every main navigation area and its common workflows.
3. [Calendar and Events](calendar-and-events.md) - create events, use month and agenda views, notify participants, and share calendar files.
4. [Leviathan Live](leviathan-live.md) - prepare and run a set list, use performance material, playback, timing, pedals, Local Live, and audio routing.
5. [Hardware Performance Qualification](hardware-performance-qualification.md) - understand how Android compatibility, Leviathan Live minimums, and recommended multitrack hardware are measured.
6. [People, Membership, and Sharing](people-membership-sharing.md) - directory records, workspace seats, band membership, material access, and guest links.
7. [Offline Work and Synchronization](offline-and-sync.md) - queued changes, attachment readiness, conflicts, and recovery.
8. [Wear OS Companion](wear-os.md) - installation, connection states, controls, and limitations.
9. [Theme System](theme-system.md) - planned cross-platform appearance choices, accessibility, persistence, and Leviathan Live behavior.
10. [Troubleshooting and Support](troubleshooting.md) - symptom-based checks and the information to include with a support request.
11. [Glossary](glossary.md) - product terms used across the web, Android, and Wear applications.

## Product boundaries

Studio Leviathan uses one canonical cloud workspace:

- The **web application** is the complete management platform and canonical store. It includes full administration, billing, account approval, platform operations, large-screen layout editing, and every detailed record editor.
- The **Android application** is the offline-first operational application. It synchronizes approved workspace data, supports rehearsal and performance workflows, provides focused creation and editing where implemented, and keeps stage-critical material available locally.
- The **Wear OS application** is a companion for Leviathan Live. It does not replace the phone or tablet and does not contain platform administration.

An Android screen may intentionally provide a focused subset of the corresponding web screen. A missing administrative control is not necessarily a synchronization failure. Check the relevant guide before assuming that a control should be present.

## Help inside the app

Open **More > Help** for the in-app reference. It is designed for quick answers while using the application. These documents provide longer procedures, preparation checklists, explanations, and troubleshooting context.

Alpha and beta accounts also see the music-themed **Tester Feedback** button in the top bar. Use it for product feedback; use support when access, data safety, or urgent recovery assistance is required.

## Documentation maintenance rule

When a user-facing workflow changes, update all applicable locations in the same release:

- the in-app help in `HelpContent.kt`;
- the relevant guide in this directory;
- Play listing or reviewer instructions when store behavior changes;
- release notes for behavior testers need to retest;
- architecture documentation when storage, security, or synchronization contracts change.
