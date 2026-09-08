# Branding Firewall

The Android client keeps product identity separate from compatibility-sensitive implementation names.

## Display identity

All user-facing identity values live in `app/src/main/res/values/strings.xml`:

- `app_name`
- `agent_name`
- `agent_workshop_name`
- `live_mode_name`
- `publisher_name`
- `copyright_holder`
- `brand_tagline`
- `brand_descriptor`
- `public_base_url`
- `api_base_url`
- `export_file_prefix`

Launcher labels, splash and login screens, navigation labels, notifications, live-mode labels, assistant-facing copy, and network locations must use these resources. A white-label source set can override the strings and the generic `brand_logo` and `brand_splash_icon` drawables without changing Kotlin code.

The default package displays **Studio Leviathan**, **Crew**, **The Foundry**, and **Leviathan Live**. The Crown is the approved product symbol. Its canonical SVG and raster exports live in the web repository under `drumdb/branding/studio-leviathan`; Android drawables are platform derivatives of that master.

## Stable compatibility identifiers

A future rename must not require changing these identifiers:

- Application ID and Kotlin package: `com.manoogianmedia.studiorack`
- Room database name and schema identity
- API routes, request fields, and sync entity names
- WorkManager, notification channel, intent-extra, and preference keys
- Existing APK download aliases and filenames
- Existing cached files and device credentials

These values are implementation contracts, not branding. They may retain legacy names so installed applications continue to update and synchronize without migration risk.

New builds use `https://www.manoogianmedia.com/leviathan/api/v1`. The server keeps the legacy `/studiorack/api` mount operational for already-installed builds.

## New domain work

Venue, contact, calendar, sharing, guest-access, and live-session models must use neutral domain names. Product identity belongs only at the display boundary.
