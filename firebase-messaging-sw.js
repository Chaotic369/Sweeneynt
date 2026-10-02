importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyAau9fKT3kxa17lJb4g0QIqlhipJvWXPMw",
  authDomain: "hunter-prey.firebaseapp.com",
  databaseURL: "https://hunter-prey-default-rtdb.firebaseio.com",
  projectId: "hunter-prey",
  storageBucket: "hunter-prey.firebasestorage.app",
  messagingSenderId: "533931279568",
  appId: "1:533931279568:web:26dc7f86194ecfaa73cad5"
});

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil(self.clients.claim()));

const messaging = firebase.messaging();

// Always show a visible notification with the real message text.
messaging.onBackgroundMessage((payload) => {
  const d = (payload && payload.data) || {};
  const title = d.title || "New message";
  const body = d.body || "New message";
  const id = d.msgId || String(Date.now());
  return self.registration.showNotification(title, {
    body: body,
    tag: "msg-" + id,          // unique per message so none get replaced/dropped
    renotify: true,
    icon: "icon-192.png",
    badge: "badge-96.png",
    vibrate: [200, 100, 200],
    requireInteraction: false,
    timestamp: d.ts ? Number(d.ts) : Date.now(),
    data: { room: d.room || "" }
  });
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      for (const c of list) {
        if ("focus" in c) return c.focus();
      }
      if (clients.openWindow) return clients.openWindow("./");
    })
  );
});
