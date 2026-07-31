# Jenkins Daily Loads

A native Android app and mobile web view for the current day's vehicle loads in Google Sheets.

## Android app

The Android application lives in `app/` and uses package name
`com.jenkinstowing.dailyloads`. It signs in with Google, finds the phone-local
`YYYY-MM-DD_LOAD Sheet`, reads vehicle rows from `Sheet1`, and writes completion
changes directly to the matching sheet row.

The GitHub Actions **Build Android APK** workflow builds a debug APK for every
pull request and uploads it as the `Jenkins-Daily-debug` artifact.

Before distributing a signed release, add an Android OAuth client for
`com.jenkinstowing.dailyloads` and the signing certificate SHA-1 to the same
Google Cloud project used by the load sheets.

To build with a local Gradle 8.11.1 installation:

```bash
gradle :app:assembleDebug
```

## Mobile web app

Requires Node.js 20 or newer.

```bash
npm install
npm run dev
```

Sign in with the Google account configured for the Firebase project. The app finds
`YYYY-MM-DD_LOAD Sheet` using the phone's local date, reads vehicle rows from
`Sheet1`, and writes completion changes back immediately.

Use `npm run lint` to type-check and `npm run build` to create the production bundle.
