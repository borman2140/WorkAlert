# WorkAlert

WorkAlert is a minimal native Android app for a two-person alert system.

## Included in this first scaffold
- Kotlin Android application
- Two-second local test tone and vibration
- Android Quick Settings tile
- GitHub Actions workflow that builds a debug APK

## Not yet included
Remote delivery to the coworker's phone is not enabled yet. The next stage adds Firebase Cloud Messaging and device pairing. This initial APK is only for testing the local tone and Quick Settings tile.

## Build
Push the repository to GitHub. Open **Actions → Android APK** and download the `WorkAlert-1.0-debug` artifact.

## Android notes
For reliable background notifications in the later remote-alert version, allow notifications and exempt WorkAlert from battery optimization. Android volume and Do Not Disturb settings remain in control of actual playback volume.
