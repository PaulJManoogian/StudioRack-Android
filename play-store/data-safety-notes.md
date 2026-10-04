# Data Safety Working Notes

These notes are a truthful starting point for the Google Play Data safety form. Recheck them against the production server, every bundled SDK, and the final Play form wording before submission.

## Collection and sharing

- The app does not contain advertising SDKs and does not sell user data.
- Data is sent to Studio Leviathan's server to provide account, synchronization, sharing, notification, and support functionality. This is service-provider processing, not a sale.
- Account identity may include name, email address, workspace membership, role, and account identifiers.
- User content may include contacts selected by the user, equipment and maintenance records, songs, set lists, schedules, locations, notes, charts, images, audio, and other attachments.
- App activity may include synchronization state, queued changes, sharing activity, notification state, and operational diagnostics needed to provide and troubleshoot the service.
- Device or other identifiers may be used for authenticated device sessions, notifications, and synchronization.

## Permissions and optional data

- Contacts access is used only when the user explicitly chooses to select a device contact. The app does not continuously upload the address book.
- Notification permission is used for Studio Leviathan reminders, Crew activity, maintenance, sessions, and related account alerts.
- Photos, documents, charts, and audio are accessed only when the user selects or downloads those materials through app features.

## Security and deletion

- Production server traffic uses HTTPS. Leviathan Live can intentionally use a user-controlled local network for offline performance coordination.
- Device credentials are protected with Android Keystore-backed encryption.
- Account deletion and data-retention answers in Play Console must match the public privacy policy and the production account-deletion workflow before release.

## Items to confirm in Play Console

1. Confirm every collected data category against the final release and server logs.
2. Confirm whether each category is required or optional.
3. Confirm account-deletion URL and deletion-request process.
4. Declare the authenticated-account requirement and provide reviewer access.
5. Recheck the external `Manage Module Billing` link against Google Play payments policy before rollout. Do not remove the feature without a product decision.
