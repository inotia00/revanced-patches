package com.liskovsoft.sharedutils.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.util.Range;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.utils.Logger;

public final class Helpers {
    public static final int REMOVE_PACKAGE_CODE = 521;
    private static final String ARRAY_DELIM = "%AR%";
    private static final String DATA_DELIM = "%OB%";
    private static final String PAIR_DELIM = "%PR%";
    private static final String LEGACY_ARRAY_DELIM = "|";
    private static final String LEGACY_DATA_DELIM = ",";
    private static final String OBJ_DELIM = "&vi;";
    private static final String MIME_VP9 = "video/x-vnd.on2.vp9";
    private static final String MIME_AV1 = "video/av01";
    private static final Pattern URL_PREFIX = Pattern.compile("^[a-z.]+://.+$");
    private static Boolean sIsVP9Supported;
    private static Boolean sIsAV1Supported;
    private static int sVP9MaxHeight;
    private static int sAV1MaxHeight;
    private static long sCachedRamSize = -1;
    private static Random sRandom;
    // https://unicode-table.com/en/
    // https://www.compart.com/en/unicode/
    public static final String THUMB_UP = "\uD83D\uDC4D";
    public static final String THUMB_DOWN = "\uD83D\uDC4E";
    public static final String NON_BREAKING_SPACE = "\u00A0";
    public static final String SPEECH = "\uD83D\uDDE8";
    public static final String SPEAKER = "\uD83D\uDD08";
    //public static final String HOURGLASS = "⌛";
    public static final String HOURGLASS = "\u231B";

    /**
     * Simple wildcard matching routine. Implemented without regex. So you may expect huge performance boost.
     * @param host
     * @param mask
     * @return
     */
    public static boolean matchSubstr(String host, String mask) {
        String[] sections = mask.split("\\*");
        String text = host;
        for (String section : sections) {
            int index = text.indexOf(section);
            if (index == -1) {
                return false;
            }
            text = text.substring(index + section.length());
        }
        return true;
    }

    public static boolean matchSubstrNoCase(String host, String mask) {
        return matchSubstr(host.toLowerCase(), mask.toLowerCase());
    }

    public static <T> T[] appendArray(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }

    public static String encodeURI(byte[] data) {
        try {
            // make behaviour of java uri-encode the same as javascript's one
            return URLEncoder.encode(new String(data, "UTF-8"), "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getDeviceName() {
        return String.format("%s (%s)", Build.MODEL, Build.PRODUCT);
    }

    public static String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return release + " (" + sdkVersion +")";
    }

    public static int getDeviceDpi(Context context) {
        int dpi = 0;

        if (context != null && context.getResources() != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            dpi = displayMetrics != null ? displayMetrics.densityDpi : 0;
        }

        return dpi;
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).format(new Date());
    }

    public static boolean isGenymotion() {
        String deviceName = getDeviceName();

        return deviceName.contains("(vbox86p)");
    }

    public static boolean deviceMatch(String[] devicesToProcess) {
        String thisDeviceName = Helpers.getDeviceName();
        for (String deviceName : devicesToProcess) {
            boolean match = matchSubstrNoCase(thisDeviceName, deviceName);
            if (match) {
                return true;
            }
        }
        return false;
    }

    public static String toString(Throwable ex) {
        if (ex instanceof IllegalStateException &&
                ex.getCause() != null) {
            ex = ex.getCause();
        }

        String message = ex.getMessage();

        if (message == null || message.isEmpty()) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            message = errors.toString();
        }

        return String.format("%s: %s", ex.getClass().getCanonicalName(), message);
    }

    public static String toString(InputStream content) {
        return FileHelpers.toString(content);
    }

    public static String toString(Intent intent) {
        return dumpIntent(intent);
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }

        return obj.toString();
    }

    public static String toString(float num) {
        if (num % 1.0 != 0) {
            return String.valueOf(num);
        } else {
            return String.valueOf((int) num);
        }
    }

    public static String toIntString(Object floatOrIntString) {
        if (floatOrIntString == null) {
            return null;
        }

        float val = Float.parseFloat(String.valueOf(floatOrIntString));
        return String.valueOf((int) val);
    }

    public static InputStream toStream(String content) {
        return FileHelpers.toStream(content);
    }

    public static void postOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static String unixToLocalDate(Context ctx, String timestamp) {
        Locale current = ctx.getResources().getConfiguration().locale;
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, current);
        Date date;
        if (timestamp == null) {
            date = new Date();
        } else {
            date = new Date((long) Integer.parseInt(timestamp) * 1000);
        }
        return dateFormat.format(date);
    }

    public static String runMultiMatcher(String input, String... patterns) {
        if (input == null) {
            return null;
        }

        Pattern regex;
        Matcher matcher;
        String result = null;
        for (String pattern : patterns) {
            regex = Pattern.compile(pattern);
            matcher = regex.matcher(input);

            if (matcher.find()) {
                result = matcher.group(matcher.groupCount()); // get last group
                break;
            }
        }

        return result;
    }

    public static String runMultiMatcher(String input, Pattern... patterns) {
        if (input == null) {
            return null;
        }

        String result = null;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                result = matcher.group(matcher.groupCount()); // get last group
                break;
            }
        }

        return result;
    }

    public static boolean isCallable(Context ctx, Intent intent) {
        List<ResolveInfo> list = ctx.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * Format float and remove unneeded zeroes after dot
     */
    public static String formatFloat(double d) {
        // This is to show symbol . instead of ,
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
        // Define the maximum number of decimals (number of symbols #)
        DecimalFormat df = new DecimalFormat("#.##", otherSymbols);

        return df.format(d);
    }

    /**
     * Limit digits after dot
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Get scale for use in {@link android.webkit.WebView} instantiation
     * @param ctx context
     * @param picWidth constant that I knew beforehand
     * @return calculated scale
     */
    public static int getScale(Context ctx, int picWidth) {
        Point p = new Point();
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getSize(p);
        int width = p.x;
        Double val = (double) width / (double) picWidth;
        val = val * 100d;
        return val.intValue();
    }

    private static boolean isNaN(String num) {
        return num == null                                      ||
               num.isEmpty()                                    ||
               num.contains(" ")                                ||
               num.contains(";")                                ||
               num.contains("&")                                ||
               num.contains(",")                                ||
               num.contains(".")                                ||
               num.contains(":")                                ||
               num.contains("/")                                ||
               num.contains("\\")                               ||
               !Character.isDigit(num.charAt(num.length() - 1)) ||
               !Character.isDigit(num.charAt(0));
    }

    /**
     * Any number, e.g. -1.0, 15
     */
    public static boolean isNumeric(String s) {
        return s != null && s.matches("^[-+]?\\d*\\.?\\d+$");
    }

    public static boolean isInteger(String s) {
        return s != null && s.matches("^[-+]?\\d+$");
    }

    public static boolean hasWords(String s) {
        return s != null && s.matches("^.*[^\\d\\W]+.*$");
    }

    public static boolean hasDigits(String s) {
        return s != null && s.matches("^.*[-+]?\\d*\\.?\\d+.*$");
    }

    /**
     * Force normal font size regardless of the system settings
     * @param configuration app config
     * @param ctx activity
     */
    public static void adjustFontScale(Configuration configuration, Activity ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }

        configuration.fontScale = (float) 1.0; // normal size
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        ctx.getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public static <T> Map<String, T> convertToObj(String jsonContent) {
        Type type = new TypeToken<Map<String, T>>(){}.getType();
        Gson gson = new Gson();
        return gson.fromJson(jsonContent, type);
    }

    /**
     * Return true to first matched string from the array
     * @param fullStr full string
     * @param nameArr array to match
     * @return whether ended with arr
     */
    public static boolean endsWithAny(String fullStr, String... nameArr) {
        if (fullStr == null) {
            return false;
        }

        for (String name : nameArr) {
            if (name == null) {
                continue;
            }

            if (fullStr.endsWith(name)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMicAvailable(Context context) {
        PackageManager pm = context.getPackageManager();

        boolean isMicAvail = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);

        //boolean isLeanback = false;
        //
        //if (VERSION.SDK_INT >= 21) {
        //    // Android TV user likely have mics
        //    isLeanback = isAndroidTV(context) || isAmazonFireTVDevice();
        //}

        return isMicAvail || VERSION.SDK_INT >= 21;
    }

    public static boolean isAndroidTVLauncher(Context context) {
        return  isPackageExists(context, "com.amazon.tv.leanbacklauncher") || // port of the official Android TV launcher (https://github.com/tsynik/LeanbackLauncher)
                isPackageExists(context, "com.google.android.leanbacklauncher") ||
                isPackageExists(context, "com.google.android.tvlauncher") || // Android TV 10
                isPackageExists(context, "com.google.android.apps.tv.launcherx"); // Google TV Home
    }

    public static boolean isGoogleTVLauncher(Context context) {
        String pkgName = "com.google.android.apps.tv.launcherx";
        //int pkgVersionCode = 413515; // 557827
        //return isPackageExists(context, pkgName) && getPackageVersionCode(context, pkgName) >= pkgVersionCode;
        return isPackageExists(context, pkgName);
    }

    //public static boolean isAndroidTVRecommendations(Context context) {
    //    return isPackageExists(context, "com.google.android.leanbacklauncher.recommendations");
    //}

    public static boolean isATVChannelsSupported(Context context) {
        return VERSION.SDK_INT >= 26 && isAndroidTVLauncher(context);
    }

    public static boolean isATVRecommendationsSupported(Context context) {
        boolean isApiSupported = VERSION.SDK_INT >= 21 && VERSION.SDK_INT < 26;
        return isApiSupported && (isAndroidTVLauncher(context) || isAmazonFireTVDevice());
    }

    public static boolean isPictureInPictureSupported(Context context) {
        if (context == null) {
            return false;
        }

        return VERSION.SDK_INT >= 24 && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public static boolean isTouchSupported(Context context) {
        if (context == null) {
            return false;
        }

        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    public static boolean isAndroidTV(Context context) {
        PackageManager pm = context.getPackageManager();

        if (VERSION.SDK_INT <= 21) {
            return false;
        }

        return (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK));
    }

    public static String findFirst(String input, Pattern pattern) {
        String regExpVal = null;

        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                regExpVal = matcher.group(1);
            } else {
                regExpVal = matcher.group(0); // all match
            }
        }

        return regExpVal;
    }

    public static boolean matchAll(String input, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

    public static boolean matchAll(String input, String... regex) {
        for (String reg : regex) {
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

    public static void makeActivityFullscreen(Activity activity) {
        activity.getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);

        View decorView = activity.getWindow().getDecorView();

        if (VERSION.SDK_INT >= 19) {
            // https://developer.android.com/codelabs/gesture-navigation
            // Keep navigation bar in gesture mode to support gestures
            int hideNavigation = isEdgeToEdgeEnabled(activity) != 2 ? View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : 0;
            decorView.setSystemUiVisibility(hideNavigation | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * More advance approach. Including automobile systems support.
     */
    public static void makeActivityFullscreen2(Activity activity) {
        if (Build.VERSION.SDK_INT >= 30) {
            activity.getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            // https://developer.android.com/codelabs/gesture-navigation
            // Keep navigation bar in gesture mode to support gestures???
            //if (controller != null && isEdgeToEdgeEnabled(activity) != 2) {
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // https://developer.android.com/codelabs/gesture-navigation
            // Keep navigation bar in gesture mode to support gestures???
            //int hideNavigation = isEdgeToEdgeEnabled(activity) != 2 ? (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) : 0;
            int hideNavigation = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            int sticky = Build.VERSION.SDK_INT >= 19 ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : 0;
            activity.getWindow().getDecorView().setSystemUiVisibility(hideNavigation | sticky | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        if (VERSION.SDK_INT >= 19) {
            // Make status and nav bars transparent
            // <item name="android:windowTranslucentStatus">false</item>
            // <item name="android:windowTranslucentNavigation">false</item>
            activity.getWindow().setFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS, LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setFlags(LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    public static void addFullscreenListener(Activity activity) {
        activity.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                makeActivityFullscreen2(activity);
            }
        });
    }

    /**
     * 0 : Navigation is displaying with 3 buttons<br/>
     * 1 : Navigation is displaying with 2 button(Android P navigation mode)<br/>
     * 2 : Full screen gesture(Gesture on android Q)<br/>
     * <a href="https://developer.android.com/codelabs/gesture-navigation">tutorial</a>
     */
    private static int isEdgeToEdgeEnabled(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
        if (resourceId > 0) {
            return resources.getInteger(resourceId);
        }
        return 0;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void makeActivityHorizontal(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static boolean equalsAny(Integer orig, Integer... arr) {
        return equalsAny(orig, (Object[]) arr);
    }

    public static boolean equalsAny(String orig, String... arr) {
        return equalsAny(orig, (Object[]) arr);
    }

    public static boolean equalsAny(Object orig, Object... arr) {
        if (orig == null || arr == null) {
            return false;
        }

        for (Object item : arr) {
            if (orig.equals(item)) {
                return true;
            }
        }

        return false;
    }

    public static boolean equalsAny(int orig, int... arr) {
        if (arr == null) {
            return false;
        }

        for (int item : arr) {
            if (orig == item) {
                return true;
            }
        }

        return false;
    }

    public static boolean equals(Object first, Object second) {
        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        return first.equals(second);
    }

    public static boolean containsAny(String first, String... second) {
        if (second == null) {
            return false;
        }

        for (String item : second) {
            if (contains(first, item)) {
                return true;
            }
        }

        return false;
    }

    public static boolean contains(String first, String second) {
        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        first = normalize(first);
        second = normalize(second);

        return first.contains(second);
    }

    public static boolean startsWith(String word, String prefix) {
        if (word == null && prefix == null) {
            return true;
        }

        if (word == null || prefix == null) {
            return false;
        }

        word = normalize(word);
        prefix = normalize(prefix);

        return word.startsWith(prefix);
    }

    public static boolean startsWithAny(String word, String... prefixes) {
        if (word == null || prefixes == null) {
            return false;
        }

        for (String prefix : prefixes) {
            if (startsWith(word, prefix)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDash(String id) {
        if (!Helpers.isNumeric(id)) {
            return false;
        }

        int maxRegularITag = 50;
        int itag = Integer.parseInt(id);

        return itag > maxRegularITag;
    }

    public static BufferedReader exec(String... params) throws IOException {
        Process process = Runtime.getRuntime().exec(params);
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public static void enableButtonSounds(Context context, boolean enable) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, !enable);
    }

    /**
     * Find all packages starting for specified name
     * @param context ctx
     * @param pkgPrefix starts with
     * @return packages or empty list if not found
     */
    public static List<String> findPackagesByPrefix(Context context, String pkgPrefix) {
        List<String> pkgNames = new ArrayList<>();

        for (ApplicationInfo info : getInstalledPackages(context)) {
            if (info.packageName != null && info.packageName.startsWith(pkgPrefix)) {
                pkgNames.add(info.packageName);
            }
        }

        return pkgNames;
    }

    private static boolean isPackageExists(Context context, String pkgName) {
        PackageInfo packageInfo = getPackageInfo(context, pkgName);

        return packageInfo != null;
    }

    private static int getPackageVersionCode(Context context, String pkgName) {
        PackageInfo packageInfo = getPackageInfo(context, pkgName);
        return packageInfo != null ? packageInfo.versionCode : -1;
    }

    @Nullable
    private static PackageInfo getPackageInfo(Context context, String pkgName) {
        PackageManager manager = context.getPackageManager();
        PackageInfo packageInfo = null;

        try {
            packageInfo = manager.getPackageInfo(pkgName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            // NOP
        }

        return packageInfo;
    }

    public static void removePackage(Context context, String pkgName) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + pkgName));
        context.startActivity(intent);
    }

    public static void removePackageAndGetResult(Activity context, String pkgName) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + pkgName));
        context.startActivityForResult(intent, REMOVE_PACKAGE_CODE);
    }

    public static List<ApplicationInfo> getInstalledPackages(Context context) {
        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.

        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    public static List<String> getInstalledPackagesWithMainActivity(Context context) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities(mainIntent, 0);
        List<String> result = new ArrayList<>();
        for (ResolveInfo info : pkgAppsList) {
            result.add(info.activityInfo.packageName);
        }

        return result;
    }

    public static boolean isEmpty(Intent intent) {
        if (intent == null) {
            return true;
        }

        if (intent.getExtras() == null) {
            return true;
        }

        return intent.getExtras().isEmpty();
    }

    public static void mergeIntents(Intent mainIntent, Intent newIntent) {
        Bundle extras = mainIntent.getExtras();

        if (extras != null) {
            extras.putAll(newIntent.getExtras());
            mainIntent.putExtras(extras);
        }
    }

    public static void bringToBackOld(View child) {
        ViewGroup parent = (ViewGroup) child.getParent();
        if (parent != null && parent.indexOfChild(child) != 0) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    /**
     * Not working!!!
     */
    public static void bringToBack(View myCurrentView) {
        ViewGroup myViewGroup = ((ViewGroup) myCurrentView.getParent());
        int index = myViewGroup.indexOfChild(myCurrentView);
        for(int i = 0; i < index; i++) {
            myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));
        }
    }

    public static void enableScreensaver(Activity context) {
        if (context == null) {
            return;
        }

        context.runOnUiThread(() -> context.getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    public static void disableScreensaver(Activity context) {
        if (context == null) {
            return;
        }

        context.runOnUiThread(() -> {
            try {
                context.getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (ArrayIndexOutOfBoundsException e) { // A rare unknown crash (length=0; index=16)
                e.printStackTrace();
            }
        });
    }

    /**
     * Don't work. Maybe need WRITE_SETTINGS permission?
     */
    public static void setBrightness(Activity context, float level) {
        if (context == null) {
            return;
        }

        context.runOnUiThread(() -> {
            LayoutParams layoutParams = context.getWindow().getAttributes();
            layoutParams.screenBrightness = level;
            context.getWindow().setAttributes(layoutParams);
        });
    }

    /**
     * Utility method to check if device is Amazon Fire TV device
     * @return {@code true} true if device is Amazon Fire TV device.
     */
    public static boolean isAmazonFireTVDevice() {
        String deviceName = Build.MODEL;
        String manufacturerName = Build.MANUFACTURER;
        return (deviceName.startsWith("AFT")
                && "Amazon".equalsIgnoreCase(manufacturerName));
    }

    public static boolean isActivityExists(Intent intent, Context context) {
        if (intent == null || context == null) {
            return false;
        }

        return intent.resolveActivityInfo(context.getPackageManager(), PackageManager.MATCH_DEFAULT_ONLY) != null;
    }

    /**
     * Get a MemoryInfo object for the device's current memory status.
     */
    public static ActivityManager.MemoryInfo getAvailableMemory(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    /**
     * Check that the app could be easily uninstalled without root.
     */
    public static boolean isUserApp(PackageInfo info) {
        if (info != null && info.applicationInfo != null) {
            ApplicationInfo ai = info.applicationInfo;
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (ai.flags & mask) == 0;
        }

        return false;
    }

    public static boolean nearlyEqual(float a, float b, float epsilon) {
        final float diff = Math.abs(a - b);

        return diff <= epsilon;
    }

    public static boolean floatEquals(float num1, float num2) {
        float epsilon = 0.01f;
        return Math.abs(num1 - num2) < epsilon;
    }

    public static String getSimpleClassName(String name) {
        if (name == null) {
            return null;
        }

        return name.substring(name.lastIndexOf('.') + 1);
    }

    public static String dumpIntent(Intent intent) {
        if (intent != null) {
            return intent.toUri(0);
        }

        return null;
    }

    public static boolean checkStackTrace(String name) {
        if (name == null) {
            return false;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace == null) {
            return false;
        }

        for (StackTraceElement item : stackTrace) {
            if (item.getClassName().toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        Matcher m = URL_PREFIX.matcher(url);
        return m.matches();
    }

    /**
     * Works in most of the cases.<br/>
     * https://stackoverflow.com/questions/5105354/how-to-show-soft-keyboard-when-edittext-is-focused
     */
    public static void showKeyboard(@Nullable Context context) {
        if (context == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Sometimes doesn't work. IDK why.
     */
    public static void showKeyboardAlt(@Nullable Context context, View view) {
        if (context == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(view, 0);
    }

    public static void hideKeyboard(@Nullable Context context, View view) {
        if (context == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * https://stackoverflow.com/questions/4745988/how-do-i-detect-if-software-keyboard-is-visible-on-android-device-or-not
     */
    public static boolean isKeyboardShown(@Nullable Context context) {
        if (context == null) {
            return false;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        try {
            Object height =
                    InputMethodManager.class.getMethod("getInputMethodWindowVisibleHeight").invoke(inputMethodManager);
            return height != null && ((int) height) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static long getDeviceRam(Context context) {
        if (sCachedRamSize != -1) {
            return sCachedRamSize;
        }

        if (context == null) {
            return -1;
        }

        long result;

        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo);
            result = memInfo.totalMem;
        } else {
            result = 500_000_000; // safe value for devices with 1gb or more...
        }

        sCachedRamSize = result;

        return result;
    }

    public static String replace(String content, Pattern oldVal, String newVal) {
        if (content == null) {
            return null;
        }

        return oldVal.matcher(content).replaceFirst(newVal);
    }

    public static void setField(Object these, String fieldName, int value) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);
            if (f1 != null) {
                // Change private modifier to public
                f1.setAccessible(true);
                // Remove final modifier (don't working!!!)
                //Field modifiersField = Field.class.getDeclaredField("modifiers");
                //modifiersField.setAccessible(true);
                //modifiersField.setInt(f1, f1.getModifiers() & ~Modifier.FINAL);
                // Set field (at last)
                f1.setInt(these, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setField(Object these, String fieldName, long value) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);
            if (f1 != null) {
                // Change private modifier to public
                f1.setAccessible(true);
                // Remove final modifier (don't working!!!)
                //Field modifiersField = Field.class.getDeclaredField("modifiers");
                //modifiersField.setAccessible(true);
                //modifiersField.setInt(f1, f1.getModifiers() & ~Modifier.FINAL);
                // Set field (at last)
                f1.setLong(these, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setField(Object these, String fieldName, Object value) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);
            if (f1 != null) {
                // Change private modifier to public
                f1.setAccessible(true);
                // Remove final modifier (don't working!!!)
                //Field modifiersField = Field.class.getDeclaredField("modifiers");
                //modifiersField.setAccessible(true);
                //modifiersField.setInt(f1, f1.getModifiers() & ~Modifier.FINAL);
                // Set field (at last)
                f1.set(these, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static @Nullable Object getField(Object these, String fieldName) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);

            if (f1 != null) {
                f1.setAccessible(true);
                return f1.get(these);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Field getDeclaredField(Class<?> aClass, String fieldName) {
        if (aClass == null) { // null if superclass is object
            return null;
        }

        Field f1 = null;

        try {
            f1 = aClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            f1 = getDeclaredField(aClass.getSuperclass(), fieldName);
        }

        return f1;
    }

    public static int parseInt(String numString) {
        return parseInt(numString, -1);
    }

    public static int parseInt(String numString, int defaultValue) {
        if (!isInteger(numString)) {
            return defaultValue;
        }

        return Integer.parseInt(numString);
    }

    public static long parseLong(String numString) {
        return parseLong(numString, -1);
    }

    public static long parseLong(String numString, long defaultValue) {
        if (!isInteger(numString)) {
            return defaultValue;
        }

        return Long.parseLong(numString);
    }

    public static float parseFloat(String numString) {
        return parseFloat(numString, -1);
    }

    public static float parseFloat(String numString, float defaultValue) {
        if (!isNumeric(numString)) {
            return defaultValue;
        }

        return Float.parseFloat(numString);
    }

    public static boolean parseBoolean(String boolString) {
        return Boolean.parseBoolean(boolString);
    }

    public static String parseStr(String str) {
        if (str == null || str.equals("null")) {
            return null;
        }

        return str;
    }

    public static int parseInt(String[] arr, int index) {
        return parseInt(arr, index, 0);
    }

    public static int parseInt(String[] arr, int index, int defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        return parseInt(arr[index], defaultValue);
    }

    public static long parseLong(String[] arr, int index) {
        return parseLong(arr, index, 0);
    }

    public static long parseLong(String[] arr, int index, long defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        return parseLong(arr[index], defaultValue);
    }

    @Nullable
    public static String parseStr(String[] arr, int index) {
        return parseStr(arr, index, null);
    }

    public static String parseStr(String[] arr, int index, String defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        String result = parseStr(arr[index]);
        return result != null ? result : defaultValue;
    }

    public static boolean parseBoolean(String[] arr, int index) {
        return parseBoolean(arr, index, false);
    }

    public static boolean parseBoolean(String[] arr, int index, boolean defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        return parseBoolean(arr[index]);
    }

    public static float parseFloat(String[] arr, int index) {
        return parseFloat(arr, index, 0);
    }

    public static float parseFloat(String[] arr, int index, float defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        return parseFloat(arr[index], defaultValue);
    }

    public static List<Integer> parseIntList(String[] arr, int index) {
        return parseList(arr, index, Helpers::parseInt);
    }

    public static List<Long> parseLongList(String[] arr, int index) {
        return parseList(arr, index, Helpers::parseLong);
    }

    public static List<String> parseStrList(String[] arr, int index) {
        return parseList(arr, index, Helpers::parseStr);
    }

    public interface Parser<T> {
        @Nullable T parse(String spec);
    }

    public static <T> List<T> parseList(String[] arr, int index, String delim, Parser<T> itemParser) {
        return parseList(delim, parseStr(arr, index), itemParser);
    }

    public static <T> List<T> parseList(String delim, String data, Parser<T> itemParser) {
        List<T> result = new ArrayList<>();

        if (data != null) {
            String[] listArr = split(data, delim);

            for (String item : listArr) {
                T parsed = itemParser.parse(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
        }

        return result;
    }

    public static <T> List<T> parseList(String[] arr, int index, Parser<T> itemParser) {
        return parseList(ARRAY_DELIM, parseStr(arr, index), itemParser);
    }

    public static <T, K> Map<T, K> parseMap(String[] arr, int index, Parser<T> keyParser, Parser<K> valueParser) {
        return parseMap(parseStr(arr, index), keyParser, valueParser);
    }

    public static <T, K> Map<T, K> parseMap(String spec, Parser<T> keyParser, Parser<K> valueParser) {
        Map<T, K> result = new HashMap<>();

        if (spec != null) {
            String[] listArr = splitArray(spec);

            for (String item : listArr) {
                //String[] keyValPair = item.split("\\|");
                String[] keyValPair = split(item, PAIR_DELIM);

                if (keyValPair.length != 2) {
                    continue;
                }

                result.put(keyParser.parse(keyValPair[0]), valueParser.parse(keyValPair[1]));
            }
        }

        return result;
    }

    public static <T> T parseItem(String[] arr, int index, Parser<T> parser) {
        return parseItem(arr, index, parser, null);
    }

    public static <T> T parseItem(String[] arr, int index, Parser<T> parser, T defaultValue) {
        if (arr == null || arr.length <= index || index < 0) {
            return defaultValue;
        }

        T result = parser.parse(arr[index]);
        return result != null ? result : defaultValue;
    }

    public static String[] parseArray(String[] arr, int index) {
        return splitArray(parseStr(arr, index));
    }

    public static String[] splitArray(String arr) {
        return split(arr, ARRAY_DELIM);
    }

    public static String mergeArray(Object... items) {
        return merge(ARRAY_DELIM, items);
    }

    public static <T> String mergeList(Collection<T> list) {
        return merge(ARRAY_DELIM, list.toArray());
    }

    public static <T> String mergeList(String delim, Collection<T> list) {
        return merge(delim, list.toArray());
    }

    public static <T, K> String mergeMap(Map<T, K> map) {
        List<String> pairs = new ArrayList<>();
        for (Entry<T, K> pair : map.entrySet()) {
            //pairs.add(String.format("%s|%s", pair.getKey(), pair.getValue()));
            pairs.add(merge(PAIR_DELIM, pair.getKey(), pair.getValue()));
        }

        return mergeList(pairs);
    }

    public static String[] splitData(String data) {
        return split(data, DATA_DELIM);
    }

    public static String mergeData(Object... items) {
        return merge(DATA_DELIM, items);
    }

    public static String[] splitObj(String obj) {
        return split(obj, OBJ_DELIM);
    }

    public static String mergeObj(Object... items) {
        return merge(OBJ_DELIM, items);
    }

    public static String[] split(String data, String delim) {
        if (data == null) {
            return null;
        }

        // NOTE: empty array/object represented by space
        if (data.trim().isEmpty()) {
            return new String[]{};
        }

        return data.split(Pattern.quote(delim));
    }

    public static String merge(String delim, Object... params) {
        if (params == null) {
            return null;
        }

        if (params.length == 0) {
            return " "; // NOTE: empty array/object represented by space
        }

        StringBuilder sb = new StringBuilder();

        for (Object param : params) {
            if (sb.length() != 0) {
                sb.append(delim);
            }

            if (param instanceof Collection) {
                param = mergeList((Collection<?>) param);
            } else if (param instanceof Map) {
                param = mergeMap((Map<?, ?>) param);
            }

            sb.append(param);
        }

        return sb.toString();
    }

    public static String combineItems(String divider, Object... items) {
        StringBuilder result = new StringBuilder();

        if (items != null) {
            for (Object item : items) {
                if (item == null) {
                    continue;
                }

                String strItem = item.toString();

                if (strItem == null || strItem.isEmpty()) {
                    continue;
                }

                if (divider == null || result.length() == 0) {
                    result.append(strItem);
                } else {
                    result.append(divider).append(strItem);
                }
            }
        }

        return result.length() != 0 ? result.toString() : null;
    }

    public static void openLink(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // Fix: Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // NOP
        }
    }

    public static int getResourceId(String resourceName, String resourceType, Context context) {
        if (resourceName == null) {
            return -1;
        }

        return context.getResources().getIdentifier(resourceName, resourceType, context.getPackageName());
    }

    /**
     * Info: https://stackoverflow.com/questions/7896615/android-how-to-get-value-of-an-attribute-in-code
     */
    public static int getThemeAttr(Context context, int attrName) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attrName, outValue, true);
        return outValue.resourceId;
    }

    @SafeVarargs
    public static <T> T firstNonNull(T... objects) {
        for (T obj : objects) {
            if (obj != null) {
                return obj;
            }
        }

        return null;
    }

    public static int[] range(int start, int end, int step) {
        int size = (end - start) / step + 1;
        int[] result = new int[size];
        int value = start;

        for (int i = 0; i < size; i++) {
             result[i] = value;
             value += step;
        }

        return result;
    }

    public static float[] range(float origStart, float origEnd, float origStep) {
        float multiplexer = 1000f;
        int start = (int) (origStart * multiplexer);
        int end = (int) (origEnd * multiplexer);
        int step = (int) (origStep * multiplexer);

        int size = (end - start) / step + 1;
        float[] result = new float[size];
        int value = start;

        for (int i = 0; i < size; i++) {
            result[i] = value / multiplexer;
            value += step;
        }

        return result;
    }

    /**
     * Creates map from string array resource. Uses '|' as delimiter.
     * @return key/value map
     */
    public static Map<String, String> getMap(Context context, int arrayResId) {
        return getMap(context.getResources().getStringArray(arrayResId), "|", new LinkedHashMap<>());
    }

    /**
     * Creates map from string array. Uses any string as delimiter.
     * @return key/value map
     */
    public static Map<String, String> getMap(String[] array, String delim, Map<String, String> defaultMap) {
        for (String item : array) {
            StringTokenizer tokenizer = new StringTokenizer(item, delim);
            String key = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            defaultMap.put(key, value);
        }
        return defaultMap;
    }

    /**
     * Positive hash code generator.
     */
    public static int hashCode(Object... items) {
        if (items == null || items.length == 0) {
            return -1;
        }

        int hash = 0;

        for (Object item : items) {
            if (item != null) {
                hash = 31 * hash + item.hashCode();
            }
        }

        return Math.abs(hash);
    }

    /**
     * Positive hash code generator.
     */
    public static int hashCodeAny(Object... items) {
        if (items == null || items.length == 0) {
            return -1;
        }

        int hash = -1;

        for (Object item : items) {
            // Don't skip zero hash because or you'll broke Home section (id == 0)
            if (item != null && item.hashCode() != -1) {
                hash = 31 * hash + item.hashCode();
                break;
            }
        }

        return hash != -1 ? Math.abs(hash) : -1;
    }

    public static <T> boolean allNulls(List<T> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }

        for (T item : items) {
            if (item != null) {
                return false;
            }
        }

        return true;
    }

    @SafeVarargs
    public static <T> boolean allNulls(T... items) {
        if (items == null) {
            return true;
        }

        for (T item : items) {
            if (item != null) {
                return false;
            }
        }

        return true;
    }
    
    public static <T> boolean anyNull(List<T> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (T item : items) {
            if (item == null) {
                return true;
            }
        }

        return false;
    }

    @SafeVarargs
    public static <T> boolean anyNull(T... items) {
        if (items == null) {
            return false;
        }

        for (T item : items) {
            if (item == null) {
                return true;
            }
        }

        return false;
    }

    public static String decode(String urlDecoded) {
        try {
            urlDecoded = URLDecoder.decode(urlDecoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.printException(() -> "decode failed", e);
        }
        return urlDecoded;
    }

    public static String encode(String anyString) {
        try {
            anyString = URLEncoder.encode(anyString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return anyString;
    }

    public static String getHost(String url) {
        String result = null;

        try {
            URI uri = new URI(url);
            result = uri.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isUrl(String link) {
        return link != null && Patterns.WEB_URL.matcher(link).matches();
    }

    public interface Filter<T> {
        boolean test(T item);
    }

    /**
     * Predicate replacement function for devices with Android 6.0 and below.
     * @return removed items (if any) or null (if nothing removed)
     */
    @Nullable
    public static <T> List<T> removeIf(Collection<T> collection, Filter<T> filter) {
        if (collection == null || filter == null) {
            return null;
        }

        List<T> removed = null;
        try {
            for (T next : collection) {
                if (filter.test(next)) {
                    if (removed == null) {
                        removed = new ArrayList<>();
                    }
                    removed.add(next);
                }
            }
            if (removed != null) {
                collection.removeAll(removed);
            }
        } catch (UnsupportedOperationException e) { // read only collection
            removed = null;
            e.printStackTrace();
        }

        return removed;
    }

    public static <T> boolean containsIf(Collection<T> collection, Filter<T> filter) {
        if (collection == null || filter == null) {
            return false;
        }

        for (T next : collection) {
            if (filter.test(next)) {
                return true;
            }
        }

        return false;
    }

    public static <T> List<T> filter(Collection<T> collection, Filter<T> filter) {
        return filter(collection, filter, -1);
    }

    public static <T> List<T> filter(Collection<T> collection, Filter<T> filter, int limit) {
        if (collection == null || filter == null) {
            return null;
        }

        List<T> result = null;
        for (T next : collection) {
            if (filter.test(next)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(next);
            }
            limit--;
            if (limit == 0) {
                break;
            }
        }

        return result;
    }

    @Nullable
    public static <T> T findFirst(Collection<T> collection, Filter<T> filter) {
        if (collection == null || filter == null) {
            return null;
        }

        for (T next : collection) {
            if (filter.test(next)) {
                return next;
            }
        }

        return null;
    }

    @Nullable
    public static <T> T findLast(Collection<T> collection, Filter<T> filter) {
        if (collection == null || filter == null) {
            return null;
        }

        T result = null;

        for (T next : collection) {
            if (filter.test(next)) {
                result = next;
            }
        }

        return result;
    }

    /**
     * Fix duplicated items inside ATV channels etc.
     */
    public static <T> void removeDuplicates(Collection<T> list) {
        try {
            Set<T> set = new LinkedHashSet<>(list);
            list.clear();
            list.addAll(set);
        } catch (UnsupportedOperationException e) { // read only collection
            e.printStackTrace();
        }
    }

    public static <T> T get(Collection<T> collection, int index) {
        if (collection == null) {
            return null;
        }

        T result = null;
        int idx = 0;

        for (T item : collection) {
            if (idx == index) {
                result = item;
                break;
            }

            idx++;
        }

        return result;
    }

    public static <T> T get(Callable<T> callable) {
        T result = null;

        try {
            result = callable.call();
        } catch (Exception e) {
            Logger.printException(() -> "get failed", e);
        }

        return result;
    }

    public static boolean isVP9Supported() {
        if (sIsVP9Supported == null) {
            sIsVP9Supported = getCodecMaxHeight(MIME_VP9) != -1;
        }
        return sIsVP9Supported;
    }

    public static boolean isAV1Supported() {
        if (sIsAV1Supported == null) {
            // Not tested yet!!!
            sIsAV1Supported = getCodecMaxHeight(MIME_AV1) != -1;
        }
        return sIsAV1Supported;
    }

    public static boolean isVP9ResolutionSupported(int height) {
        if (height <= 0) {
            return false;
        }

        if (sVP9MaxHeight == 0) { // not initialized
            // TV capabilities sometimes are limited to the screen resolution not real decoder support
            switch (Build.MODEL) {
                // FHD devices with fake 2K support
                case "AFTSSS": // fire tv stick 3th gen
                    sVP9MaxHeight = 1080;
                    break;
                // FHD tvs capable 4K
                case "MiTV-AXSO0":
                case "VIDAA_TV":
                case "PATH_7XPRO":
                    sVP9MaxHeight = 2160;
                    break;
                default:
                    sVP9MaxHeight = getCodecMaxHeight(MIME_VP9);
                    break;
            }
        }

        return height <= sVP9MaxHeight;
    }

    public static boolean isAV1ResolutionSupported(int height) {
        if (height <= 0) {
            return false;
        }

        if (sAV1MaxHeight == 0) { // not initialized
            sAV1MaxHeight = getCodecMaxHeight(MIME_AV1);

            // On Rockchip (and some others) av1 codec info is bugged.
            // Reported max resolution is 360p.
            if (sAV1MaxHeight > 0 && sAV1MaxHeight < 1080) {
                sAV1MaxHeight = 2160;
            }
        }

        return height <= sAV1MaxHeight;
    }

    /**
     * <a href="https://developer.android.com/reference/android/media/MediaCodec">More info</a>
     */
    private static int getCodecMaxHeight(String mimeType) {
        if (VERSION.SDK_INT < 21) {
            return -1;
        }

        try {
            MediaCodecInfo[] codecInfos = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();

            for (MediaCodecInfo codecInfo : codecInfos) {
                if (codecInfo.isEncoder() || !isHardwareAccelerated(codecInfo.getName())) {
                    continue;
                }

                String[] types = codecInfo.getSupportedTypes();

                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType)) {
                        CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(type);
                        Range<Integer> heights = capabilities.getVideoCapabilities().getSupportedHeights();
                        return heights.getUpper();
                    }
                }
            }
        } catch (RuntimeException e) {
            // cannot get MediaCodecList
        }

        return -1;
    }

    /**
     * <a href="https://github.com/google/ExoPlayer/issues/4757">More info</a>
     * @param videoCodecName name from CodecInfo
     * @return is accelerated
     */
    public static boolean isHardwareAccelerated(String videoCodecName) {
        if (videoCodecName == null) {
            return false;
        }

        for (String name : new String[]{"omx.google.", "c2.android."}) {
            if (videoCodecName.toLowerCase().startsWith(name)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Binary values check utility
     */
    public static boolean check(int origin, int... values) {
        int combined = 0;

        for (int value : values) {
            combined |= value;
        }

        return (origin & combined) == combined;
    }

    public static String abbreviate(String title, int maxLength) {
        if (title == null || maxLength <= 0 || title.length() <= maxLength) {
            return title;
        }

        return title.substring(0, maxLength) + "\u2026"; // ...
    }

    /**
     * Trim with respect of &nbsp; charater
     */
    public static String trim(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("\u00a0", " ").trim(); // &nbsp;
    }

    public static int getRandomIndex(int size) {
        if (size <= 0) {
            return size;
        }

        return getRandom().nextInt(size);
    }

    public static int getRandomNumber(int min, int max) {
        return getRandom().nextInt((max - min) + 1) + min;
    }

    public static int getRandomNumber(int max) {
        return getRandom().nextInt(max);
    }

    public static <T> T getRandomItem(T[] items) {
        return items[Helpers.getRandomNumber(0, items.length - 1)];
    }

    public static <T extends Comparable<T>> T[] sortNatural(T[] stringArray) {
        Arrays.sort(stringArray, T::compareTo);
        return stringArray;
    }

    /**
     * Implementing Fisher–Yates shuffle
     */
    public static <T> T[] shuffleArray(T[] arr) {
        if (arr == null || arr.length == 0) {
            return arr;
        }

        for (int i = arr.length - 1; i > 0; i--) {
            int index = getRandom().nextInt(i + 1);
            // Simple swap
            T item = arr[index];
            arr[index] = arr[i];
            arr[i] = item;
        }

        return arr;
    }

    public static Random getRandom() {
        if (sRandom == null) {
            sRandom = new Random();
        }

        return sRandom;
    }

    public static void enableActivity(Context context, String activityClassName, boolean enable) {
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, activityClassName),
                enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static String join(CharSequence delim, CharSequence... elements) {
        if (delim == null || elements == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (CharSequence element : elements) {
            if (element == null || element.length() == 0) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(delim);
            }

            builder.append(element);
        }

        return builder.toString();
    }

    /**
     * <a href="https://developer.amazon.com/docs/fire-tv/implement-voiceview-accessibility-features-fire-os.html">...</a>
     */
    public static void describedBy(View view, Integer... ids) {
        // You can set extras on a button which is described by some
        // static text elsewhere on the screen as follows.
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.getExtras().putString("com.amazon.accessibility.describedBy", Helpers.merge(" ", (Object[]) ids));
                info.setEnabled(host.isEnabled());
            }
        });
    }

    /**
     * Trying to split while keeping the words not divided.
     */
    public static List<String> splitStringBySize(String str, int size) {
        List<String> split = new ArrayList<>();

        Scanner scanner = new Scanner(str).useDelimiter(" ");

        StringBuilder builder = new StringBuilder();

        while (scanner.hasNext()) {
            String aWord = scanner.next();

            if (builder.length() != 0) {
                builder.append(" ");
            }

            builder.append(aWord);

            if (builder.length() > size) {
                split.add(builder.toString());
                builder = new StringBuilder();
            }
        }

        if (builder.length() != 0) {
            split.add(builder.toString());
        }

        scanner.close();

        return split;
    }

    public static boolean isJUnitTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }

    public static <T> T getNextValue(T currentValue, T[] valueList) {
        T nextState = valueList[0];
        boolean found = false;

        for (T state : valueList) {
            if (found) {
                nextState = state;
                break;
            }

            if (state.equals(currentValue)) {
                found = true;
            }
        }
        return nextState;
    }

    /**
     * NOTE: proper item order not guaranteed!!!<br/>
     * Limit the maximum size of a Map by removing oldest entries when limit reached
     */
    public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
        return new LinkedHashMap<K, V>(maxEntries + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /**
     * Trim playlist if one exceeds max size
     */
    public static <T> List<T> createLRUList(final int maxEntries) {
        return new ArrayList<T>() {
            @Override
            public boolean add(T element) {
                if (contains(element)) {
                    remove(element);
                }

                if (size() > maxEntries) {
                    remove(0);
                }

                return super.add(element);
            }
        };
    }

    /**
     * Trim playlist if one exceeds max size
     */
    public static <T> List<T> createSafeLRUList(final int maxEntries) {
        return new CopyOnWriteArrayList<T>() {
            @Override
            public boolean add(T element) {
                if (contains(element)) {
                    remove(element);
                }

                if (size() > maxEntries) {
                    remove(0);
                }

                return super.add(element);
            }
        };
    }

    private static String normalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }

        return word.toLowerCase().replace("ё", "е");
    }

    public static String ellipsize(String str, int maxLen) {
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }

    public static boolean isTextRTL(CharSequence text) {
        if (text == null) {
            return false;
        }

        // One char not enough. Rtl strings may contain couple ltr chars.
        int len = Math.min(text.length(), 3);
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            byte directionality = Character.getDirectionality(c);
            if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    public static int invertColor(int color) {
        // Extract RGB components from the color
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // Invert the colors by subtracting from 255
        int invertedRed = 255 - red;
        int invertedGreen = 255 - green;
        int invertedBlue = 255 - blue;

        // Combine the inverted colors back into a single color
        return Color.rgb(invertedRed, invertedGreen, invertedBlue);
    }

    public static boolean isAppInForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    public static List<String> findAll(String data, Pattern pattern) {
        if (data == null) {
            return null;
        }

        List<String> result = null;
        Matcher matcher = pattern.matcher(data);

        while (matcher.find()) {
            if (result == null) {
                result = new ArrayList<>();
            }

            result.add(matcher.group());
        }

        return result;
    }
}
