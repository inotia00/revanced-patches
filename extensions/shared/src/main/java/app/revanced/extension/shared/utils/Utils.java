package app.revanced.extension.shared.utils;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static app.revanced.extension.shared.utils.BaseThemeUtils.getCancelOrNeutralButtonBackgroundColor;
import static app.revanced.extension.shared.utils.BaseThemeUtils.getOkButtonBackgroundColor;
import static app.revanced.extension.shared.utils.BaseThemeUtils.isDarkModeEnabled;
import static app.revanced.extension.shared.utils.StringRef.str;

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
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.Bidi;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import app.revanced.extension.shared.settings.AppLanguage;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.settings.StringSetting;

@SuppressWarnings("deprecation")
public class Utils {

    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    @SuppressLint("StaticFieldLeak")
    static volatile Context context;
    private static Locale contextLocale;

    // Cached Collator instance with its locale.
    @Nullable
    private static Locale cachedCollatorLocale;
    @Nullable
    private static Collator cachedCollator;

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{P}+");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");

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

    public static boolean isWebViewSupported() {
        try {
            CookieManager.getInstance();
            return true;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    /**
     * General purpose pool for network calls and other background tasks.
     * All tasks run at max thread priority.
     */
    private static final ThreadPoolExecutor backgroundThreadPool = new ThreadPoolExecutor(
            3, // 3 threads always ready to go
            Integer.MAX_VALUE,
            10, // For any threads over the minimum, keep them alive 10 seconds after they go idle
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> { // ThreadFactory
                Thread t = new Thread(r);
                t.setPriority(Thread.MAX_PRIORITY); // run at max priority
                return t;
            });

    public static void runOnBackgroundThread(@NonNull Runnable task) {
        backgroundThreadPool.execute(task);
    }

    @NonNull
    public static <T> Future<T> submitOnBackgroundThread(@NonNull Callable<T> call) {
        return backgroundThreadPool.submit(call);
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

    /**
     * Includes sub children.
     */
    public static <R extends View> R getChildViewByResourceName(View view, String str) {
        var child = view.findViewById(ResourceUtils.getIdIdentifier(str));
        //noinspection unchecked
        return (R) child;
    }

    @Nullable
    public static <T extends View> T getChildView(ViewGroup viewGroup, MatchFilter<View> filter) {
        return getChildView(viewGroup, false, filter);
    }

    /**
     * @param searchRecursively If children ViewGroups should also be
     *                          recursively searched using depth first search.
     * @return The first child view that matches the filter.
     */
    @Nullable
    public static <T extends View> T getChildView(ViewGroup viewGroup, boolean searchRecursively,
                                                  MatchFilter<View> filter) {
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
            Logger.printException(() -> "Context is not set by extension hook, returning null", null);
        }
        return context;
    }

    public static Resources getResources() {
        return getResources(true);
    }

    public static Resources getResources(boolean useContext) {
        if (useContext) {
            if (context != null) {
                return context.getResources();
            }
            Activity mActivity = activityRef.get();
            if (mActivity != null) {
                return mActivity.getResources();
            }
        }

        return Resources.getSystem();
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
            if (Utils.contextLocale != null) {
                return mContext;
            }
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
                    Configuration configuration = new Configuration(getResources(false).getConfiguration());
                    configuration.setLocale(contextLocale);
                    contextLocale = null;
                    setContext(mContext.createConfigurationContext(configuration));
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "resetLocalizedContext failed", null);
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
        Logger.printInfo(() -> "Set context: " + appContext);

        BaseThemeUtils.setThemeColor();
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

    public static int validateColor(StringSetting settings) {
        try {
            return Color.parseColor(settings.get());
        } catch (IllegalArgumentException ex) {
            // This code should never be reached.
            // Color picker rejects and will not save bad colors to a setting.
            // If a user imports bad data, the color picker preference resets the
            // bad color before this method can be called.
            Logger.printDebug(() -> "Could not parse color: $setting", ex);
            Utils.showToastShort(str("revanced_settings_color_invalid"));
            settings.resetToDefault();
            return validateColor(settings); // Recursively return.
        }
    }

    public static int validateValue(IntegerSetting settings, int min, int max, String message) {
        int value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    public static float validateValue(FloatSetting settings, float min, float max, String message) {
        float value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    @Nullable
    private static Boolean isRightToLeftTextLayout;

    /**
     * @return If the device language uses right to left text layout (Hebrew, Arabic, etc).
     * If this should match any ReVanced language override then instead use
     * {@link #isRightToLeftLocale(Locale)} with {@link BaseSettings#REVANCED_LANGUAGE}.
     * This is the default locale of the device, which may differ if
     * {@link BaseSettings#REVANCED_LANGUAGE} is set to a different language.
     */
    public static boolean isRightToLeftLocale() {
        if (isRightToLeftTextLayout == null) {
            isRightToLeftTextLayout = isRightToLeftLocale(Locale.getDefault());
        }
        return isRightToLeftTextLayout;
    }

    /**
     * @return If the locale uses right to left text layout (Hebrew, Arabic, etc).
     */
    public static boolean isRightToLeftLocale(Locale locale) {
        String displayLanguage = locale.getDisplayLanguage();
        return new Bidi(displayLanguage, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isRightToLeft();
    }

    /**
     * @return A UTF8 string containing a left-to-right or right-to-left
     * character of the device locale. If this should match any ReVanced language
     * override then instead use {@link #getTextDirectionString(Locale)} with
     * {@link BaseSettings#REVANCED_LANGUAGE}.
     */
    public static String getTextDirectionString() {
        return getTextDirectionString(isRightToLeftLocale());
    }

    public static String getTextDirectionString(Locale locale) {
        return getTextDirectionString(isRightToLeftLocale(locale));
    }

    private static String getTextDirectionString(boolean isRightToLeft) {
        return isRightToLeft
                ? "\u200F"  // u200F = right to left character.
                : "\u200E"; // u200E = left to right character.
    }

    /**
     * @return if the text contains at least 1 number character,
     * including any unicode numbers such as Arabic.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean containsNumber(CharSequence text) {
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

    /**
     * Configures the parameters of a dialog window, including its width, gravity, vertical offset and background dimming.
     * The width is calculated as a percentage of the screen's portrait width and the vertical offset is specified in DIP.
     * The default dialog background is removed to allow for custom styling.
     *
     * @param window          The {@link Window} object to configure.
     * @param gravity         The gravity for positioning the dialog (e.g., {@link Gravity#BOTTOM}).
     * @param yOffsetDip      The vertical offset from the gravity position in DIP.
     * @param widthPercentage The width of the dialog as a percentage of the screen's portrait width (0-100).
     * @param dimAmount       If true, sets the background dim amount to 0 (no dimming); if false, leaves the default dim amount.
     */
    public static void setDialogWindowParameters(Window window, int gravity, int yOffsetDip, int widthPercentage, boolean dimAmount) {
        WindowManager.LayoutParams params = window.getAttributes();

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int portraitWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);

        params.width = (int) (portraitWidth * (widthPercentage / 100.0f)); // Set width based on parameters.
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = gravity;
        params.y = yOffsetDip > 0 ? dipToPixels(yOffsetDip) : 0;
        if (dimAmount) {
            params.dimAmount = 0f;
        }

        window.setAttributes(params); // Apply window attributes.
        window.setBackgroundDrawable(null); // Remove default dialog background
    }

    /**
     * Adds a styled button to a dialog's button container with customizable text, click behavior, and appearance.
     * The button's background and text colors adapt to the app's dark mode setting. Buttons stretch to full width
     * when on separate rows or proportionally based on content when in a single row (Neutral, Cancel, OK order).
     * When wrapped to separate rows, buttons are ordered OK, Cancel, Neutral.
     *
     * @param context       Context to create the button and access resources.
     * @param buttonText    Button text to display.
     * @param onClick       Action to perform when the button is clicked, or null if no action is required.
     * @param isOkButton    If this is the OK button, which uses distinct background and text colors.
     * @param dismissDialog If the dialog should be dismissed when the button is clicked.
     * @param dialog        The Dialog to dismiss when the button is clicked.
     * @return The created Button.
     */
    public static Button addButton(Context context, String buttonText, Runnable onClick,
                                   boolean isOkButton, boolean dismissDialog, Dialog dialog) {
        Button button = new Button(context, null, 0);
        button.setText(buttonText);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);

        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(createCornerRadii(20), null, null));
        int backgroundColor = isOkButton
                ? getOkButtonBackgroundColor() // Background color for OK button (inversion).
                : getCancelOrNeutralButtonBackgroundColor(); // Background color for Cancel or Neutral buttons.
        background.getPaint().setColor(backgroundColor);
        button.setBackground(background);

        button.setTextColor(isDarkModeEnabled()
                ? (isOkButton ? Color.BLACK : Color.WHITE)
                : (isOkButton ? Color.WHITE : Color.BLACK));

        // Set internal padding.
        final int dip16 = dipToPixels(16);
        button.setPadding(dip16, 0, dip16, 0);

        button.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
            if (dismissDialog) {
                dialog.dismiss();
            }
        });

        return button;
    }

    /**
     * Creates an array of corner radii for a rounded rectangle shape.
     *
     * @param dp Radius in density-independent pixels (dip) to apply to all corners.
     * @return An array of eight float values representing the corner radii
     * (top-left, top-right, bottom-right, bottom-left).
     */
    public static float[] createCornerRadii(float dp) {
        final float radius = dipToPixels(dp);
        return new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
    }

    /**
     * Converts dip value to actual device pixels.
     *
     * @param dip The density-independent pixels value.
     * @return The device pixel value.
     */
    public static int dipToPixels(float dip) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                getResources(false).getDisplayMetrics()
        );
    }

    /**
     * Converts a percentage of the screen height to actual device pixels.
     *
     * @param percentage The percentage of the screen height (e.g., 30 for 30%).
     * @return The device pixel value corresponding to the percentage of screen height.
     */
    public static int percentageHeightToPixels(int percentage) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (metrics.heightPixels * (percentage / 100.0f));
    }

    /**
     * Converts a percentage of the screen width to actual device pixels.
     *
     * @param percentage The percentage of the screen width (e.g., 30 for 30%).
     * @return The device pixel value corresponding to the percentage of screen width.
     */
    public static int percentageWidthToPixels(int percentage) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (metrics.widthPixels * (percentage / 100.0f));
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
                    onStartAction.onStart(dialog);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onStart failure: " + dialog.getClass().getSimpleName(), ex);
            }
        }
    }

    /**
     * Interface for {@link #showDialog(Activity, Dialog, boolean, DialogFragmentOnStartAction)}.
     */
    @FunctionalInterface
    public interface DialogFragmentOnStartAction {
        void onStart(Dialog dialog);
    }

    public static void showDialog(Activity activity, Dialog dialog) {
        showDialog(activity, dialog, true, null);
    }

    /**
     * Utility method to allow showing a Dialog on top of other dialogs.
     * Calling this will always display the dialog on top of all other dialogs
     * previously called using this method.
     * <p>
     * Be aware the on start action can be called multiple times for some situations,
     * such as the user switching apps without dismissing the dialog then switching back to this app.
     * <p>
     * This method is only useful during app startup and multiple patches may show their own dialog,
     * and the most important dialog can be called last (using a delay) so it's always on top.
     * <p>
     * For all other situations it's better to not use this method and
     * call {@link Dialog#show()} on the dialog.
     */
    public static void showDialog(Activity activity,
                                  Dialog dialog,
                                  boolean isCancelable,
                                  @Nullable DialogFragmentOnStartAction onStartAction) {
        verifyOnMainThread();

        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.dialog = dialog;
        fragment.onStartAction = onStartAction;
        fragment.setCancelable(isCancelable);

        fragment.show(activity.getFragmentManager(), null);
    }

    public static AlertDialog.Builder getDialogBuilder(@NonNull Context context) {
        return new AlertDialog.Builder(context, isSDKAbove(22)
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : AlertDialog.THEME_DEVICE_DEFAULT_DARK
        );
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
     * @param builder Alertdialog builder to apply theme to.
     *                When used in a method containing an override, it must be called before 'super'.
     */
    public static AlertDialog setAlertDialogThemeAndShow(final AlertDialog.Builder builder) {
        final int dialogStyle = ResourceUtils.getStyleIdentifier("revanced_dialog_rounded_corners");
        if (dialogStyle != 0) {
            builder.getContext().setTheme(dialogStyle);
        }
        AlertDialog dialog = builder.show();
        Window window = dialog.getWindow();
        if (dialogStyle != 0 && window != null) {
            window.setBackgroundDrawable(null); // Remove default dialog background.
        }
        return dialog;
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

    /**
     * Safe to call from any thread.
     *
     * @param messageToToast Message to show.
     * @param toastDuration  Either {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}.
     */
    public static void showToast(String messageToToast, int toastDuration) {
        Objects.requireNonNull(messageToToast);
        runOnMainThreadNowOrLater(() -> {
            Context currentContext = context;

            if (currentContext == null) {
                Logger.printException(() -> "Cannot show toast (context is null): " + messageToToast);
            } else {
                Logger.printDebug(() -> "Showing toast: " + messageToToast);
                Toast.makeText(currentContext, messageToToast, toastDuration).show();
            }
        });
    }

    public static boolean isLandscapeOrientation() {
        final int orientation = getResources(false).getConfiguration().orientation;
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
     * Automatically logs any exceptions the runnable throws.
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
     * @return if the calling thread is on the main thread.
     */
    public static boolean isCurrentlyOnMainThread() {
        if (isSDKAbove(23)) {
            return Looper.getMainLooper().isCurrentThread();
        } else {
            return Looper.getMainLooper().getThread() == Thread.currentThread();
        }
    }

    /**
     * @throws IllegalStateException if the calling thread is _off_ the main thread.
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

    /**
     * Calling extension code must ensure the un-patched app has the permission
     * <code>android.permission.ACCESS_NETWORK_STATE</code>,
     * otherwise the app will crash if this method is used.
     */
    public static boolean isNetworkConnected() {
        NetworkType networkType = getNetworkType();
        return networkType == NetworkType.MOBILE
                || networkType == NetworkType.WIFI;
    }

    /**
     * Calling extension code must ensure the un-patched app has the permission
     * <code>android.permission.ACCESS_NETWORK_STATE</code>,
     * otherwise the app will crash if this method is used.
     */
    @SuppressLint("MissingPermission")
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
     * As the class {@code org.brotli.dec.BrotliInputStream} is already included in YouTube,
     * Some classes will not be merged during patching.
     * As a workaround, the obfuscated BrotliInputStream class from YouTube is entered here during the patching process.
     *
     * @return BrotliInputStream
     */
    public static InputStream getBrotliInputStream(InputStream inputStream) {
        // Rest of the implementation added by patch.
        if (inputStream != null) {
            Logger.printInfo(() -> "getBrotliInputStream");
        }
        return inputStream;
    }

    public static FrameLayout.LayoutParams getLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int left_margin = dipToPixels(20);
        int top_margin = dipToPixels(10);
        int right_margin = dipToPixels(20);
        int bottom_margin = dipToPixels(4);
        params.setMargins(left_margin, top_margin, right_margin, bottom_margin);

        return params;
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

    /**
     * Removes punctuation and converts text to lowercase. Returns an empty string if input is null.
     */
    public static String removePunctuationToLowercase(@Nullable CharSequence original) {
        if (original == null) return "";
        return PUNCTUATION_PATTERN.matcher(original).replaceAll("")
                .toLowerCase(BaseSettings.REVANCED_LANGUAGE.get().getLocale());
    }

    /**
     * Normalizes text for search: applies NFD, removes diacritics, and lowercases (locale-neutral).
     * Returns an empty string if input is null.
     */
    public static String normalizeTextToLowercase(@Nullable CharSequence original) {
        if (original == null) return "";
        return DIACRITICS_PATTERN.matcher(Normalizer.normalize(original, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a cached Collator for the current locale, or creates a new one if locale changed.
     */
    private static Collator getCollator() {
        Locale currentLocale = BaseSettings.REVANCED_LANGUAGE.get().getLocale();

        if (cachedCollator == null || !currentLocale.equals(cachedCollatorLocale)) {
            cachedCollatorLocale = currentLocale;
            cachedCollator = Collator.getInstance(currentLocale);
            cachedCollator.setStrength(Collator.SECONDARY); // Case-insensitive, diacritic-insensitive.
        }

        return cachedCollator;
    }

    /**
     * Sorts a {@link PreferenceGroup} and all nested subgroups by title or key.
     * <p>
     * The sort order is controlled by the {@link Sort} suffix present in the preference key.
     * Preferences without a key or without a {@link Sort} suffix remain in their original order.
     * <p>
     * Sorting is performed using {@link Collator} with the current user locale,
     * ensuring correct alphabetical ordering for all supported languages
     * (e.g., Ukrainian "і", German "ß", French accented characters, etc.).
     *
     * @param group the {@link PreferenceGroup} to sort
     */
    public static void sortPreferenceGroups(PreferenceGroup group) {
        Sort groupSort = Sort.fromKey(group.getKey(), Sort.UNSORTED);
        List<Pair<String, Preference>> preferences = new ArrayList<>();

        // Get cached Collator for locale-aware string comparison.
        Collator collator = getCollator();

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
                    sortValue = removePunctuationToLowercase(preference.getTitle());
                    break;
                case BY_KEY:
                    sortValue = preference.getKey();
                    break;
                case UNSORTED:
                    continue; // Keep original sorting.
                default:
                    throw new IllegalStateException();
            }

            preferences.add(new Pair<>(sortValue, preference));
        }

        // Sort the list using locale-specific collation rules.
        Collections.sort(preferences, (pair1, pair2)
                -> collator.compare(pair1.first, pair2.first));

        // Reassign order values to reflect the new sorted sequence.
        int index = 0;
        for (Pair<String, Preference> pair : preferences) {
            int order = index++;
            Preference pref = pair.second;

            // Move any screens, intents, and the one off About preference to the top.
            if (pref instanceof PreferenceScreen || pref.getIntent() != null) {
                // Any arbitrary large number.
                order -= 1000;
            }

            pref.setOrder(order);
        }
    }

    public static CharSequence setHtml(@Nullable CharSequence original) {
        if (original == null) return "";
        return Html.fromHtml(original.toString(), FROM_HTML_MODE_COMPACT);
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

    public static int clamp(int value, int lower, int upper) {
        return Math.max(lower, Math.min(value, upper));
    }

    public static float clamp(float value, float lower, float upper) {
        return Math.max(lower, Math.min(value, upper));
    }
}
