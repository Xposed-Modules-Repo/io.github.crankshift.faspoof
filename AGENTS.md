# AGENTS.md

An Xposed module for Android Auto: one Java class, no Gradle, no test suite.
Read [CONTRIBUTING.md](CONTRIBUTING.md) before changing the build, the stubs, or the hook.

## Build

`./build.sh` is the only build. It hardcodes macOS Android Studio JDK and SDK paths; on another
machine, edit `JAVA_HOME`, `SDK` and the tool versions at the top of the script.

## Verification

Correctness is observable only on a rooted device: install, restart Android Auto, read
`adb logcat | grep FASpoof`. A clean build says nothing about whether the hook works, so report
an unverified change as unverified.

## Constraints

- `stub/` declares the Xposed API that the framework supplies at runtime. Compile against it,
  keep it out of the dex. Signatures must match the real API exactly — `findAndHookMethod`
  returns `XC_MethodHook.Unhook`. After editing a stub, confirm what shipped with
  `dexdump -f build/classes.dex | grep -i "class descriptor"`: only `io.github.crankshift.faspoof`
  classes belong there.
- `Main.java` matches private `InstallSourceInfo` fields by name fragment and inlines the
  android.jar constants it needs. Both are deliberate version-proofing; keep them.
- The Android Auto package name appears in three places that must agree: `res/values/arrays.xml`,
  the `xposedscope` meta-data in `AndroidManifest.xml`, and the `ANDROID_AUTO` constant in
  `Main.java`.
- Keep `ks.jks`. It is gitignored and generated on first build; a different key makes updates
  fail to install with a signature mismatch.
- `build/`, `*.apk` and `*.idsig` are gitignored build output. Leave them untracked.
