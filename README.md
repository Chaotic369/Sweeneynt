# Sweeney – hunter push notifications (Android)

Hunter gets a push when the prey sends a message, even if Chrome is closed.
Messages are held up to 6 hours if the phone is off/offline, then delivered when it reconnects.

## One-time setup
1. Firebase console → project `hunter-prey` → upgrade to **Blaze** plan (required for Cloud Functions; usage here is far inside the free quota).
2. Realtime Database must be in `us-central1` (URL ends `firebaseio.com`) – matches the function region.
3. `npm i -g firebase-tools && firebase login`
4. `cd functions && npm install && cd ..`
5. `firebase deploy` (deploys hosting + function). Site must be served over HTTPS.
6. Database rules must let the app write `rooms/<room>/push/hunter` and must NOT let anyone read it.

## On the hunter's Android phone
1. Open the site in Chrome, choose Hunter, enter PIN, tap **Allow** on the notification prompt.
2. Chrome menu → **Add to Home screen / Install app**.
3. Settings → Apps → Chrome (and the installed app) → Battery → **Unrestricted**. Don't force-stop Chrome.

## Limits
- Background alerts show the system default sound (custom chimes only play while the app is open).
- One hunter device per room (latest login wins).

## Deploying the function without GitHub
See DEPLOY.md (Google Cloud Shell, 4 commands).

## Native Android app (fast, reliable alerts)
Folder `android/`. It only receives pushes and shows the real message text; it does not
depend on the website UI, so you can change the site freely.
1. Firebase console -> Project settings -> Add app -> Android. Package name: `com.sweeney.hunter`.
2. Download `google-services.json` and put it at `android/app/google-services.json`, commit and push.
3. GitHub -> Actions -> "Build Android APK" -> Run workflow -> download artifact `SweeneyHunter-apk`.
4. Install the APK on the hunter phone, open it, allow notifications, tap "Allow background (battery)".
5. Database rule: allow write (not read) on `hunterDevices/$id`. Redeploy the function.
6. Turn the browser hunter option off on that phone to avoid double alerts (the function already prefers the native app).
Change `SITE_URL` in `android/app/src/main/java/com/sweeney/hunter/Config.java` if your site URL differs.
