# Vehicle Capture Android App

This is an Android Studio project for a vehicle registration capture app.

Features:
- Rear camera using CameraX
- ML Kit OCR
- Manual correction before saving
- Offline in-memory records during the app session
- Excel (.xlsx) export to Downloads

## Build
Open this folder in Android Studio, sync Gradle, then Build > Build APK(s).

A GitHub Actions workflow is included. If the project is pushed to a GitHub repository, Actions can build the debug APK automatically and provide it as an artifact.

## Note
This is Version 1. Production use should add persistent local database storage, duplicate detection, better plate-specific OCR/cropping, and a safer Android Storage Access Framework export flow.
