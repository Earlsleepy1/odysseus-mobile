# Odysseus Mobile for Android

This is a lightweight Android app wrapper for the Odysseus web workspace. It includes the mobile UI changes from the main project and connects to an Odysseus server over HTTP or HTTPS. The Python backend stays on your computer/server; the Android app is the mobile client.

## Test with an Android emulator

1. Start Odysseus on the host computer at port `7000`.
2. Build the debug APK:

   ```bash
   ./gradlew :app:assembleDebug
   ```

3. Install `app/build/outputs/apk/debug/app-debug.apk` on the emulator.
4. Leave the default server address as `http://10.0.2.2:7000` and tap **Connect**.

`10.0.2.2` means the Android emulator's host computer. If the server uses another port, edit the address in the app.

## Test on a physical Android phone

1. Start Odysseus with LAN access enabled, using `APP_BIND=0.0.0.0` and port `7000`.
2. Make sure the phone and computer are on the same Wi-Fi network.
3. Find the computer's LAN address, such as `192.168.1.20`.
4. Install the APK, enter `http://192.168.1.20:7000`, and tap **Connect**.

Use HTTPS when the server is exposed outside your private network. The manifest currently allows HTTP so local emulator and LAN testing works.

The app remembers the last server address you entered.
