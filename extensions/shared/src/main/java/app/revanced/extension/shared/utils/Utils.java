package app.revanced.extension.shared.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.text.Bidi;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import app.revanced.extension.shared.settings.AppLanguage;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;

@SuppressWarnings("deprecation")
public class Utils {

    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    @SuppressLint("StaticFieldLeak")
    private static volatile Context context;
    private static Locale contextLocale;

    protected Utils() {
    } // utility class

    public static void clickView(View view) {
        if (view == null) return;
        view.callOnClick();
    }

    /**
     * Hide a view by setting its layout height and width to 1dp.
     *
     * @param condition The setting to check for hiding the view.
     * @param view      The view to hide.
     */
    public static void hideViewBy0dpUnderCondition(BooleanSetting condition, View view) {
        hideViewBy0dpUnderCondition(condition.get(), view);
    }

    public static void hideViewBy0dpUnderCondition(boolean enabled, View view) {
        if (!enabled) return;

        hideViewByLayoutParams(view);
    }

    /**
     * Hide a view by setting its visibility to GONE.
     *
     * @param condition The setting to check for hiding the view.
     * @param view      The view to hide.
     */
    public static void hideViewUnderCondition(BooleanSetting condition, View view) {
        hideViewUnderCondition(condition.get(), view);
    }

    /**
     * Hide a view by setting its visibility to GONE.
     *
     * @param condition The setting to check for hiding the view.
     * @param view      The view to hide.
     */
    public static void hideViewUnderCondition(boolean condition, View view) {
        if (!condition) return;
        if (view == null) return;

        view.setVisibility(View.GONE);
    }

    @SuppressWarnings("unused")
    public static void hideViewByRemovingFromParentUnderCondition(BooleanSetting condition, View view) {
        hideViewByRemovingFromParentUnderCondition(condition.get(), view);
    }

    public static void hideViewByRemovingFromParentUnderCondition(boolean condition, View view) {
        if (!condition) return;
        if (view == null) return;
        if (!(view.getParent() instanceof ViewGroup viewGroup))
            return;

        viewGroup.removeView(view);
    }

    /**
     * General purpose pool for network calls and other background tasks.
     * All tasks run at max thread priority.
     */
    private static final ThreadPoolExecutor backgroundThreadPool = new ThreadPoolExecutor(
            3, // 3 threads always ready to go
            10,
            10, // For any threads over the minimum, keep them alive 10 seconds after they go idle
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> { // ThreadFactory
                Thread t = new Thread(r);
                t.setPriority(Thread.NORM_PRIORITY); // run at norm priority
                return t;
            },
            ThreadPoolExecutor.DiscardPolicy()
    );

    public static void runOnBackgroundThread(@NonNull Runnable task) {
        backgroundThreadPool.execute(task);
    }

    @NonNull
    public static <T> Future<T> submitOnBackgroundThread(@NonNull Callable<T> call) {
        ThreadPoolExecutor submitThreadPool = backgroundThreadPool;
        submitThreadPool.allowCoreThreadTimeOut(true);

        return submitThreadPool.submit(call);
    }

    /**
     * Simulates a delay by doing meaningless calculations.
     * Used for debugging to verify UI timeout logic.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static long doNothingForDuration(long amountOfTimeToWaste) {
        final long timeCalculationStarted = System.currentTimeMillis();
        Logger.printDebug(() -> "Artificially creating delay of: " + amountOfTimeToWaste + "ms");

        long meaninglessValue = 0;
        while (System.currentTimeMillis() - timeCalculationStarted < amountOfTimeToWaste) {
            // could do a thread sleep, but that will trigger an exception if the thread is interrupted
            meaninglessValue += Long.numberOfLeadingZeros((long) Math.exp(Math.random()));
        }
        // return the value, otherwise the compiler or VM might optimize and remove the meaningless time wasting work,
        // leaving an empty loop that hammers on the System.currentTimeMillis native call
        return meaninglessValue;
    }

    public static boolean containsAny(@NonNull String value, @NonNull String... targets) {
        return indexOfFirstFound(value, targets) >= 0;
    }

    public static int indexOfFirstFound(@NonNull String value, @NonNull String... targets) {
        for (String string : targets) {
            if (!string.isEmpty()) {
                final int indexOf = value.indexOf(string);
                if (indexOf >= 0) return indexOf;
            }
        }
        return -1;
    }

    public interface MatchFilter<T> {
        boolean matches(T object);
    }

    public static <R extends View> R getChildView(@NonNull Activity activity, @NonNull String str) {
        final View decorView = activity.getWindow().getDecorView();
        return getChildView(decorView, str);
    }

    /**
     * @noinspection unchecked
     */
    public static <R extends View> R getChildView(@NonNull View view, @NonNull String str) {
        view = view.findViewById(ResourceUtils.getIdIdentifier(str));
        if (view != null) {
            return (R) view;
        } else {
            throw new IllegalArgumentException("View with name" + str + " not found");
        }
    }

    /**
     * @param searchRecursively If children ViewGroups should also be
     *                          recursively searched using depth first search.
     * @return The first child view that matches the filter.
     */
    @Nullable
    public static <T extends View> T getChildView(@NonNull ViewGroup viewGroup, boolean searchRecursively,
                                                  @NonNull MatchFilter<View> filter) {
        for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (filter.matches(childAt)) {
                //noinspection unchecked
                return (T) childAt;
            }
            // Must do recursive after filter check, in case the filter is looking for a ViewGroup.
            if (searchRecursively && childAt instanceof ViewGroup) {
                T match = getChildView((ViewGroup) childAt, true, filter);
                if (match != null) return match;
            }
        }
        return null;
    }

    /**
     * @return The first child view that matches the filter.
     * @noinspection rawtypes, unchecked
     */
    @Nullable
    public static <T extends View> T getChildView(@NonNull ViewGroup viewGroup, @NonNull MatchFilter filter) {
        for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (filter.matches(childAt)) {
                return (T) childAt;
            }
        }
        return null;
    }

    @Nullable
    public static ViewParent getParentView(@NonNull View view, int nthParent) {
        ViewParent parent = view.getParent();

        int currentDepth = 0;
        while (++currentDepth < nthParent && parent != null) {
            parent = parent.getParent();
        }

        if (currentDepth == nthParent) {
            return parent;
        }

        final int finalDepthLog = currentDepth;
        final ViewParent finalParent = parent;
        Logger.printDebug(() -> "Could not find parent view of depth: " + nthParent
                + " and instead found at: " + finalDepthLog + " view: " + finalParent);
        return null;
    }

    public static void restartApp(@NonNull Context mContext) {
        String packageName = mContext.getPackageName();
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) return;
        Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        // Required for API 34 and later
        // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        mainIntent.setPackage(packageName);
        if (mContext instanceof Activity mActivity) {
            mActivity.finishAndRemoveTask();
        }
        mContext.startActivity(mainIntent);
        System.runFinalizersOnExit(true);
        System.exit(0);
    }

    public static Activity getActivity() {
        return activityRef.get();
    }

    public static Context getContext() {
        if (context == null) {
            Logger.initializationException(Utils.class, "Context is null, returning null!", null);
        }
        return context;
    }

    public static Resources getResources() {
        if (context != null) {
            return context.getResources();
        }
        Activity mActivity = activityRef.get();
        if (mActivity != null) {
            return mActivity.getResources();
        }
        throw new IllegalStateException("Get resources failed");
    }

    /**
     * Compare MainActivity's Locale and Context's Locale.
     * If the Locale of MainActivity and the Locale of Context are different, the Locale of MainActivity is applied.
     * <p>
     * If Locale changes, resources should also change and be saved locally.
     * Otherwise, {@link ResourceUtils#getString(String)} will be updated to the incorrect language.
     *
     * @param mContext Context to check locale.
     * @return Context with locale applied.
     */
    public static Context getLocalizedContext(Context mContext) {
        try {
            Activity mActivity = activityRef.get();
            if (mActivity != null && mContext != null) {
                AppLanguage language = BaseSettings.REVANCED_LANGUAGE.get();

                // Locale of Application.
                Locale applicationLocale = language == AppLanguage.DEFAULT
                        ? mActivity.getResources().getConfiguration().locale
                        : language.getLocale();

                // Locale of Context.
                Locale contextLocale = mContext.getResources().getConfiguration().locale;

                // If they are different, overrides the Locale of the Context and resource.
                if (applicationLocale != contextLocale) {
                    Utils.contextLocale = contextLocale;

                    // If they are different, overrides the Locale of the Context and resource.
                    Locale.setDefault(applicationLocale);
                    Configuration configuration = new Configuration(mContext.getResources().getConfiguration());
                    configuration.setLocale(applicationLocale);
                    return mContext.createConfigurationContext(configuration);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getLocalizedContext failed", ex);
        }

        return mContext;
    }

    public static void resetLocalizedContext() {
        try {
            if (contextLocale != null) {
                Locale.setDefault(contextLocale);
                Context mContext = getContext();
                if (mContext != null) {
                    Configuration config = mContext.getResources().getConfiguration();
                    config.setLocale(contextLocale);
                    setContext(mContext.createConfigurationContext(config));
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "resetLocalizedContext failed", ex);
        }
    }

    public static void setActivity(Activity mainActivity) {
        activityRef = new WeakReference<>(mainActivity);
    }

    public static void setContext(@Nullable Context appContext) {
        // Typically, Context is invoked in the constructor method, so it is not null.
        // Since some are invoked from methods other than the constructor method,
        // it may be necessary to check whether Context is null.
        if (appContext == null) {
            return;
        }

        // Must initially set context to check the app language.
        context = appContext;
        Logger.initializationInfo(Utils.class, "Set context: " + appContext);
    }

    public static void setClipboard(@NonNull String text) {
        setClipboard(text, null);
    }

    public static void setClipboard(@NonNull String text, @Nullable String toastMessage) {
        if (context != null && context.getSystemService(Context.CLIPBOARD_SERVICE) instanceof ClipboardManager clipboardManager) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("ReVanced", text);
            clipboardManager.setPrimaryClip(clip);

            // Do not show a toast if using Android 13+ as it shows it's own toast.
            // But if the user copied with a timestamp then show a toast.
            // Unfortunately this will show 2 toasts on Android 13+, but no way around this.
            if (isSDKAbove(33) || toastMessage == null) return;
            showToastShort(toastMessage);
        }
    }

    public static String getFormattedTimeStamp(long videoTime) {
        return "'" + videoTime +
                "' (" +
                getTimeStamp(videoTime) +
                ")\n";
    }

    @SuppressLint("DefaultLocale")
    public static String getTimeStamp(long time) {
        long hours;
        long minutes;
        long seconds;

        if (isSDKAbove(26)) {
            final Duration duration = Duration.ofMillis(time);

            hours = duration.toHours();
            minutes = duration.toMinutes() % 60;
            seconds = duration.getSeconds() % 60;
        } else {
            final long currentVideoTimeInSeconds = time / 1000;

            hours = currentVideoTimeInSeconds / (60 * 60);
            minutes = (currentVideoTimeInSeconds / 60) % 60;
            seconds = currentVideoTimeInSeconds % 60;
        }

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static void setEditTextDialogTheme(final AlertDialog.Builder builder) {
        setEditTextDialogTheme(builder, false);
    }

    /**
     * No video id in these parameters.
     */
    private static final String[] PATH_NO_VIDEO_ID = {
            "ad_break",         // This request fetches a list of times when ads can be displayed.
            "get_drm_license",  // Waiting for a paid video to start.
            "heartbeat",        // This request determines whether to pause playback when the user is AFK.
            "refresh",          // Waiting for a livestream to start.
    };

    @Nullable
    public static String getVideoIdFromRequest(String url) {
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path != null && path.contains("player")) {
                if (!containsAny(path, PATH_NO_VIDEO_ID)) {
                    return uri.getQueryParameter("id");
                } else {
                    Logger.printDebug(() -> "Ignoring path: " + path);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getVideoIdFromRequest failure", ex);
        }
        return "";
    }

    /**
     * If {@link Fragment} uses [Android library] rather than [AndroidX library],
     * the Dialog theme corresponding to [Android library] should be used.
     * <p>
     * If not, the following issues will occur:
     * <a href="https://github.com/ReVanced/revanced-patches/issues/3061">ReVanced/revanced-patches#3061</a>
     * <p>
     * To prevent these issues, apply the Dialog theme corresponding to [Android library].
     *
     * @param builder  Alertdialog builder to apply theme to.
     *                 When used in a method containing an override, it must be called before 'super'.
     * @param maxWidth Whether to use alertdialog as max width.
     *                 It is used when there is a lot of content to show, such as an import/export dialog.
     */
    public static void setEditTextDialogTheme(final AlertDialog.Builder builder, boolean maxWidth) {
        final String styleIdentifier = maxWidth
                ? "revanced_edit_text_dialog_max_width_style"
                : "revanced_edit_text_dialog_style";
        final int editTextDialogStyle = ResourceUtils.getStyleIdentifier(styleIdentifier);
        if (editTextDialogStyle != 0) {
            builder.getContext().setTheme(editTextDialogStyle);
        }
    }

    public static AlertDialog.Builder getEditTextDialogBuilder(final Context context) {
        return getEditTextDialogBuilder(context, false);
    }

    public static AlertDialog.Builder getEditTextDialogBuilder(final Context context, boolean maxWidth) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        setEditTextDialogTheme(builder, maxWidth);
        return builder;
    }

    @Nullable
    private static Boolean isRightToLeftTextLayout;

    /**
     * If the device language uses right to left text layout (hebrew, arabic, etc)
     */
    public static boolean isRightToLeftTextLayout() {
        if (isRightToLeftTextLayout == null) {
            String displayLanguage = Locale.getDefault().getDisplayLanguage();
            isRightToLeftTextLayout = new Bidi(displayLanguage, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isRightToLeft();
        }
        return isRightToLeftTextLayout;
    }

    /**
     * @return if the text contains at least 1 number character,
     * including any unicode numbers such as Arabic.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean containsNumber(@NonNull CharSequence text) {
        for (int index = 0, length = text.length(); index < length; ) {
            final int codePoint = Character.codePointAt(text, index);
            if (Character.isDigit(codePoint)) {
                return true;
            }
            index += Character.charCount(codePoint);
        }

        return false;
    }

    public static CharSequence newSpanUsingStylingOfAnotherSpan(@Nullable CharSequence sourceStyle, @NonNull CharSequence newSpanText) {
        if (sourceStyle instanceof Spanned spanned) {
            return newSpanUsingStylingOfAnotherSpan(spanned, newSpanText);
        }
        return sourceStyle;
    }

    public static SpannableString newSpanUsingStylingOfAnotherSpan(@NonNull Spanned sourceStyle, @NonNull CharSequence newSpanText) {
        if (sourceStyle == newSpanText && sourceStyle instanceof SpannableString spannableString) {
            return spannableString; // Nothing to do.
        }
        SpannableString destination = new SpannableString(newSpanText);
        Object[] spans = sourceStyle.getSpans(0, sourceStyle.length(), Object.class);
        for (Object span : spans) {
            destination.setSpan(span, 0, destination.length(), sourceStyle.getSpanFlags(span));
        }
        return destination;
    }


    /**
     * @return whether the device's API level is higher than a specific SDK version.
     */
    public static boolean isSDKAbove(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    public static int dpToPx(int dp) {
        if (context == null) {
            return dp;
        } else {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        }
    }


    /**
     * Ignore this class. It must be public to satisfy Android requirements.
     */
    public static final class DialogFragmentWrapper extends DialogFragment {

        private Dialog dialog;
        @Nullable
        private DialogFragmentOnStartAction onStartAction;

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // Do not call super method to prevent state saving.
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return dialog;
        }

        @Override
        public void onStart() {
            try {
                super.onStart();

                if (onStartAction != null) {
                    onStartAction.onStart((AlertDialog) getDialog());
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onStart failure: " + dialog.getClass().getSimpleName(), ex);
            }
        }
    }

    /**
     * Interface for {@link #showDialog(Activity, AlertDialog, boolean, DialogFragmentOnStartAction)}.
     */
    @FunctionalInterface
    public interface DialogFragmentOnStartAction {
        void onStart(AlertDialog dialog);
    }

    public static void showDialog(Activity activity, AlertDialog dialog) {
        showDialog(activity, dialog, true, null);
    }

    /**
     * Utility method to allow showing an AlertDialog on top of other alert dialogs.
     * Calling this will always display the dialog on top of all other dialogs
     * previously called using this method.
     * <br>
     * Be aware the on start action can be called multiple times for some situations,
     * such as the user switching apps without dismissing the dialog then switching back to this app.
     * <br>
     * This method is only useful during app startup and multiple patches may show their own dialog,
     * and the most important dialog can be called last (using a delay) so it's always on top.
     * <br>
     * For all other situations it's better to not use this method and
     * call {@link AlertDialog#show()} on the dialog.
     */
    public static void showDialog(Activity activity,
                                  AlertDialog dialog,
                                  boolean isCancelable,
                                  @Nullable DialogFragmentOnStartAction onStartAction) {
        verifyOnMainThread();

        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.dialog = dialog;
        fragment.onStartAction = onStartAction;
        fragment.setCancelable(isCancelable);

        fragment.show(activity.getFragmentManager(), null);
    }

    /**
     * Safe to call from any thread
     */
    public static void showToastShort(@NonNull String messageToToast) {
        showToast(messageToToast, Toast.LENGTH_SHORT);
    }

    /**
     * Safe to call from any thread
     */
    public static void showToastLong(@NonNull String messageToToast) {
        showToast(messageToToast, Toast.LENGTH_LONG);
    }

    private static void showToast(@NonNull String messageToToast, int toastDuration) {
        Objects.requireNonNull(messageToToast);
        runOnMainThreadNowOrLater(() -> {
            if (context == null) {
                Logger.initializationException(Utils.class, "Cannot show toast (context is null): " + messageToToast, null);
            } else {
                Logger.printDebug(() -> "Showing toast: " + messageToToast);
                Toast.makeText(context, messageToToast, toastDuration).show();
            }
        });
    }

    public static boolean isLandscapeOrientation() {
        if (context == null) return false;
        final int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Automatically logs any exceptions the runnable throws.
     *
     * @see #runOnMainThreadNowOrLater(Runnable)
     */
    public static void runOnMainThread(@NonNull Runnable runnable) {
        runOnMainThreadDelayed(runnable, 0);
    }

    /**
     * Automatically logs any exceptions the runnable throws
     */
    public static void runOnMainThreadDelayed(@NonNull Runnable runnable, long delayMillis) {
        Runnable loggingRunnable = () -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                Logger.printException(() -> runnable.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            }
        };
        new Handler(Looper.getMainLooper()).postDelayed(loggingRunnable, delayMillis);
    }

    /**
     * If called from the main thread, the code is run immediately.<p>
     * If called off the main thread, this is the same as {@link #runOnMainThread(Runnable)}.
     */
    public static void runOnMainThreadNowOrLater(@NonNull Runnable runnable) {
        if (isCurrentlyOnMainThread()) {
            runnable.run();
        } else {
            runOnMainThread(runnable);
        }
    }

    /**
     * @return if the calling thread is on the main thread
     */
    public static boolean isCurrentlyOnMainThread() {
        if (isSDKAbove(23)) {
            return Looper.getMainLooper().isCurrentThread();
        } else {
            return Looper.getMainLooper().getThread() == Thread.currentThread();
        }
    }

    /**
     * @throws IllegalStateException if the calling thread is _off_ the main thread
     */
    public static void verifyOnMainThread() throws IllegalStateException {
        if (!isCurrentlyOnMainThread()) {
            throw new IllegalStateException("Must call _on_ the main thread");
        }
    }

    /**
     * @throws IllegalStateException if the calling thread is _on_ the main thread
     */
    public static void verifyOffMainThread() throws IllegalStateException {
        if (isCurrentlyOnMainThread()) {
            throw new IllegalStateException("Must call _off_ the main thread");
        }
    }

    public enum NetworkType {
        MOBILE("mobile"),
        WIFI("wifi"),
        NONE("none");

        private final String name;

        NetworkType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static boolean isNetworkNotConnected() {
        final NetworkType networkType = getNetworkType();
        return networkType == NetworkType.NONE;
    }

    @SuppressLint("MissingPermission") // permission already included in YouTube
    public static NetworkType getNetworkType() {
        if (context == null || !(context.getSystemService(Context.CONNECTIVITY_SERVICE) instanceof ConnectivityManager cm))
            return NetworkType.NONE;

        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected())
            return NetworkType.NONE;

        return switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_BLUETOOTH ->
                    NetworkType.MOBILE;
            default -> NetworkType.WIFI;
        };
    }

    /**
     * Hide a view by setting its layout params to 0x0
     *
     * @param view The view to hide.
     */
    public static void hideViewByLayoutParams(View view) {
        if (view == null) return;

        if (view instanceof LinearLayout) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, 0);
            view.setLayoutParams(layoutParams);
        } else if (view instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(0, 0);
            view.setLayoutParams(layoutParams2);
        } else if (view instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams3 = new RelativeLayout.LayoutParams(0, 0);
            view.setLayoutParams(layoutParams3);
        } else if (view instanceof Toolbar) {
            Toolbar.LayoutParams layoutParams4 = new Toolbar.LayoutParams(0, 0);
            view.setLayoutParams(layoutParams4);
        } else if (view instanceof ViewGroup) {
            ViewGroup.LayoutParams layoutParams5 = new ViewGroup.LayoutParams(0, 0);
            view.setLayoutParams(layoutParams5);
        } else {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
    }

    public static void hideViewGroupByMarginLayoutParams(ViewGroup viewGroup) {
        // Rest of the implementation added by patch.
        viewGroup.setVisibility(View.GONE);
    }

    /**
     * {@link PreferenceScreen} and {@link PreferenceGroup} sorting styles.
     */
    private enum Sort {
        /**
         * Sort by the localized preference title.
         */
        BY_TITLE("_sort_by_title"),

        /**
         * Sort by the preference keys.
         */
        BY_KEY("_sort_by_key"),

        /**
         * Unspecified sorting.
         */
        UNSORTED("_sort_by_unsorted");

        final String keySuffix;

        Sort(String keySuffix) {
            this.keySuffix = keySuffix;
        }

        @NonNull
        static Sort fromKey(@Nullable String key, @NonNull Sort defaultSort) {
            if (key != null) {
                for (Sort sort : values()) {
                    if (key.endsWith(sort.keySuffix)) {
                        return sort;
                    }
                }
            }
            return defaultSort;
        }
    }

    private static final Pattern punctuationPattern = Pattern.compile("\\p{P}+");

    /**
     * Strips all punctuation and converts to lower case.  A null parameter returns an empty string.
     */
    public static String removePunctuationConvertToLowercase(@Nullable CharSequence original) {
        if (original == null) return "";
        return punctuationPattern.matcher(original).replaceAll("").toLowerCase();
    }

    /**
     * Sort a PreferenceGroup and all it's sub groups by title or key.
     * <p>
     * Sort order is determined by the preferences key {@link Sort} suffix.
     * <p>
     * If a preference has no key or no {@link Sort} suffix,
     * then the preferences are left unsorted.
     */
    public static void sortPreferenceGroups(@NonNull PreferenceGroup group) {
        Sort groupSort = Sort.fromKey(group.getKey(), Sort.UNSORTED);
        SortedMap<String, Preference> preferences = new TreeMap<>();

        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference preference = group.getPreference(i);

            final Sort preferenceSort;
            if (preference instanceof PreferenceGroup subGroup) {
                sortPreferenceGroups(subGroup);
                preferenceSort = groupSort; // Sort value for groups is for it's content, not itself.
            } else {
                // Allow individual preferences to set a key sorting.
                // Used to force a preference to the top or bottom of a group.
                preferenceSort = Sort.fromKey(preference.getKey(), groupSort);
            }

            final String sortValue;
            switch (preferenceSort) {
                case BY_TITLE:
                    sortValue = removePunctuationConvertToLowercase(preference.getTitle());
                    break;
                case BY_KEY:
                    sortValue = preference.getKey();
                    break;
                case UNSORTED:
                    continue; // Keep original sorting.
                default:
                    throw new IllegalStateException();
            }

            preferences.put(sortValue, preference);
        }

        int index = 0;
        for (Preference pref : preferences.values()) {
            int order = index++;

            // Move any screens, intents, and the one off About preference to the top.
            if (pref instanceof PreferenceScreen || pref.getIntent() != null) {
                // Arbitrary high number.
                order -= 1000;
            }

            pref.setOrder(order);
        }
    }

    /**
     * Set all preferences to multiline titles if the device is not using an English variant.
     * The English strings are heavily scrutinized and all titles fit on screen
     * except 2 or 3 preference strings and those do not affect readability.
     * <p>
     * Allowing multiline for those 2 or 3 English preferences looks weird and out of place,
     * and visually it looks better to clip the text and keep all titles 1 line.
     */
    public static void setPreferenceTitlesToMultiLineIfNeeded(PreferenceGroup group) {
        if (!isSDKAbove(26)) {
            return;
        }

        String revancedLocale = Utils.getContext().getResources().getConfiguration().locale.getLanguage();
        if (revancedLocale.equals(Locale.ENGLISH.getLanguage())) {
            return;
        }

        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference pref = group.getPreference(i);
            pref.setSingleLineTitle(false);

            if (pref instanceof PreferenceGroup subGroup) {
                setPreferenceTitlesToMultiLineIfNeeded(subGroup);
            }
        }
    }
}
