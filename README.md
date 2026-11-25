# Noir Music

A native Android music-player starter built with Kotlin, Jetpack Compose and AndroidX Media3.

## Current implementation

- Dark purple Compose interface
- Runtime permission flow for audio library access
- Local audio discovery through MediaStore
- Search by title, artist and album
- Playback using Media3 ExoPlayer in a MediaSessionService
- GitHub Actions workflow that builds and uploads a debug APK

## Build without Android Studio

1. Create a new public or private GitHub repository named `NoirMusic`.
2. Upload the contents of this folder to the repository root.
3. Open **Actions** and allow workflows if GitHub asks.
4. Run **Android APK** using **Run workflow**, or push a commit to `main`.
5. Open the completed workflow run and download the `Noir-Music-debug` artifact.
6. Extract the artifact ZIP; install `app-debug.apk` on your Android phone.

The GitHub runner uses JDK 17 and Gradle 8.9. No Android Studio is required on your computer.

## Important status

This is the first buildable foundation, not yet the finished feature-complete player. The folders tab and equalizer tab are currently UI placeholders. Synchronized lyrics (LRC/online lookup), full folder browsing, equalizer/Bass Boost, sleep timer, backup/restore, headset action handling, and a full Now Playing screen still need implementation and device testing.

The project uses `minSdk 26` (Android 8.0). Media library access is requested at runtime. Android 13+ uses `READ_MEDIA_AUDIO`; older supported versions use `READ_EXTERNAL_STORAGE`.

## Package

`com.noirmusic.app`

## Version

`1.0.0` (debug)
