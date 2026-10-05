# Documentation Maintenance

Use this checklist for every user-facing Android or Wear OS change.

## Required updates

1. Update `app/src/main/java/com/manoogianmedia/studiorack/ui/HelpContent.kt` when users need a quick in-app explanation.
2. Update the appropriate guide linked from `docs/index.md`.
3. Update `docs/architecture.md` when the data contract, security boundary, storage, synchronization, audio, or Wear relationship changes.
4. Update `README.md` when setup, build, product scope, or primary documentation entry points change.
5. Update `WEAR_OS_RELEASE.md` for watch packaging or capability changes.
6. Update `play-store/listing-en-US.md`, Data safety notes, reviewer instructions, screenshots, or release checklist when externally visible behavior changes.
7. Add concise release notes describing what testers should verify.

## Writing rules

- Use current labels exactly as shown in the application.
- Give a complete navigation path, for example **Sessions > Schedule > Add Event**.
- State whether internet is required.
- State whether the action is role- or module-dependent.
- Separate Android behavior from web administration.
- Explain destructive effects and retained data.
- Never expose internal provider names, database fields, credentials, tenant identifiers, or implementation details in tenant-facing copy unless they are necessary product concepts.
- Do not promise that Android can edit a record merely because it can display it.
- Describe Private labels as classifications unless a documented permission boundary enforces privacy.
- Use sliders/switches for Boolean setting language and buttons/toggles for mode changes, matching the interface.

## Verification

- Search the repository for old labels after a navigation change.
- Build and run unit, instrumentation, and lint checks after changing in-app help.
- Open Help on phone and tablet to check wrapping and section navigation.
- Verify links from `docs/index.md` and image references.
- Review documentation as a Viewer, Editor, Account Manager, and Owner; controls can differ by role.
- Recheck offline claims against the actual repository/API path.
