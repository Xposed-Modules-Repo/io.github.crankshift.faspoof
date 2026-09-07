# Contributing

Everything a developer needs to work on this module. For what it does and how to install it,
see [README.md](README.md).

## Background: why the module exists

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

Fermata Auto ships its own Xposed module. On the setup this was developed against it did not
work even with Android Auto correctly in its scope — its hook class contains a single hooked
method and one string literal. This module is an independent, more complete implementation of
the same idea.

## How it works

Scoped to `com.google.android.projection.gearhead` only, the module hooks
`android.app.ApplicationPackageManager` and, for the target package:

- `getInstallerPackageName(String)` — returns `com.android.vending`. Deprecated, but some code
  paths still use it.
- `getInstallSourceInfo(String)` — rewrites the returned `InstallSourceInfo`: the initiating and
  installing package names become `com.android.vending`, `packageSource` becomes 0, and a null
  initiator `SigningInfo` is filled in with the Play Store's real signing info. The originating
  package is left alone, because a genuine Play install leaves it null.

Because the rewrite is unconditional rather than keyed on a particular installer, it does not
matter how the target app was installed.

`InstallSourceInfo`'s field names are private API, so they are matched by name fragment
(`initiating`, `installing`, `packagesource`) rather than exact name. If a future Android
version renames them all, the module falls back to rewriting every non-null String field.
For the same reason, the android.jar constants the hook needs are inlined rather than imported.

## Project layout

| Path | What it is |
| --- | --- |
| `src/io/github/crankshift/faspoof/Main.java` | The whole module: one `IXposedHookLoadPackage`. |
| `stub/` | Minimal Xposed API declarations, compiled against but not shipped. |
| `assets/xposed_init` | Names the entry-point class. |
| `AndroidManifest.xml` | Xposed metadata, including the `xposedscope` array reference. |
| `res/values/arrays.xml` | The scope list the manager pre-ticks. |
| `build.sh` | The build. |

## Build

No Gradle and no system JDK. `build.sh` uses the JDK bundled with Android Studio and the
Android SDK build-tools:

```sh
./build.sh
```

It produces `FermataAutoEnabler.apk`, signed with a local key generated on first run and kept
at `ks.jks` (gitignored — keep it, or updates will fail to install with a signature mismatch).

The paths at the top of the script are hardcoded for macOS with Android Studio. On another
machine, edit `JAVA_HOME`, `SDK`, the build-tools version and the platform version to match.

The Xposed API is supplied by the framework at runtime. Rather than depend on a published API
jar, `stub/` contains minimal declarations compiled against but deliberately excluded from the
dex output. Their signatures must match the real API exactly — in particular
`XposedHelpers.findAndHookMethod` must return `XC_MethodHook.Unhook`, or the call will not
resolve at runtime.

After touching the stubs, verify they did not leak into the build:

```sh
dexdump -f build/classes.dex | grep -i "class descriptor"
```

Only `io.github.crankshift.faspoof` classes should be listed.

## Changing the target package

The target is a constant in `src/io/github/crankshift/faspoof/Main.java`:

```java
private static final String TARGET = "me.aap.fermata.auto.dear.google.why";
```

Change it to spoof a different app, or add more packages if you sideload several, then rebuild.

## Testing a change

There is no test suite. The module is only observable on a rooted device: build, install,
restart Android Auto, and read the log.

```sh
adb install -r FermataAutoEnabler.apk
adb shell am force-stop com.google.android.projection.gearhead
adb logcat | grep FASpoof
```

A successful load prints `hooks installed in <process>` for each Android Auto process. Hook
failures are logged rather than thrown, so an absent line means the hook did not install.

## Releasing

A release is the only thing Obtainium and the README download link can see, so its shape is part
of the interface:

1. Bump `android:versionCode` (integer, +1) and `android:versionName` in `AndroidManifest.xml`.
2. `./build.sh`. The APK must be signed with the same `ks.jks` as every earlier release, or the
   update fails to install with a signature mismatch.
3. Tag `<versionCode>-<versionName>` — `2-1.1`, and so on. That is the LSPosed module repository
   convention and the rest of this depends on it.
4. Publish a full GitHub release: not a draft, not a pre-release, with exactly one asset named
   `FermataAutoEnabler.apk`.

Obtainium reads the tag as the version string. The deep link in [README.md](README.md) carries a
`versionExtractionRegEx` of `^\d+-(.+)$` with `matchGroupToUse` `$1`, which reduces `2-1.1` to
`1.1` — the `versionName` the installed APK reports — so Obtainium can tell installed from
latest instead of offering an endless update. Drafts and pre-releases are skipped by default, and
a second APK asset would make `preferredApkIndex: 0` pick the wrong file.

Changing the tag format, the asset name or the package name means regenerating the link. It is
`obtainium://app/` followed by the URL-encoded form of:

```json
{
  "id": "io.github.crankshift.faspoof",
  "url": "https://github.com/Xposed-Modules-Repo/io.github.crankshift.faspoof",
  "author": "Xposed-Modules-Repo",
  "name": "Fermata Auto Enabler",
  "preferredApkIndex": 0,
  "additionalSettings": "{\"versionExtractionRegEx\":\"^\\\\d+-(.+)$\",\"matchGroupToUse\":\"$1\",\"versionDetection\":true,\"apkFilterRegEx\":\"\\\\.apk$\",\"about\":\"Xposed module that makes sideloaded Fermata Auto visible in Android Auto.\"}"
}
```

`additionalSettings` is a JSON string inside the JSON, not a nested object; Obtainium calls
`jsonDecode` on it a second time and rejects the import if it is an object. Encode the whole
thing with `python3 -c 'import json,urllib.parse,sys; print(urllib.parse.quote(sys.stdin.read().strip(), safe=""))'`
and paste the result after `obtainium://app/`.
