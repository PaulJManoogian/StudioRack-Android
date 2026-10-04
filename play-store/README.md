# Google Play Release Kit

This directory contains Studio Leviathan's Play listing copy, compliance working notes, reproducible store artwork, screenshots, and release checklist.

Generate the icon and feature graphic from the approved Studio Leviathan brand assets:

```powershell
$env:NODE_PATH = "C:\Users\paulj\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules"
& "C:\Users\paulj\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe" .\play-store\generate-assets.mjs
```

Create signed Android App Bundles locally:

```powershell
.\gradlew.bat :app:bundleRelease :wear:bundleRelease
```

The ignored `release` directory contains upload-ready bundles. `release-manifest.md` records the exact filenames, versions, hashes, and public certificate fingerprint for the current closed-beta build. Private signing material is intentionally stored outside this repository.

The `assets/screenshots` directories contain reviewed Android phone, Android tablet, and Wear OS images captured from the actual application. Replace or add screenshots as the beta content evolves; never use a screenshot containing credentials, access codes, or private participant details.
