# Location Tracker (personal-use, self-tracking app)

A minimal Android app for **your own phone**. It runs a foreground service that
gets a GPS location fix roughly every 60 seconds — including while the app is
backgrounded — and pushes each reading to a free [ntfy.sh](https://ntfy.sh)
topic so you can watch it from a browser or another device.

This app is intended to track the device it's installed on, with your own
knowledge and consent — it always shows a persistent notification while
tracking is active (Android requires this for any background-location app).
It is not designed to run hidden or to track a device you don't control.

## 1. Build the APK

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open Android Studio → **Open** → select this `LocationTracker` folder.
3. Let Gradle sync (first time takes a few minutes, needs internet).
4. Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. When it finishes, click **locate** in the notification, or find the file at:
   `app/build/outputs/apk/debug/app-debug.apk`

## 2. Install it on your phone

1. Copy `app-debug.apk` to your phone (USB cable, email to yourself, Google
   Drive, etc.) and open it there.
2. Android will warn about installing from an unknown source — this is
   expected for any app not from the Play Store. Go to **Settings → Security**
   (or the prompt that appears) and allow installation for that source.
3. Open the app.

## 3. Grant permissions

The app will ask for:
- **Notifications** (Android 13+) — needed to show the "tracking active" notice.
- **Location "While using the app"** — tap Allow.
- **Location "All the time"** — on the next screen it opens Settings; choose
  **Allow all the time** so tracking keeps working when the app isn't open.

For best reliability, also exclude the app from battery optimization:
**Settings → Apps → Location Tracker → Battery → Unrestricted**.

## 4. Pick a topic name and start tracking

1. In the app, type a topic name — make it long and hard to guess, e.g.
   `my-phone-loc-x7q2f9`, since anyone who knows the exact topic name can
   read updates published to it.
2. Tap **Start Tracking**. You'll see a persistent notification confirming
   it's running, with the most recent coordinates.

## 5. View your location on your computer

Open this in a browser on your PC (replace with your topic name):

```
https://ntfy.sh/my-phone-loc-x7q2f9
```

Leave that tab open and updates will appear live, or install the free
[ntfy desktop/mobile app](https://ntfy.sh/) and subscribe to the same topic
for push notifications.

## Notes / limitations

- Accuracy depends on GPS signal — expect a few meters outdoors, much worse
  indoors or in dense cities.
- Android's battery-saving features (Doze mode) may delay updates if the
  phone sits still and unplugged for a long time; the battery-optimization
  step above minimizes this but won't eliminate it entirely.
- This is a student/personal project, not a hardened production app —
  there's no login on the ntfy topic, so treat the topic name like a
  password and don't share it.
- Update interval (60 seconds) is set in `LocationService.kt`
  (`requestLocationUpdates` calls) if you want it faster/slower.
