package app.revanced.extension.youtube.patches.general;

import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;
import static app.revanced.extension.shared.utils.Utils.hideViewByLayoutParams;
import static app.revanced.extension.shared.utils.Utils.hideViewGroupByMarginLayoutParams;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.youtube.patches.utils.PatchStatus.ImageSearchButton;
import static app.revanced.extension.youtube.patches.utils.PatchStatus.TargetActivityClass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.apps.youtube.app.application.Shell_SettingsActivity;
import com.google.android.apps.youtube.app.settings.SettingsActivity;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings({"deprecation", "unused"})
public class GeneralPatch {

    // region [Disable layout updates] patch

    private static final String[] REQUEST_HEADER_KEYS = {
            "X-Youtube-Cold-Config-Data",
            "X-Youtube-Cold-Hash-Data",
            "X-Youtube-Hot-Config-Data",
            "X-Youtube-Hot-Hash-Data"
    };

    private static final boolean DISABLE_LAYOUT_UPDATES =
            Settings.DISABLE_LAYOUT_UPDATES.get();

    /**
     * @param key   Keys to be added to the header of CronetBuilder.
     * @param value Values to be added to the header of CronetBuilder.
     * @return Empty value if setting is enabled.
     */
    public static String disableLayoutUpdates(String key, String value) {
        if (DISABLE_LAYOUT_UPDATES && StringUtils.equalsAny(key, REQUEST_HEADER_KEYS)) {
            Logger.printDebug(() -> "Blocking: " + key);
            return "";
        }

        return value;
    }

    // endregion

    // region [Disable sign in to TV popup] patch

    public static boolean disableSignInToTvPopup() {
        return Settings.DISABLE_SIGNIN_TO_TV_POPUP.get();
    }

    // endregion

    // region [Disable splash animation] patch

    public static boolean disableSplashAnimation(boolean original) {
        return !Settings.DISABLE_SPLASH_ANIMATION.get() && original;
    }

    public static int disableSplashAnimation(int i, int i2) {
        if (!Settings.DISABLE_SPLASH_ANIMATION.get() || i != i2) {
            return i;
        }
        return i - 1;
    }

    // endregion

    // region [Enable gradient loading screen] patch

    public static boolean enableGradientLoadingScreen() {
        return Settings.ENABLE_GRADIENT_LOADING_SCREEN.get();
    }

    // endregion

    // region [Hide layout components] patch

    public static boolean disableTranslucentStatusBar(boolean original) {
        return !Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get() && original;
    }

    private static String[] accountMenuBlockList;

    private static String[] getAccountMenuBlockList(Context mContext) {
        if (accountMenuBlockList == null) {
            int settingsIdentifier = ResourceUtils.getIdentifier("settings", ResourceUtils.ResourceType.STRING, mContext);
            if (settingsIdentifier != 0) {
                String settings = mContext.getResources().getString(settingsIdentifier);
                accountMenuBlockList = Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n");
                // Some settings should not be hidden.
                accountMenuBlockList = Arrays.stream(accountMenuBlockList)
                        .filter(item -> !Objects.equals(item, settings))
                        .toArray(String[]::new);
            }
        }
        //noinspection ReplaceNullCheck
        if (accountMenuBlockList != null) {
            return accountMenuBlockList;
        } else {
            return Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n");
        }
    }

    /**
     * hide account menu in you tab
     *
     * @param menuTitleCharSequence menu title
     */
    public static void hideAccountList(View view, CharSequence menuTitleCharSequence) {
        if (!Settings.HIDE_ACCOUNT_MENU.get())
            return;
        if (menuTitleCharSequence == null)
            return;
        if (!(view.getParent().getParent().getParent() instanceof ViewGroup viewGroup))
            return;

        hideAccountMenu(viewGroup, menuTitleCharSequence.toString());
    }

    /**
     * hide account menu for tablet and old clients
     *
     * @param menuTitleCharSequence menu title
     */
    public static void hideAccountMenu(View view, CharSequence menuTitleCharSequence) {
        if (!Settings.HIDE_ACCOUNT_MENU.get())
            return;
        if (menuTitleCharSequence == null)
            return;
        if (!(view.getParent().getParent() instanceof ViewGroup viewGroup))
            return;

        hideAccountMenu(viewGroup, menuTitleCharSequence.toString());
    }

    private static void hideAccountMenu(ViewGroup viewGroup, String menuTitleString) {
        for (String filter : getAccountMenuBlockList(viewGroup.getContext())) {
            if (!filter.isEmpty()) {
                if (Settings.HIDE_ACCOUNT_MENU_FILTER_TYPE.get()) {
                    if (menuTitleString.contains(filter))
                        hideViewGroup(viewGroup);
                } else {
                    if (menuTitleString.equals(filter))
                        hideViewGroup(viewGroup);
                }
            }
        }
    }

    private static void hideViewGroup(ViewGroup viewGroup) {
        if (viewGroup.getLayoutParams() instanceof MarginLayoutParams)
            hideViewGroupByMarginLayoutParams(viewGroup);
        else
            viewGroup.setLayoutParams(new LayoutParams(0, 0));
    }

    public static int hideHandle(int originalValue) {
        return Settings.HIDE_HANDLE.get() ? 8 : originalValue;
    }

    public static boolean hideFloatingMicrophone(boolean original) {
        return Settings.HIDE_FLOATING_MICROPHONE.get() || original;
    }

    // endregion

    // region [Remove viewer discretion dialog] patch

    /**
     * Injection point.
     * <p>
     * The {@link AlertDialog#getButton(int)} method must be used after {@link AlertDialog#show()} is called.
     * Otherwise {@link AlertDialog#getButton(int)} method will always return null.
     * <a href="https://stackoverflow.com/a/4604145"/>
     * <p>
     * That's why {@link AlertDialog#show()} is absolutely necessary.
     * Instead, use two tricks to hide Alertdialog.
     * <p>
     * 1. Change the size of AlertDialog to 0.
     * 2. Disable AlertDialog's background dim.
     * <p>
     * This way, AlertDialog will be completely hidden,
     * and {@link AlertDialog#getButton(int)} method can be used without issue.
     */
    public static void confirmDialog(final AlertDialog dialog) {
        if (!Settings.REMOVE_VIEWER_DISCRETION_DIALOG.get()) {
            return;
        }

        // This method is called after AlertDialog#show(),
        // So we need to hide the AlertDialog before pressing the possitive button.
        final Window window = dialog.getWindow();
        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (window != null && button != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.height = 0;
            params.width = 0;

            // Change the size of AlertDialog to 0.
            window.setAttributes(params);

            // Disable AlertDialog's background dim.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            Utils.clickView(button);
        }
    }

    public static void confirmDialogAgeVerified(final AlertDialog dialog) {
        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (!button.getText().toString().equals(str("og_continue")))
            return;

        confirmDialog(dialog);
    }

    // endregion

    // region [Fix Hype button icon] patch

    public static boolean fixHypeButtonIconEnabled() {
        return Settings.FIX_HYPE_BUTTON_ICON.get();
    }

    public static String getWatchNextEndpointVersionOverride() {
        return "19.26.42";
    }

    // endregion

    // region [Spoof app version] patch

    private static int legacyFragmentId = 0;

    public static boolean disableCairoFragment(boolean original) {
        return !Settings.FIX_SPOOF_APP_VERSION_SIDE_EFFECT.get() && original;
    }

    public static String getVersionOverride(String appVersion) {
        return Settings.SPOOF_APP_VERSION.get()
                ? Settings.SPOOF_APP_VERSION_TARGET.get()
                : appVersion;
    }

    public static int useLegacyFragment(int original) {
        if (Settings.FIX_SPOOF_APP_VERSION_SIDE_EFFECT.get()) {
            if (legacyFragmentId == 0) {
                legacyFragmentId = getXmlIdentifier("settings_fragment_legacy");
            }
            if (legacyFragmentId != 0) {
                return legacyFragmentId;
            }
        }

        return original;
    }

    // endregion

    // region [Toolbar components] patch

    private static int generalHeaderAttributeId = 0;
    private static int premiumHeaderAttributeId = 0;

    public static void setDrawerNavigationHeader(View lithoView) {
        final int headerAttributeId = getHeaderAttributeId();

        lithoView.getViewTreeObserver().addOnDrawListener(() -> {
            if (!(lithoView instanceof ViewGroup viewGroup))
                return;
            if (!(viewGroup.getChildAt(0) instanceof ImageView imageView))
                return;
            final Activity mActivity = Utils.getActivity();
            if (mActivity == null)
                return;
            imageView.setImageDrawable(getHeaderDrawable(mActivity, headerAttributeId));
        });
    }

    public static int getHeaderAttributeId() {
        if (premiumHeaderAttributeId == 0) {
            generalHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytWordmarkHeader");
            premiumHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytPremiumWordmarkHeader");
        }

        return Settings.CHANGE_YOUTUBE_HEADER.get()
                ? premiumHeaderAttributeId
                : generalHeaderAttributeId;
    }

    public static boolean overridePremiumHeader() {
        return Settings.CHANGE_YOUTUBE_HEADER.get();
    }

    private static Drawable getHeaderDrawable(Activity mActivity, int resourceId) {
        // Rest of the implementation added by patch.
        return ResourceUtils.getDrawable("");
    }

    private static final boolean wideSearchbarEnabled = Settings.ENABLE_WIDE_SEARCH_BAR.get();
    // Loads the search bar deprecated by Google.
    private static final boolean wideSearchbarWithHeaderEnabled = Settings.ENABLE_WIDE_SEARCH_BAR_WITH_HEADER.get();
    private static final boolean wideSearchbarYouTabEnabled = Settings.ENABLE_WIDE_SEARCH_BAR_IN_YOU_TAB.get();

    public static boolean enableWideSearchBar(boolean original) {
        return wideSearchbarEnabled || original;
    }

    /**
     * Limitation: Premium header will not be applied for YouTube Premium users if the user uses the 'Wide search bar with header' option.
     * This is because it forces the deprecated search bar to be loaded.
     * As a solution to this limitation, 'Change YouTube header' patch is required.
     */
    public static boolean enableWideSearchBarWithHeader(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return wideSearchbarWithHeaderEnabled || original;
    }

    public static boolean enableWideSearchBarWithHeaderInverse(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return !wideSearchbarWithHeaderEnabled && original;
    }

    public static boolean enableWideSearchBarInYouTab(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return !wideSearchbarYouTabEnabled && original;
    }

    private static int searchBarId = 0;
    private static int youtubeTextId = 0;
    private static int searchBoxId = 0;
    private static int searchIconId = 0;

    public static void setWideSearchBarLayout(View view) {
        if (!wideSearchbarEnabled)
            return;

        if (searchBarId == 0) {
            searchBarId = ResourceUtils.getIdIdentifier("search_bar");
        }

        if (!(view.findViewById(searchBarId) instanceof RelativeLayout searchBarView))
            return;

        // When the deprecated search bar is loaded, two search bars overlap.
        // Manually hides another search bar.
        if (wideSearchbarWithHeaderEnabled) {
            if (youtubeTextId == 0) {
                youtubeTextId = ResourceUtils.getIdIdentifier("youtube_text");
                searchBoxId = ResourceUtils.getIdIdentifier("search_box");
                searchIconId = ResourceUtils.getIdIdentifier("search_icon");
            }
            final View searchIconView = searchBarView.findViewById(searchIconId);
            final View searchBoxView = searchBarView.findViewById(searchBoxId);
            final View textView = searchBarView.findViewById(youtubeTextId);
            if (textView != null) {
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(0, 0);
                layoutParams.setMargins(0, 0, 0, 0);
                textView.setLayoutParams(layoutParams);
            }
            // The search icon in the deprecated search bar is clickable, but onClickListener is not assigned.
            // Assign onClickListener and disable the effect when clicked.
            if (searchIconView != null && searchBoxView != null) {
                searchIconView.setOnClickListener(view1 -> searchBoxView.callOnClick());
                searchIconView.getBackground().setAlpha(0);
            }
        } else {
            // This is the legacy method - Wide search bar without YouTube header.
            // Since the padding start is 0, it does not look good.
            // Add a padding start of 8.0 dip.
            final int paddingLeft = searchBarView.getPaddingLeft();
            final int paddingRight = searchBarView.getPaddingRight();
            final int paddingTop = searchBarView.getPaddingTop();
            final int paddingBottom = searchBarView.getPaddingBottom();
            final int paddingStart = Utils.dipToPixels(8f);

            // In RelativeLayout, paddingStart cannot be assigned programmatically.
            // Check RTL layout and set left padding or right padding.
            if (Utils.isRightToLeftLocale()) {
                searchBarView.setPadding(paddingLeft, paddingTop, paddingStart, paddingBottom);
            } else {
                searchBarView.setPadding(paddingStart, paddingTop, paddingRight, paddingBottom);
            }
        }
    }

    public static boolean hideCastButton(boolean original) {
        return !Settings.HIDE_TOOLBAR_CAST_BUTTON.get() && original;
    }

    public static void hideCastButton(MenuItem menuItem) {
        if (!Settings.HIDE_TOOLBAR_CAST_BUTTON.get())
            return;

        menuItem.setVisible(false);
        menuItem.setEnabled(false);
    }

    public static void hideCreateButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_CREATE_BUTTON.get())
            return;

        hideViewUnderCondition(isCreateButton(enumString), view);
    }

    public static void hideNotificationButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_NOTIFICATION_BUTTON.get())
            return;

        hideViewUnderCondition(isNotificationButton(enumString), view);
    }

    public static void hideSearchButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get())
            return;

        hideViewUnderCondition(isSearchButton(enumString), view);
    }

    public static void hideSearchButton(MenuItem menuItem, int original) {
        menuItem.setShowAsAction(
                Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get()
                        ? MenuItem.SHOW_AS_ACTION_NEVER
                        : original
        );
    }

    public static boolean hideSearchTermThumbnail() {
        return Settings.HIDE_SEARCH_TERM_THUMBNAIL.get();
    }

    public static boolean hideSearchTermThumbnail(boolean original) {
        return !hideSearchTermThumbnail() && original;
    }

    private static final boolean hideImageSearchButton = Settings.HIDE_IMAGE_SEARCH_BUTTON.get();
    private static final boolean hideVoiceSearchButton = Settings.HIDE_VOICE_SEARCH_BUTTON.get();

    /**
     * If the user does not hide the Image search button but only the Voice search button,
     * {@link View#setVisibility(int)} cannot be used on the Voice search button.
     * (This breaks the search bar layout.)
     * <p>
     * In this case, {@link Utils#hideViewByLayoutParams(View)} should be used.
     */
    private static final boolean showImageSearchButtonAndHideVoiceSearchButton = !hideImageSearchButton && hideVoiceSearchButton && ImageSearchButton();

    public static boolean hideImageSearchButton(boolean original) {
        return !hideImageSearchButton && original;
    }

    public static void hideVoiceSearchButton(View view) {
        if (showImageSearchButtonAndHideVoiceSearchButton) {
            hideViewByLayoutParams(view);
        } else {
            hideViewUnderCondition(hideVoiceSearchButton, view);
        }
    }

    public static void hideVoiceSearchButton(View view, int visibility) {
        if (showImageSearchButtonAndHideVoiceSearchButton) {
            view.setVisibility(visibility);
            hideViewByLayoutParams(view);
        } else {
            view.setVisibility(
                    hideVoiceSearchButton
                            ? View.GONE : visibility
            );
        }
    }

    /**
     * Injection point.
     * If the round search bar is enabled, the patch will not work.
     * Forcibly disable it.
     */
    public static boolean disableRoundSearchBar(boolean original) {
        return !Settings.HIDE_YOU_MAY_LIKE_SECTION.get() && original;
    }

    /**
     * Injection point.
     *
     * @param searchQuery Keywords entered in the search bar.
     * @return Whether the setting is enabled and the search query is empty.
     */
    public static boolean hideYouMayLikeSection(String searchQuery) {
        return Settings.HIDE_YOU_MAY_LIKE_SECTION.get()
                // The 'You may like' section is only visible when no search terms are entered.
                // To avoid unnecessary collection traversals, filtering is performed only when the searchQuery is empty.
                && StringUtils.isEmpty(searchQuery);
    }

    /**
     * Injection point.
     *
     * @param searchTerm This class contains information related to search terms.
     *                   The {@code toString()} method of this class overrides the search term.
     * @param endpoint   Endpoint related with the search term.
     *                   For search history, this value is:
     *                   '/complete/deleteitems?client=youtube-android-pb&delq=${searchTerm}&deltok=${token}'.
     *                   (If you long press on the search history,
     *                   you will see a dialog 'Remove from search history?')
     *                   For search suggestions, this value is null or empty.
     * @return Whether search term is a search history or not.
     */
    public static boolean isSearchHistory(Object searchTerm, String endpoint) {
        boolean isSearchHistory = endpoint != null && endpoint.contains("/delete");
        if (!isSearchHistory) {
            Logger.printDebug(() -> "Remove search suggestion: " + searchTerm);
        }
        return isSearchHistory;
    }

    /**
     * In ReVanced, image files are replaced to change the header,
     * Whereas in RVX, the header is changed programmatically.
     * There is an issue where the header is not changed in RVX when YouTube Doodles are hidden.
     * As a workaround, manually set the header when YouTube Doodles are hidden.
     */
    public static void hideYouTubeDoodles(ImageView imageView, Drawable drawable) {
        final Activity mActivity = Utils.getActivity();
        if (Settings.HIDE_YOUTUBE_DOODLES.get() && mActivity != null) {
            drawable = getHeaderDrawable(mActivity, getHeaderAttributeId());
        }
        imageView.setImageDrawable(drawable);
    }

    private static int settingsDrawableId = 0;
    private static int settingsCairoDrawableId = 0;

    public static int getCreateButtonDrawableId(int original) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get()) {
            return original;
        }

        if (settingsDrawableId == 0) {
            settingsDrawableId = ResourceUtils.getDrawableIdentifier("yt_outline_gear_black_24");
        }

        if (settingsDrawableId == 0) {
            return original;
        }

        // If the user has patched YouTube 19.26.42,
        // Or spoofed the app version to 19.26.42 or earlier.
        if (!ExtendedUtils.IS_19_28_OR_GREATER || ExtendedUtils.isSpoofingToLessThan("19.27.00")) {
            return settingsDrawableId;
        }

        if (settingsCairoDrawableId == 0) {
            settingsCairoDrawableId = ResourceUtils.getDrawableIdentifier("yt_outline_gear_cairo_black_24");
        }

        return settingsCairoDrawableId == 0
                ? settingsDrawableId
                : settingsCairoDrawableId;
    }

    public static void replaceCreateButton(String enumString, View toolbarView) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get())
            return;
        // Check if the button is a create button.
        if (!isCreateButton(enumString))
            return;
        ImageView imageView = getChildView((ViewGroup) toolbarView, view -> view instanceof ImageView);
        if (imageView == null)
            return;

        // Overriding is possible only after OnClickListener is assigned to the create button.
        Utils.runOnMainThreadDelayed(() -> {
            if (Settings.REPLACE_TOOLBAR_CREATE_BUTTON_TYPE.get()) {
                imageView.setOnClickListener(GeneralPatch::openRVXSettings);
                imageView.setOnLongClickListener(button -> {
                    openYouTubeSettings(button);
                    return true;
                });
            } else {
                imageView.setOnClickListener(GeneralPatch::openYouTubeSettings);
                imageView.setOnLongClickListener(button -> {
                    openRVXSettings(button);
                    return true;
                });
            }
        }, 0);
    }

    private static void openYouTubeSettings(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(context.getPackageName());
        intent.setClass(context, Shell_SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    private static void openRVXSettings(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(context.getPackageName());
        intent.setData(Uri.parse("revanced_settings_intent"));
        intent.setClassName(context.getPackageName(), TargetActivityClass());
        context.startActivity(intent);
    }

    /**
     * The theme of {@link Shell_SettingsActivity} is dark theme.
     * Since this theme is hardcoded, we should manually specify the theme for the activity.
     * <p>
     * Since {@link Shell_SettingsActivity} only invokes {@link SettingsActivity}, finish activity after specifying a theme.
     *
     * @param base {@link Shell_SettingsActivity}
     */
    public static void setShellActivityTheme(Activity base) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get())
            return;

        base.setTheme(ThemeUtils.getThemeId());
        Utils.runOnMainThreadDelayed(base::finish, 0);
    }


    private static boolean isCreateButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "CREATION_ENTRY", // Create button for Phone layout
                "FAB_CAMERA" // Create button for Tablet layout
        );
    }

    private static boolean isNotificationButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "TAB_ACTIVITY", // Notification button
                "TAB_ACTIVITY_CAIRO" // Notification button (New layout)
        );
    }

    private static boolean isSearchButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "SEARCH", // Search button
                "SEARCH_CAIRO", // Search button (New layout)
                "SEARCH_BOLD" // Search button (Shorts)
        );
    }

    // endregion

}
