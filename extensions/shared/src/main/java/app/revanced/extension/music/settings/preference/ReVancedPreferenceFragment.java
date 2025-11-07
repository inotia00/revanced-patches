package app.revanced.extension.music.settings.preference;

import static app.revanced.extension.music.settings.Settings.APP_INFO;
import static app.revanced.extension.music.settings.Settings.BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN;
import static app.revanced.extension.music.settings.Settings.CHANGE_START_PAGE;
import static app.revanced.extension.music.settings.Settings.CUSTOM_FILTER_STRINGS;
import static app.revanced.extension.music.settings.Settings.CUSTOM_PLAYBACK_SPEEDS;
import static app.revanced.extension.music.settings.Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY;
import static app.revanced.extension.music.settings.Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY;
import static app.revanced.extension.music.settings.Settings.DISABLE_MUSIC_VIDEO_IN_ALBUM_REDIRECT_TYPE;
import static app.revanced.extension.music.settings.Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE;
import static app.revanced.extension.music.settings.Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME;
import static app.revanced.extension.music.settings.Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS;
import static app.revanced.extension.music.settings.Settings.OPEN_DEFAULT_APP_SETTINGS;
import static app.revanced.extension.music.settings.Settings.OPTIONAL_SPONSOR_BLOCK_SETTINGS_PREFIX;
import static app.revanced.extension.music.settings.Settings.REPLACE_NAVIGATION_BUTTON_ABOUT;
import static app.revanced.extension.music.settings.Settings.RETURN_YOUTUBE_USERNAME_ABOUT;
import static app.revanced.extension.music.settings.Settings.SB_API_URL;
import static app.revanced.extension.music.settings.Settings.SETTINGS_IMPORT_EXPORT;
import static app.revanced.extension.music.settings.Settings.SPOOF_APP_VERSION_TARGET;
import static app.revanced.extension.music.settings.Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT;
import static app.revanced.extension.music.settings.Settings.SPOOF_STREAMING_DATA_VR_AUTH_TOKEN_ABOUT;
import static app.revanced.extension.music.settings.Settings.WATCH_HISTORY_TYPE;
import static app.revanced.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.revanced.extension.music.utils.ExtendedUtils.getLayoutParams;
import static app.revanced.extension.music.utils.RestartUtils.showRestartDialog;
import static app.revanced.extension.shared.patches.PatchStatus.PatchVersion;
import static app.revanced.extension.shared.settings.BaseSettings.RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT;
import static app.revanced.extension.shared.settings.BaseSettings.RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY;
import static app.revanced.extension.shared.settings.Setting.getSettingFromPath;
import static app.revanced.extension.shared.utils.ResourceUtils.getStringArray;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

import app.revanced.extension.music.patches.utils.ReturnYouTubeDislikePatch;
import app.revanced.extension.music.returnyoutubedislike.ReturnYouTubeDislike;
import app.revanced.extension.music.settings.ActivityHook;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.utils.ExtendedUtils;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.settings.preference.YouTubeDataAPIDialogBuilder;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("all")
public class ReVancedPreferenceFragment extends PreferenceFragment {

    private static final String IMPORT_EXPORT_SETTINGS_ENTRY_KEY = "revanced_settings_import_export_entries";
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

    private static String existingSettings;


    public ReVancedPreferenceFragment() {
    }

    /**
     * Injection point.
     */
    public static void onPreferenceChanged(@Nullable String key, boolean newValue) {
        if (key == null || key.isEmpty())
            return;

        if (key.equals(Settings.RESTORE_OLD_PLAYER_LAYOUT.key) && newValue) {
            Settings.RESTORE_OLD_PLAYER_BACKGROUND.save(newValue);
        } else if (key.equals(Settings.RYD_ENABLED.key)) {
            ReturnYouTubeDislikePatch.onRYDStatusChange(newValue);
        } else if (key.equals(Settings.RYD_DISLIKE_PERCENTAGE.key) || key.equals(Settings.RYD_COMPACT_LAYOUT.key)) {
            ReturnYouTubeDislike.clearAllUICaches();
        }

        for (Setting<?> setting : Setting.allLoadedSettings()) {
            if (key.equals(setting.key)) {
                ((BooleanSetting) setting).save(newValue);
                if (setting.rebootApp) {
                    showRebootDialog();
                }
                break;
            }
        }
    }

    public static void showRebootDialog() {
        final Activity activity = ActivityHook.getActivity();
        if (activity == null)
            return;

        showRestartDialog(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            final Activity baseActivity = this.getActivity();
            final Activity mActivity = ActivityHook.getActivity();
            final Intent savedInstanceStateIntent = baseActivity.getIntent();
            if (savedInstanceStateIntent == null)
                return;

            final String dataString = savedInstanceStateIntent.getDataString();
            if (dataString == null || dataString.isEmpty())
                return;

            if (dataString.startsWith(OPTIONAL_SPONSOR_BLOCK_SETTINGS_PREFIX)) {
                SponsorBlockCategoryPreference.showDialog(baseActivity, dataString.replaceAll(OPTIONAL_SPONSOR_BLOCK_SETTINGS_PREFIX, ""));
                return;
            } else if (dataString.equals(OPEN_DEFAULT_APP_SETTINGS)) {
                openDefaultAppSetting();
                return;
            }

            final Setting<?> settings = getSettingFromPath(dataString);
            if (settings instanceof StringSetting stringSetting) {
                if (settings.equals(BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN)
                        || settings.equals(CUSTOM_FILTER_STRINGS)
                        || settings.equals(CUSTOM_PLAYBACK_SPEEDS)
                        || settings.equals(CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY)
                        || settings.equals(CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY)
                        || settings.equals(ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE)
                        || settings.equals(HIDE_ACCOUNT_MENU_FILTER_STRINGS)
                        || settings.equals(RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY)) {
                    ResettableEditTextPreference.showDialog(mActivity, stringSetting);
                } else if (settings.equals(EXTERNAL_DOWNLOADER_PACKAGE_NAME)) {
                    ExternalDownloaderPreference.showDialog(mActivity);
                } else if (settings.equals(SB_API_URL)) {
                    SponsorBlockApiUrlPreference.showDialog(mActivity);
                } else if (settings.equals(SPOOF_APP_VERSION_TARGET)) {
                    ResettableListPreference.showDialog(mActivity, stringSetting, 0);
                } else {
                    Logger.printDebug(() -> "Failed to find the right value: " + dataString);
                }
            } else if (settings instanceof BooleanSetting) {
                if (settings.equals(APP_INFO)) {
                    AppInfoDialogBuilder.showDialog(mActivity);
                } else if (settings.equals(SETTINGS_IMPORT_EXPORT)) {
                    importExportListDialogBuilder();
                } else if (settings.equals(RETURN_YOUTUBE_USERNAME_ABOUT)) {
                    YouTubeDataAPIDialogBuilder.showDialog(mActivity);
                } else if (settings.equals(REPLACE_NAVIGATION_BUTTON_ABOUT)) {
                    ResettableListPreference.showDialog(mActivity, CHANGE_START_PAGE, 0);
                } else if (settings.equals(SPOOF_STREAMING_DATA_VR_AUTH_TOKEN_ABOUT)) {
                    SpoofStreamingDataVRAuthTokenDialogBuilder.showDialog(mActivity);
                } else {
                    Logger.printDebug(() -> "Failed to find the right value: " + dataString);
                }
            } else if (settings instanceof EnumSetting<?> enumSetting) {
                if (settings.equals(CHANGE_START_PAGE)
                        || settings.equals(DISABLE_MUSIC_VIDEO_IN_ALBUM_REDIRECT_TYPE)
                        || settings.equals(RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT)
                        || settings.equals(WATCH_HISTORY_TYPE)
                ) {
                    ResettableListPreference.showDialog(mActivity, enumSetting, 0);
                } else if (settings.equals(SPOOF_STREAMING_DATA_DEFAULT_CLIENT)) {
                    ResettableListPreference.showDialog(mActivity, enumSetting, 1);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate failure", ex);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void openDefaultAppSetting() {
        try {
            Context context = getActivity();
            final Uri uri = Uri.parse("package:" + context.getPackageName());
            final Intent intent = isSDKAbove(31)
                    ? new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri)
                    : new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
            context.startActivity(intent);
        } catch (Exception exception) {
            Logger.printException(() -> "openDefaultAppSetting failed");
        }
    }

    /**
     * Build a ListDialog for Import / Export settings
     * When importing/exporting as file, {@link #onActivityResult} is used, so declare it here.
     */
    private void importExportListDialogBuilder() {
        try {
            final Activity activity = getActivity();
            final String[] mEntries = getStringArray(IMPORT_EXPORT_SETTINGS_ENTRY_KEY);

            getDialogBuilder(activity)
                    .setTitle(str("revanced_settings_import_export_title"))
                    .setItems(mEntries, (dialog, index) -> {
                        switch (index) {
                            case 0 -> exportActivity();
                            case 1 -> importActivity();
                            case 2 -> importExportEditTextDialogBuilder();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "importExportListDialogBuilder failure", ex);
        }
    }

    /**
     * Build a EditTextDialog for Import / Export settings
     */
    private void importExportEditTextDialogBuilder() {
        try {
            final Activity activity = getActivity();
            final EditText textView = new EditText(activity);
            existingSettings = Setting.exportToJson(null);
            textView.setText(existingSettings);
            textView.setInputType(textView.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8); // Use a smaller font to reduce text wrap.

            TextInputLayout textInputLayout = new TextInputLayout(activity);
            textInputLayout.setLayoutParams(getLayoutParams());
            textInputLayout.addView(textView);

            FrameLayout container = new FrameLayout(activity);
            container.addView(textInputLayout);

            getDialogBuilder(activity)
                    .setTitle(str("revanced_settings_import_export_title"))
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_settings_import_copy"), (dialog, which) -> Utils.setClipboard(textView.getText().toString(), str("revanced_share_copy_settings_success")))
                    .setPositiveButton(str("revanced_settings_import"), (dialog, which) -> importSettings(activity, textView.getText().toString()))
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "importExportEditTextDialogBuilder failure", ex);
        }
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    private void exportActivity() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        var appName = ExtendedUtils.getAppLabel();
        var versionName = ExtendedUtils.getAppVersionName();
        final String formatDate = dateFormat.format(new Date(System.currentTimeMillis()));
        final StringBuilder sb = new StringBuilder();
        sb.append(appName);
        sb.append("_v");
        sb.append(versionName);
        String patchVersion = PatchVersion();
        if (!"Unknown".equals(patchVersion)) {
            sb.append("_rvp_v");
            sb.append(patchVersion);
        }
        sb.append("_settings_");
        sb.append(formatDate);

        var intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, sb.toString());
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    /**
     * Invoke the SAF(Storage Access Framework) to import settings
     */
    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            exportText(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importText(data.getData());
        }
    }

    private void exportText(Uri uri) {
        try {
            final Context context = this.getContext();

            @SuppressLint("Recycle")
            FileWriter jsonFileWriter =
                    new FileWriter(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "w"))
                                    .getFileDescriptor()
                    );
            PrintWriter printWriter = new PrintWriter(jsonFileWriter);
            printWriter.write(Setting.exportToJson(null));
            printWriter.close();
            jsonFileWriter.close();

            showToastShort(str("revanced_settings_export_success"));
        } catch (IOException e) {
            showToastShort(str("revanced_settings_export_failed"));
        }
    }

    private void importText(Uri uri) {
        final Context context = this.getContext();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            @SuppressLint("Recycle")
            FileReader fileReader =
                    new FileReader(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "r"))
                                    .getFileDescriptor()
                    );
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
            fileReader.close();

            final boolean restartNeeded = Setting.importFromJSON(context, sb.toString());
            if (restartNeeded) {
                ReVancedPreferenceFragment.showRebootDialog();
            }
        } catch (IOException e) {
            showToastShort(str("revanced_settings_import_failed"));
            throw new RuntimeException(e);
        }
    }

    private void importSettings(Activity mActivity, String replacementSettings) {
        try {
            existingSettings = Setting.exportToJson(null);
            if (replacementSettings.equals(existingSettings)) {
                return;
            }
            final boolean restartNeeded = Setting.importFromJSON(mActivity, replacementSettings);
            if (restartNeeded) {
                ReVancedPreferenceFragment.showRebootDialog();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "importSettings failure", ex);
        }
    }
}