# Fermata Auto Enabler

An Xposed module that makes sideloaded [Fermata Auto](https://github.com/AndreyPavlenko/Fermata)
visible in Android Auto.

Android Auto hides apps that did not come from the Play Store. This module reports the Play
Store as Fermata Auto's install source, but only inside the Android Auto process. Every other
app on the device still sees the true install source, and it does not matter how Fermata Auto
was installed — `pm install`, a file manager, or anything else.

Tested on Android 16 (SDK 36) with Android Auto 17.5 and the Vector framework (API 102). It
should work on LSPosed too; it uses only the classic Xposed API.

## Requirements

- Root, and an Xposed framework: [Vector](https://github.com/JingMatrix/Vector/releases),
  LSPosed, or equivalent.
- [Fermata Auto](https://github.com/AndreyPavlenko/Fermata/releases) installed.
- Android Auto with developer mode on and **Unknown sources** enabled
  (Android Auto settings → tap the version 10 times → Developer settings).
  This module does not replace that setting; both are needed.

## Install

### With Obtainium

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](obtainium://app/%7B%22id%22%3A%22io.github.crankshift.faspoof%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FXposed-Modules-Repo%2Fio.github.crankshift.faspoof%22%2C%22author%22%3A%22crankshift%22%2C%22name%22%3A%22Fermata%20Auto%20Enabler%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5E%5C%5C%5C%5Cd%2B-%28.%2B%29%24%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Xposed%20module%20that%20makes%20sideloaded%20Fermata%20Auto%20visible%20in%20Android%20Auto.%5C%22%7D%22%7D)

### By hand

1. Open the [latest release](https://github.com/Xposed-Modules-Repo/io.github.crankshift.faspoof/releases/latest)
   and download `FermataAutoEnabler.apk`.
2. Tap the downloaded file and allow the install.

The module is also in the module repository built into LSPosed and Vector, if you would rather
install it from there.

## Enable

1. Open your Xposed manager — Vector, LSPosed, or equivalent.
2. Find **Fermata Auto Enabler** in the module list and switch it on.
3. Open the module's scope and tick **Android Auto**, and nothing else.
4. Force stop Android Auto so it restarts with the hook in place: Android settings → Apps →
   Android Auto → Force stop. A reboot does the same.
5. Open Android Auto settings → Customize launcher. Fermata Auto is now in the list; tick it.

If Fermata Auto is still missing, check that **Unknown sources** is on in Android Auto's
developer settings — this module does not replace it. To confirm the module loaded at all, see
the log check in [CONTRIBUTING.md](CONTRIBUTING.md).

## Scope

Keep this module scoped to Android Auto and nothing else. A wider scope would make other apps
believe the target came from the Play Store, which is both unnecessary and a bad idea.

## Spoofing a different app

The target package is a compile-time constant, so a different app needs a rebuild. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).
