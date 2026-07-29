# Jenkins Daily Loads

A minimal, mobile-first view of the current day's vehicle loads in Google Sheets.

## Run locally

Requires Node.js 20 or newer.

```bash
npm install
npm run dev
```

Sign in with the Google account configured for the Firebase project. The app finds
`YYYY-MM-DD_LOAD Sheet` using the phone's local date, reads vehicle rows from
`Sheet1`, and writes completion changes back immediately.

Use `npm run lint` to type-check and `npm run build` to create the production bundle.
