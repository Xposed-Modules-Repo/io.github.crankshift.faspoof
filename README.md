# Fermata Auto Enabler

An Xposed module that makes sideloaded [Fermata Auto](https://github.com/AndreyPavlenko/Fermata)
visible in Android Auto, by reporting the Play Store as its install source — but only inside the
Android Auto process.

Tested on Android 16 (SDK 36) with Android Auto 17.5 and the Vector framework (API 102).
It should work on LSPosed too; it uses only the classic Xposed API.

## The problem

Android Auto hides apps that were not installed from the Play Store. The usual workaround is:

```
pm install -i com.android.vending app.apk
```

That is not enough on current Android Auto. `-i` sets only the *installing* package. Android
installs also record an *initiating* package, and installing from a shell leaves it as
`com.android.shell`:

```
$ dumpsys package me.aap.fermata.auto.dear.google.why
    installerPackageName=com.android.vending      # spoofed
    initiatingPackageName=com.android.shell       # not spoofed
    packageSource=1                               # a real Play install has 0
```

Compare a genuine Play Store install:

```
    installerPackageName=com.android.vending
    initiatingPackageName=com.android.vending
    packageSource=0
    <install-initiator-sigs>                      # the initiator's signatures
```

There is no `pm` flag that sets the initiating package. It is written by the system at install
time, from the identity of whatever created the install session.

Patching the record afterwards does not work either. On Android 16, `/data/system/packages.xml`
is protected by fs-verity:

```
$ lsattr -l /data/system/packages.xml
/data/system/packages.xml    Encrypted, Verity
```

fs-verity seals a file read-only at the kernel level for every UID, including root. Writes
return `EPERM` while `chmod`, `chown` and `chcon` still succeed, which makes the failure
confusing at first. Neighbouring files such as `packages.list` are not sealed.

So the value cannot be changed on disk, and cannot be set correctly at install time. It has to
be corrected in memory, in the only process that cares.

## What this module does

Scoped to `com.google.android.projection.gearhead` only, it hooks
`android.app.ApplicationPackageManager` and, for the target package:

- `getInstallerPackageName(String)` — returns `com.android.vending`.
- `getInstallSourceInfo(String)` — rewrites the returned `InstallSourceInfo`: the initiating and
  installing package names become `com.android.vending`, `packageSource` becomes 0, and a null
  initiator `SigningInfo` is filled in with the Play Store's real signing info. The originating
  package is left alone, because a genuine Play install leaves it null.

Nothing outside the Android Auto process is affected. Every other app on the device still sees
the true install source.

Because the rewrite is unconditional rather than keyed on a particular installer, **it does not
matter how the target app was installed** — `pm install`, a file manager, or anything else.

`InstallSourceInfo`'s field names are private API, so they are matched by name fragment
(`initiating`, `installing`, `packagesource`) rather than exact name. If a future Android
version renames them all, the module falls back to rewriting every non-null String field.

## Configuration

The target package is a constant in `src/com/crankshift/faspoof/Main.java`:

```java
private static final String TARGET = "me.aap.fermata.auto.dear.google.why";
```

Change it to spoof a different app, or add more packages if you sideload several.

## Requirements

- Root, and an Xposed framework: Vector, LSPosed, or equivalent.
- The target app installed.
- Android Auto with developer mode on and **Unknown sources** enabled
  (Android Auto settings → tap the version 10 times → Developer settings).
  This module does not replace that setting; both are needed.

## Build

No Gradle and no system JDK required. `build.sh` uses the JDK bundled with Android Studio and
the Android SDK build-tools:

```sh
./build.sh
```

It produces `FermataAutoEnabler.apk`, signed with a local key generated on first run and kept
at `ks.jks` (gitignored — keep it, or updates will fail to install with a signature mismatch).

The Xposed API is supplied by the framework at runtime. Rather than depend on a published API
jar, `stub/` contains minimal declarations compiled against but deliberately excluded from the
dex output. Their signatures must match the real API exactly — in particular
`XposedHelpers.findAndHookMethod` must return `XC_MethodHook.Unhook`, or the call will not
resolve at runtime.

Verify the stubs did not leak into the build:

```sh
dexdump -f build/classes.dex | grep -i "class descriptor"
```

Only `com.crankshift.faspoof` classes should be listed.

## Install

```sh
adb install -r FermataAutoEnabler.apk
```

Then enable the module and limit its scope to Android Auto. In a manager UI, tick only
Android Auto. With the Vector CLI:

```sh
su -c "/data/adb/lspd/cli modules enable com.crankshift.faspoof"
su -c "/data/adb/lspd/cli scope set com.crankshift.faspoof com.google.android.projection.gearhead/0"
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

The app should then appear in Android Auto settings → Customize launcher.

## Notes

Fermata Auto ships its own Xposed module. On the setup this was developed against it did not
work even with Android Auto correctly in its scope — its hook class contains a single hooked
method and one string literal. This module is an independent, more complete implementation of
the same idea.

Scope this module to Android Auto and nothing else. Giving it a wider scope would make other
apps believe the target came from the Play Store, which is both unnecessary and a bad idea.

## License

MIT. See [LICENSE](LICENSE).
