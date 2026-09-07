#!/bin/bash
# Builds and signs the Fermata Auto Enabler Xposed module.
# Uses the JDK bundled with Android Studio; no system JDK is required.
set -euo pipefail

P="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$SDK/build-tools/37.0.0:$SDK/platform-tools:$PATH"
AJ="$SDK/platforms/android-36.1/android.jar"
B="$P/build"
OUT="$P/FermataAutoEnabler.apk"

rm -rf "$B"; mkdir -p "$B/stubcls" "$B/cls"

# The Xposed API is provided by the framework at runtime. Compile against local stubs and
# keep them off the classpath of the dex output so they are never shipped.
javac -nowarn --release 17 -d "$B/stubcls" $(find "$P/stub" -name '*.java')
javac -nowarn --release 17 -cp "$B/stubcls" -d "$B/cls" $(find "$P/src" -name '*.java')
d8 --min-api 28 --lib "$AJ" --classpath "$B/stubcls" --output "$B" $(find "$B/cls" -name '*.class')

aapt2 compile --dir "$P/res" -o "$B/res.zip"
aapt2 link -o "$B/base.apk" -I "$AJ" --manifest "$P/AndroidManifest.xml" -A "$P/assets" \
    --min-sdk-version 28 --target-sdk-version 36 "$B/res.zip"

( cd "$B" && zip -q base.apk classes.dex )
zipalign -p -f 4 "$B/base.apk" "$B/aligned.apk"

if [ ! -f "$P/ks.jks" ]; then
    keytool -genkeypair -keystore "$P/ks.jks" -alias faspoof \
        -storepass faspoof -keypass faspoof \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Fermata Auto Enabler, O=local" >/dev/null 2>&1
fi
apksigner sign --ks "$P/ks.jks" --ks-pass pass:faspoof --key-pass pass:faspoof \
    --out "$OUT" "$B/aligned.apk" 2>/dev/null

echo "built: $OUT"
