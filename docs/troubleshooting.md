# Troubleshooting and Support

## Sign-in problems

### Email or access code rejected

- Confirm the exact approved email, including aliases.
- Confirm the account was approved and the workspace invitation accepted.
- Ask the workspace Owner to verify that the seat is still active.
- Use the latest welcome/access message; a replacement code can invalidate an older one.
- Do not send the access code in an ordinary support email or feedback report.

### Authenticator rejected

- Confirm the device clock is set automatically and correct.
- Wait for a fresh authenticator code and enter it once.
- Confirm the authenticator belongs to the same account email.
- Use the documented recovery route rather than repeatedly guessing.

### Passkey not found

Use email/access code/authenticator, then manage passkeys from Sign-in & Security. A Google or Microsoft login is not automatically a Studio Leviathan passkey.

## Data is missing

1. Read the connection banner and queued count.
2. Confirm the account/workspace shown on Home.
3. Run **More > Sync > Synchronize**.
4. Review conflicts and module access.
5. Confirm the record exists in the canonical web application.
6. Confirm the current role is allowed to receive it.
7. For shared material, confirm membership, role, inheritance, exceptions, expiration, and revocation.

If the record exists but an attachment does not, use the attachment procedure below.

## Attachment unavailable offline

- Reconnect and synchronize.
- Open the attachment while online.
- Confirm it is a stored Studio Leviathan file rather than an external link.
- Confirm storage space is available.
- Confirm the source file format can be previewed by Android or another installed application.
- For an event, verify the chosen/default performance material is the expected one.

## Calendar problems

### Event is on the wrong day

Check start date, end date, all-day state, and timezone. For overnight or multi-day work, enter the actual end date.

### ICS does not attach to a text

The selected messaging application may not support calendar-file attachments. Use email or Android's general share sheet.

### Person is absent from recipients

Confirm the person is directly attached to the event, belongs to an attached group, or is a linked venue contact. Confirm the chosen recipient scope and that the person has the selected email/phone method.

### Reminder did not arrive

Check Android notification permission, Studio Leviathan reminder settings, Crew/quiet hours, event reminder lead time, synchronization, and battery/background restrictions.

## Leviathan Live problems

### Current song is not visible on a phone

Confirm the phone-format control is active or let the narrow-device layout apply automatically. Return to Chart view after changing layout. If a custom web template is active, test a supplied template and synchronize.

### Section marker is missing

Confirm the song has canonical Song Sections or timed ChordPro directives, section times increase, the correct material is selected, and Android has synchronized after the last save.

### Selecting a section scrolls incorrectly

Confirm the section marker corresponds to a heading in the selected material. Duplicate names or mismatched source indices can point to the wrong occurrence. Test the same material on web and Android and report both results.

### Pedal changes focus instead of the song/page

Enable pedal control in Leviathan Live settings, verify the pedal's keyboard mode, synchronize the canonical mapping, and test the exact physical button. Some devices can emit media keys in one mode and arrow keys in another.

### No sound from a playback bus

1. Confirm playback is enabled and the song has the expected track/stems.
2. Confirm the correct routing profile is active.
3. Confirm the bus and route are not muted.
4. Confirm mono/stereo width and selected output pair.
5. Confirm Android detects the intended interface and grants USB access.
6. Check physical cabling, interface mixer, monitor routing, and downstream gain.
7. Test with the internal/stereo output to separate file problems from hardware routing.

### Local Live cannot connect

Confirm all devices are on the same Wi-Fi network, client isolation is disabled, the temporary code is current, and the host remains open. Venue guest Wi-Fi often blocks device-to-device traffic; a dedicated router is safer.

## Synchronization problems

### Changes stay queued

- Run a manual sync and read the resulting message.
- Confirm internet access, not only Wi-Fi association.
- Check for service maintenance.
- Review conflicts.
- Confirm the account still has permission for the record/module.
- Keep the app open during a large attachment upload.

### Duplicate or unexpected data

Stop editing on several devices, confirm the canonical web record, then synchronize one device at a time. Do not delete both copies until the relationship and source are understood.

## Crash or frozen screen

1. Note the exact screen, record, and action.
2. Wait briefly for a large attachment operation.
3. Return to the previous screen if possible.
4. Close and reopen the app without clearing data.
5. Confirm queued changes remain.
6. Reproduce once and submit tester feedback or support information.

Do not clear storage as an ordinary troubleshooting step.

## Submit useful beta feedback

Include:

- device make/model and Android/Wear version;
- Studio Leviathan app version;
- phone or tablet format and orientation;
- exact navigation path;
- record/song/event name when safe;
- expected result and actual result;
- online/offline/sync state;
- whether it happens every time;
- screenshot without credentials or private client information.

Use the in-app tester feedback button for product issues. For account access, security, or urgent recovery, contact `crew@studioleviathan.com`.

Never send passwords, access codes, authenticator secrets, private signing keys, API keys, or guest tokens.
