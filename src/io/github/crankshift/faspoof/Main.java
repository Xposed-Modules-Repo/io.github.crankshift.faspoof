package io.github.crankshift.faspoof;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reports the Play Store as install initiator for Fermata Auto, but only inside the
 * Android Auto process.
 *
 * Fermata is installed with `pm install -i com.android.vending`, which sets the installing
 * package but leaves the initiating package as com.android.shell. Android Auto reads the
 * initiator and hides the app. packages.xml carries fs-verity on Android 16 and cannot be
 * patched, so the value is corrected in-process instead.
 */
public class Main implements IXposedHookLoadPackage {

    private static final String TAG = "[FASpoof] ";

    private static final String ANDROID_AUTO = "com.google.android.projection.gearhead";
    private static final String TARGET = "me.aap.fermata.auto.dear.google.why";
    private static final String PLAY_STORE = "com.android.vending";
    private static final String SHELL = "com.android.shell";

    /** PackageInstaller.SessionParams.PACKAGE_SOURCE_UNSPECIFIED, what a real Play install has. */
    private static final int PACKAGE_SOURCE_PLAY = 0;

    /** PackageManager.GET_SIGNING_CERTIFICATES, inlined to avoid an android.jar dependency. */
    private static final int GET_SIGNING_CERTIFICATES = 0x08000000;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ANDROID_AUTO.equals(lpparam.packageName)) {
            return;
        }

        Class<?> pmImpl;
        try {
            pmImpl = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "could not find ApplicationPackageManager: " + t);
            return;
        }

        hookInstallerPackageName(pmImpl);
        hookInstallSourceInfo(pmImpl);
        XposedBridge.log(TAG + "hooks installed in " + lpparam.processName);
    }

    /** The deprecated single-string API. Some code paths still use it. */
    private void hookInstallerPackageName(Class<?> pmImpl) {
        try {
            XposedHelpers.findAndHookMethod(pmImpl, "getInstallerPackageName", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (isTarget(param)) {
                                param.setResult(PLAY_STORE);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "getInstallerPackageName hook failed: " + t);
        }
    }

    /** The modern API, and the one that exposes the initiating package. */
    private void hookInstallSourceInfo(Class<?> pmImpl) {
        try {
            XposedHelpers.findAndHookMethod(pmImpl, "getInstallSourceInfo", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!isTarget(param)) {
                                return;
                            }
                            Object info = param.getResult();
                            if (info != null) {
                                rewrite(info, param.thisObject);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + "getInstallSourceInfo hook failed: " + t);
        }
    }

    private static boolean isTarget(XC_MethodHook.MethodHookParam param) {
        Object[] args = param.args;
        return args != null && args.length > 0 && TARGET.equals(args[0]);
    }

    /**
     * Rewrites InstallSourceInfo in place so it looks like a Play Store install, whatever
     * actually performed the install.
     *
     * Fields are matched by name fragment rather than exact name, because they are private API
     * and have moved between Android versions. If no field matches by name, every non-null
     * String field is rewritten instead, which is cruder but version-proof.
     */
    private static void rewrite(Object info, Object packageManager) {
        boolean matchedByName = false;
        Object playSigningInfo = playSigningInfo(packageManager);

        for (Field field : info.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String name = field.getName().toLowerCase();
            try {
                field.setAccessible(true);
                if (field.getType() == String.class
                        && (name.contains("initiating") || name.contains("installing"))) {
                    // Deliberately not touching the originating package: a genuine Play install
                    // leaves it null.
                    field.set(info, PLAY_STORE);
                    matchedByName = true;
                } else if (field.getType() == int.class && name.contains("packagesource")) {
                    field.setInt(info, PACKAGE_SOURCE_PLAY);
                } else if (playSigningInfo != null
                        && field.getType().getName().endsWith("SigningInfo")
                        && field.get(info) == null) {
                    field.set(info, playSigningInfo);
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + "could not rewrite " + field.getName() + ": " + t);
            }
        }

        if (!matchedByName) {
            rewriteEveryNonNullString(info);
        }
    }

    private static void rewriteEveryNonNullString(Object info) {
        for (Field field : info.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (field.get(info) != null) {
                    field.set(info, PLAY_STORE);
                }
            } catch (Throwable ignored) {
                // best effort
            }
        }
    }

    /**
     * The Play Store's own SigningInfo, so the initiator's signatures match its name.
     * Read reflectively to avoid compiling against android.jar.
     */
    private static Object playSigningInfo(Object packageManager) {
        try {
            Method getPackageInfo = packageManager.getClass()
                    .getMethod("getPackageInfo", String.class, int.class);
            Object info = getPackageInfo.invoke(packageManager, PLAY_STORE, GET_SIGNING_CERTIFICATES);
            return info.getClass().getField("signingInfo").get(info);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "could not read Play Store signing info: " + t);
            return null;
        }
    }
}
