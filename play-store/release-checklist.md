# Google Play Closed Beta Checklist

## 1. Protect the application identity

- Keep `C:\Users\paulj\.studioleviathan\signing\studio-leviathan-upload.p12` and its password in at least two encrypted backup locations.
- Never commit the private key or `release-signing.properties`.
- Enroll in Play App Signing when the application is created. The local key is the upload key, not the Play distribution key.
- The package name is `com.manoogianmedia.studiorack`. It is compatibility-sensitive and cannot be changed after the first Play upload.

## 2. Create the Play application

- App name: Studio Leviathan
- Default language: English (United States)
- App or game: App
- Free or paid: Free
- Category: Music & Audio
- Contact email: crew@studioleviathan.com
- Privacy policy: https://www.manoogianmedia.com/privacypolicy.html

## 3. Complete App content

- Add the privacy policy and support details.
- Complete Data safety using `data-safety-notes.md` as a working checklist.
- Declare that sign-in is required and enter tested credentials from `reviewer-access-template.md`.
- Complete ads, content rating, target audience, news, and government-app declarations accurately.
- Review the existing external module-billing link against Play payments policy before rollout.

## 4. Add the store listing

- Paste the English listing from `listing-en-US.md`.
- Upload `assets/studio-leviathan-app-icon-512.png`.
- Upload `assets/studio-leviathan-feature-graphic-1024x500.png`.
- Upload at least two current phone screenshots and representative tablet screenshots from `assets/screenshots`.

## 5. Configure testing

- Create a closed-testing track for the Android phone/tablet bundle.
- Add beta testers with an email list or Google Group and copy the opt-in link.
- Personal developer accounts created after November 13, 2023 may need at least 12 opted-in testers for 14 continuous days before production access. Play Console will show whether this applies.
- Use Play Console pre-launch reports and resolve crashes, ANRs, accessibility issues, and policy warnings.

## 6. Upload the Android application

- Upload `release/studio-leviathan-mobile-0.28.0-117.aab` to the phone/tablet closed-testing track.
- Add `release-notes-en-US.txt` and roll the release out to the closed group.
- Install from the Play opt-in link and test sign-in, synchronization, offline mode, downloads, notifications, calendar, set-list editing, and Leviathan Live.

## 7. Upload the Wear OS companion

- Create a dedicated Wear OS closed-testing track under the same Play application.
- Upload `release/studio-leviathan-wear-0.22.3-78002.aab` to that Wear track.
- State clearly that it is a non-standalone companion requiring the paired Android application.
- Upload current square Wear OS screenshots and test installation from Play on a paired watch.

## 8. Release discipline

- Increase each module's version code for every subsequent upload.
- Keep phone/tablet and Wear version codes unique within the shared package.
- Build and test releases locally. Do not add a GitHub Android build workflow.
- Promote the tested artifact instead of rebuilding different code for production.
