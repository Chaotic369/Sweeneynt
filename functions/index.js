const { onValueCreated } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

admin.initializeApp();

const DEAD_TOKEN_CODES = [
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument"
];

function previewOf(msg) {
  switch (msg.type) {
    case "text":
      return String(msg.text || "").slice(0, 300) || "New message";
    case "voice":
      return "🎤 Voice message";
    case "media":
      if (msg.mediaType === "image") return "📷 Photo";
      if (msg.mediaType === "video") return "🎥 Video";
      return "📎 " + (msg.fileName || "File");
    default:
      return "New message";
  }
}

// Fires for EVERY room, whenever the prey sends anything.
// Sends to every hunter device registered globally (/hunterDevices) and,
// as a fallback, to the token stored in that room (/rooms/<room>/push/hunter).
exports.notifyHunter = onValueCreated(
  {
    ref: "/rooms/{room}/messages/{msgId}",
    region: "us-central1",
    memory: "256MiB",
    timeoutSeconds: 30,
    maxInstances: 20,
    // Keep one instance always warm: removes the multi-second cold start.
    minInstances: 1,
    // If the function throws (transient FCM error), Firebase retries instead of dropping the alert.
    retry: true
  },
  async (event) => {
    const msg = event.data.val();
    if (!msg || msg.type === "system" || msg.sender !== "prey") return null;
    const startedAt = Date.now();

    const db = admin.database();
    const room = String(event.params.room);

    // Gather tokens (global devices + room token), de-duplicated.
    const [devSnap, roomSnap, nameSnap] = await Promise.all([
      db.ref("/hunterDevices").get(),
      db.ref(`/rooms/${room}/push/hunter`).get(),
      db.ref(`/rooms/${room}/settings/names/prey`).get()
    ]);

    const targets = new Map(); // token -> cleanup ref
    const nativeTargets = new Map();
    if (devSnap.exists()) {
      devSnap.forEach((child) => {
        const v = child.val() || {};
        if (!v.token) return;
        targets.set(v.token, child.ref);
        if (v.type === "native") nativeTargets.set(v.token, child.ref);
      });
    }
    if (roomSnap.exists() && roomSnap.val().token) {
      const t = roomSnap.val().token;
      if (!targets.has(t)) targets.set(t, roomSnap.ref);
    }
    // If the native Android app is installed, use only it (avoids double alerts
    // from the old browser registration on the same phone).
    if (nativeTargets.size) {
      targets.clear();
      nativeTargets.forEach((ref, t) => targets.set(t, ref));
    }
    if (!targets.size) return null;

    const sender = (nameSnap.exists() && String(nameSnap.val())) || "Prey";
    const body = previewOf(msg);

    let transientError = null;
    await Promise.all(
      [...targets.entries()].map(async ([token, cleanupRef]) => {
        try {
          // Data-only + high priority: the service worker builds the visible
          // notification, so it always has the real text.
          await admin.messaging().send({
            token,
            data: {
              room,
              msgId: String(event.params.msgId),
              title: sender,
              body,
              ts: String(Date.now())
            },
            android: { priority: "high", ttl: 6 * 60 * 60 * 1000 },
            webpush: { headers: { Urgency: "high", TTL: "21600" } }
          });
        } catch (err) {
          if (DEAD_TOKEN_CODES.includes(err.code)) {
            await cleanupRef.remove().catch(() => {});
          } else {
            console.error("FCM send failed", err);
            transientError = err;
          }
        }
      })
    );
    console.log(`notify room=${room} devices=${targets.size} took=${Date.now() - startedAt}ms`);
    if (transientError) throw transientError; // triggers retry; same tag means no duplicate shown
    return null;
  }
);
