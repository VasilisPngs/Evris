# Evris

Android app update scout for installed apps.

## Status

Android 16+ Kotlin/Compose app.

## Current behavior

- Scans installed Android apps locally
- Shows installed apps, icons, package names, versions and user/system status
- Checks APKMirror update data through the APKMirror API-style backend
- Optionally checks Google Play through anonymous Aurora/GPlayApi access
- Installs Google Play base and split APK files through Android PackageInstaller
- Filters prerelease update channels from Settings
- Searches installed apps by app name or package name
- Rescans installed apps and checks updates from the top refresh action
- Supports manual light/dark theme switching
- Uses bottom navigation for Home, Search and Settings
- Uses Android 16 API 36 baseline for compile, target and min SDK

## Package

`com.evris.android`

## Release asset

`Evris-1.0.0.apk`

## Notes

Google Play source is experimental and can break because it depends on anonymous Aurora/GPlayApi behavior.
