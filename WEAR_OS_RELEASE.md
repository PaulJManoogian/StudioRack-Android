# Studio Leviathan Wear OS Release

The phone and watch apps use the same application ID and signing identity so Google Play can associate them under one listing. They remain separate device-targeted artifacts.

## Play Console

1. Add Wear OS in **Test and release > Advanced settings > Form factors**.
2. Upload a round Wear OS screenshot and opt in to the Wear OS review policy.
3. Publish the mobile and Wear app bundles to the same internal-testing audience.
4. Confirm that the phone artifact uses its normal version code and the Wear artifact uses its independent Wear version-code sequence.
5. Verify the listing on a tester account from both the phone and watch Play Stores.

## Builds

```powershell
.\gradlew.bat :app:bundleRelease :wear:bundleRelease
```

Release bundles are produced in:

- `app/build/outputs/bundle/release/`
- `wear/build/outputs/bundle/release/`

Release builds must be signed with the same production key. Do not upload debug-signed artifacts.

## Installation Experience

The Android app checks connected Wear OS nodes and the `leviathan_live_watch` capability. The Wear OS panel reports whether the companion is installed. If it is missing, **Install on Watch** opens the Google Play listing on each connected watch through `RemoteActivityHelper`.

## Release Verification

- Phone reports the connected watch by name.
- Phone reports `INSTALLED` after the Wear companion is installed.
- Watch reports `LIVE` when Leviathan Live is open on the phone or tablet.
- Previous, next, metronome, and mute commands are acknowledged without duplicate execution.
- The watch retains the latest set state as `CACHED` during a temporary disconnect.
