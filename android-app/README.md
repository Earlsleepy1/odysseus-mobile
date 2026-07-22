# Odysseus Mobile for Android

This is a native Android client for the Odysseus server. It does not embed the website or use a WebView. The Android app owns the connection screen, authentication, chat list loading, chat screen, and message sending; the Python backend stays on your computer/server.

## Test with an Android emulator

1. Start Odysseus on the host computer at port `7000`.
2. Build the debug APK:

   ```bash
   ./gradlew :app:assembleDebug
   ```

3. Install `app/build/outputs/apk/debug/app-debug.apk` on the emulator.
4. Leave the default server address as `http://10.0.2.2:7000` and tap **Connect**. The native client will sign in and load the first chat.

`10.0.2.2` means the Android emulator's host computer. If the server uses another port, edit the address in the app.

## Test on a physical Android phone

1. Start Odysseus with LAN access enabled, using `APP_BIND=0.0.0.0` and port `7000`.
2. Make sure the phone and computer are on the same Wi-Fi network.
3. Find the computer's LAN address, such as `192.168.1.20`.
4. Install the APK, enter `http://192.168.1.20:7000`, and tap **Connect**.

Use HTTPS when the server is exposed outside your private network. The manifest currently allows HTTP so local emulator and LAN testing works.

The app remembers the last server address you entered. The current native test build supports login/setup, 2FA prompt, model/session loading, creating a first session, loading a chat screen, and sending chat messages over the Odysseus API.
