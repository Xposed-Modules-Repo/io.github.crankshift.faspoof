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

- Root, and an Xposed framework: Vector, LSPosed, or equivalent.
- Fermata Auto installed.
- Android Auto with developer mode on and **Unknown sources** enabled
  (Android Auto settings → tap the version 10 times → Developer settings).
  This module does not replace that setting; both are needed.

## Install

### With Obtainium

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](obtainium://app/%7B%22id%22%3A%22io.github.crankshift.faspoof%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FXposed-Modules-Repo%2Fio.github.crankshift.faspoof%22%2C%22author%22%3A%22crankshift%22%2C%22name%22%3A%22Fermata%20Auto%20Enabler%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5E%5C%5C%5C%5Cd%2B-%28.%2B%29%24%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Xposed%20module%20that%20makes%20sideloaded%20Fermata%20Auto%20visible%20in%20Android%20Auto.%5C%22%7D%22%7D)

### By hand

Install the module from your manager's module repository, or download the APK from the
[latest release](https://github.com/Xposed-Modules-Repo/io.github.crankshift.faspoof/releases/latest)
and install it:

```sh
adb install -r FermataAutoEnabler.apk
```

## Enable

Enable the module and limit its scope to Android Auto. In a manager UI, tick only
Android Auto. With the Vector CLI:

```sh
su -c "/data/adb/lspd/cli modules enable io.github.crankshift.faspoof"
su -c "/data/adb/lspd/cli scope set io.github.crankshift.faspoof com.google.android.projection.gearhead/0"
```

Restart Android Auto and confirm the hooks loaded:

```sh
adb shell am force-stop com.google.android.projection.gearhead
adb logcat | grep FASpoof
```

Expected:

```
[FASpoof] hooks installed in com.google.android.projection.gearhead:projection
[FASpoof] hooks installed in com.google.android.projection.gearhead:shared
[FASpoof] hooks installed in com.google.android.projection.gearhead:car
```

Fermata Auto should then appear in Android Auto settings → Customize launcher.

## Scope

Keep this module scoped to Android Auto and nothing else. A wider scope would make other apps
believe the target came from the Play Store, which is both unnecessary and a bad idea.

## Spoofing a different app

The target package is a compile-time constant, so a different app needs a rebuild. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).
