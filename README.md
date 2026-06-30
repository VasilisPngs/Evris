# Evris

Evris is an Android update scout for installed apps.

It scans installed apps on the device and checks for newer versions. APKMirror is the main source. Google Play checking is optional and experimental through anonymous Aurora/GPlayApi access.

## Features

- Local installed-app scan
- Installed version and available version comparison
- APKMirror update checks
- Optional experimental Google Play update checks
- Google Play base and split APK handling through Android PackageInstaller
- User and system app visibility
- Search by app name or package name
- Manual refresh from the main screen
- Light and dark theme support
- Signed release APK generation through GitHub Actions

## Android

- Minimum SDK: Android 16 / API 36
- Target SDK: Android 16 / API 36
- Compile SDK: Android 16 / API 36

## Package

```text
com.evris.android
```

## Release asset

```text
Evris-1.0.0.apk
```

## Google Play source

Google Play support is experimental. It depends on anonymous Aurora/GPlayApi behavior and can break if Google Play or Aurora authentication changes.
