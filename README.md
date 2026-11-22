# Glass Browser (Android)
1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
2. Pick a device or emulator and press Run.

VPN: tap "VPN off", enter a proxy (socks5://host:1080 or host:port), tap Connect.
This routes browser traffic only. A device-wide VPN needs Android's VpnService (next step).

## Build the APK without Android Studio
1. Create a free GitHub repo and upload everything in this folder (keep the .github folder).
2. Open the repo's Actions tab, run "Build APK" (it also runs on every push).
3. When it finishes, download the "glass-browser-apk" artifact, unzip it, and send app-debug.apk to your phone.
4. On the phone, open the APK and allow "Install unknown apps" for your file manager or browser.
