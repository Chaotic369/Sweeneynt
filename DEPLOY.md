# Deploying the push function (no GitHub needed)

Use this if the GitHub "Deploy push function" workflow fails, or you just want it done.
It runs in Google Cloud Shell, which is already signed in as you.

1. Open https://shell.cloud.google.com and sign in with the Google account that owns the `hunter-prey` Firebase project.
2. Paste these lines, one block at a time:

```
git clone https://github.com/Chaotic369/Sweeneynt.git
cd Sweeneynt/functions
npm install
cd ..
firebase deploy --only functions --project hunter-prey
```

3. If it asks to enable an API (Cloud Build, Eventarc, Cloud Run), answer `y`.
4. When it ends with "Deploy complete!", the function is live.

To update later: `cd Sweeneynt && git pull && firebase deploy --only functions --project hunter-prey`

## Checking it works
Firebase console -> Functions -> `notifyHunter` -> Logs.
Send a message as prey; you should see `notify room=... devices=1 took=...ms`.

## Needs
- Blaze (pay as you go) plan on the project.
- Realtime Database rule allowing writes to `hunterDevices/$id` (see chat instructions).
