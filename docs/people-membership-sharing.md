# People, Membership, and Sharing

## Three different relationships

Studio Leviathan separates three concepts that are easy to confuse:

- A **directory contact** is a person or organization record used for events, venues, communication, and notes.
- A **workspace member** has an account seat and a role inside one workspace.
- A **band/group member** belongs to one or more performing groups and can receive role-based performance material.

A person can be all three. Linking the records avoids duplicate names and email addresses while preserving different permissions.

## People and venues

Open **More > People**.

Contacts can include a photo, organization, title, private notes, several labeled phone/email methods, and a preferred method. Venues can include address, map link, phone, email, website, image, operational notes, and linked contacts with roles.

### Import a device contact

Choose the import action and select one contact from Android's picker. Studio Leviathan receives the selected information only. Review it before saving, especially labels and preferred methods.

### Communication

Call, text, and email actions are shown only when the relevant method exists and Android can resolve a compatible application. Studio Leviathan opens that application; it does not silently send the message.

## Workspace members and seats

Open **More > Members**.

The seat summary distinguishes licensed seats, reserved seats, and seats available for invitation. A pending invitation reserves a seat immediately.

### Invite a member

1. Confirm that a seat is available.
2. Select **Invite Member**.
3. Optionally link an existing contact.
4. Enter name and email.
5. Select the permitted role.
6. Send the invitation.

Invitation requires connectivity because the server creates and delivers the secure membership invitation.

### Roles

- **Owner** - billing, members, settings, ownership transfer, and operational work.
- **Account Manager** - manages Editors/Viewers and performs operational work; exact billing authority remains controlled by the workspace policy.
- **Editor** - changes operational records permitted by active modules.
- **Viewer** - read-only access.

Roles apply inside one workspace. The same person can have a different role in another band, studio, or organization.

### Manage a member

Owners and permitted Account Managers can change eligible roles, resend pending invitations, revoke access, and in the Owner's case transfer ownership. Revocation releases the seat and disconnects synchronized access for that membership; it does not erase the person's unrelated memberships.

## Bands and groups

A person can belong to several bands. Each membership can carry:

- authority within that group;
- performance roles such as drums, guitar, vocals, keys, or production;
- active/inactive status;
- primary-contact designation;
- a material-access policy.

Group sections begin collapsed so people with many memberships can scan the page. Expand one group to review or edit its members in context.

## Material access

Material access is designed around inheritance and exceptions so a 500-song catalog does not require 500 manual decisions.

Use this order:

1. Define the group's default sharing behavior.
2. Assign performance roles to members.
3. Let material inherit the group/role behavior.
4. Add exceptions only where the default is wrong.

Examples:

- Lyrics can be shared with everyone.
- Guitar ChordPro can be available to guitarists and vocalists.
- Drum charts can be limited to drummers.
- Playback stems can be restricted to members whose role requires them.
- A specific arrangement can override the default for one member or event.

## Member overlays

Shared canonical material and personal overlays are separate. A member can add personal notes or preferences without changing everyone else's source chart. The same person can therefore maintain a different overlay for the same song in different bands when context requires it.

Do not put private personal notes into the shared canonical material when they should remain member-specific.

## Joined bands

Open **More > Sharing > Joined Bands** to review material received through band membership. Access is constrained by the current membership, material rules, active status, and server permissions.

Synchronize after role or membership changes. Removed material can disappear from offline operational views after the next successful reconciliation.

## Guest links and registered shares

### Guest link

A guest link provides temporary scoped access to someone without a Studio Leviathan account. Configure expiration, role, scopes, and whether the recipient may retain a copy where supported. Treat the URL as a secret.

### Registered share

A registered share targets a matching account and appears in **Shared With Me**. Its permissions remain separate from workspace membership.

### Review outgoing shares

Open **My Shares** to inspect recipient, delivery, status, expiration, permissions, and last access. Owners can edit or revoke active shares. Token creation and revocation require connectivity.

Private directory notes, platform administration, internal database fields, delivery-provider names, and other tenants' records are never part of a normal recipient view.
