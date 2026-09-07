# Branding Firewall

The Android client keeps product identity separate from compatibility-sensitive implementation names.

## Display identity

All user-facing identity values live in `app/src/main/res/values/strings.xml`:

- `app_name`
- `agent_name`
- `publisher_name`
- `copyright_holder`
- `brand_tagline`
- `brand_descriptor`

Launcher labels, splash and login screens, navigation labels, notifications, and assistant-facing copy must use these resources. New features must not hardcode the current product or assistant names.

## Stable compatibility identifiers

A future rename must not require changing these identifiers:

- Application ID and Kotlin package: `com.manoogianmedia.studiorack`
- Room database name and schema identity
- API routes, request fields, and sync entity names
- WorkManager, notification channel, intent-extra, and preference keys
- Existing APK download path and filename
- Existing cached files and device credentials

These values are implementation contracts, not branding. They may retain legacy names so installed applications continue to update and synchronize without migration risk.

## New domain work

Venue, contact, calendar, sharing, guest-access, and live-session models must use neutral domain names. Product identity belongs only at the display boundary.
