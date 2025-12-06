package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.patches.PatchStatus.PatchVersion;
import static app.revanced.extension.shared.patches.PatchStatus.PatchedTime;
import static app.revanced.extension.shared.settings.BaseSettings.SPOOF_STREAMING_DATA_USE_JS;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT_TYPE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.widget.Toolbar;

import java.util.Date;

import app.revanced.extension.shared.innertube.client.YouTubeClient;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.LogBufferManager;
import app.revanced.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.general.ChangeFormFactorPatch;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.patches.utils.ReturnYouTubeDislikePatch;
import app.revanced.extension.youtube.returnyoutubedislike.ReturnYouTubeDislike;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.settings.YouTubeHostActivity;
import app.revanced.extension.youtube.utils.ExtendedUtils;

/**
 * Preference fragment for ReVanced settings.
 */
@SuppressWarnings("deprecation")
public class YouTubePreferenceFragment extends ToolbarPreferenceFragment {
    /**
     * The main PreferenceScreen used to display the current set of preferences.
     */
    private PreferenceScreen preferenceScreen;

    @SuppressLint("SuspiciousIndentation")
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, str) -> {
        try {
            if (str == null) {
                return;
            }

            Setting<?> setting = Setting.getSettingFromPath(str);
            if (setting == null) {
                return;
            }

            Preference mPreference = findPreference(str);
            if (mPreference == null) {
                return;
            }

            if (mPreference instanceof SwitchPreference) {
                if (ExtendedUtils.anyMatchSetting(setting)) {
                    ExtendedUtils.setPlayerFlyoutMenuAdditionalSettings();
                } else if (setting.equals(HIDE_PREVIEW_COMMENT) || setting.equals(HIDE_PREVIEW_COMMENT_TYPE)) {
                    ExtendedUtils.setCommentPreviewSettings();
                } else if (setting.equals(SPOOF_STREAMING_DATA_USE_JS)) {
                    YouTubeClient.availableClientTypes(Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get());
                    setSpoofStreamingDataPreference();
                }
            }

            setPreferenceAvailability();
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    /**
     * Initializes the preference fragment.
     */
    @Override
    protected void initialize() {
        super.initialize();

        try {
            preferenceScreen = getPreferenceScreen();
            Utils.sortPreferenceGroups(preferenceScreen);
            setPreferenceScreenToolbar(preferenceScreen);

            // Import / Export
            setBackupRestorePreference();

            // Debug log
            setDebugLogPreference();

            setPreferenceAvailability();
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Called when the fragment starts.
     */
    @Override
    public void onStart() {
        super.onStart();
        try {
            // Initialize search controller if needed.
            if (YouTubeHostActivity.searchViewController != null) {
                // Trigger search data collection after fragment is ready.
                YouTubeHostActivity.searchViewController.initializeSearchData();
            }
            Setting.preferences.preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        } catch (Exception ex) {
            Logger.printException(() -> "onStart failure", ex);
        }
    }

    @Override
    public void onDestroy() {
        Utils.resetLocalizedContext();
        super.onDestroy();
    }

    /**
     * Sets toolbar for all nested preference screens.
     */
    @Override
    protected void customizeToolbar(Toolbar toolbar) {
        YouTubeHostActivity.setToolbarLayoutParams(toolbar);
    }

    /**
     * Perform actions after toolbar setup.
     */
    @Override
    protected void onPostToolbarSetup(Toolbar toolbar, Dialog preferenceScreenDialog) {
        if (YouTubeHostActivity.searchViewController != null
                && YouTubeHostActivity.searchViewController.isSearchActive()) {
            toolbar.post(() -> YouTubeHostActivity.searchViewController.closeSearch());
        }
    }

    /**
     * Returns the preference screen for external access by SearchViewController.
     */
    public PreferenceScreen getPreferenceScreenForSearch() {
        return preferenceScreen;
    }

    /**
     * Add Preference to Import/Export settings submenu
     */
    private void setBackupRestorePreference() {
        Preference importPreference = findPreference("revanced_settings_import");
        if (importPreference == null) {
            return;
        }
        importPreference.setOnPreferenceClickListener(pref -> {
            importActivity();
            return false;
        });
        Preference exportPreference = findPreference("revanced_settings_export");
        if (exportPreference == null) {
            return;
        }
        exportPreference.setOnPreferenceClickListener(pref -> {
            settingExportInProgress = true;
            exportActivity();
            return false;
        });
    }

    /**
     * Set Preference to Debug settings submenu
     */
    private void setDebugLogPreference() {
        Preference clearLog = findPreference("revanced_debug_logs_clear_buffer");
        if (clearLog == null) {
            return;
        }
        clearLog.setOnPreferenceClickListener(pref -> {
            LogBufferManager.clearLogBuffer();
            return false;
        });

        Preference exportLogToClipboard = findPreference("revanced_debug_export_logs_to_clipboard");
        if (exportLogToClipboard == null) {
            return;
        }
        exportLogToClipboard.setOnPreferenceClickListener(pref -> {
            LogBufferManager.exportToClipboard();
            return false;
        });

        Preference exportLogToFile = findPreference("revanced_debug_export_logs_to_file");
        if (exportLogToFile == null) {
            return;
        }
        exportLogToFile.setOnPreferenceClickListener(pref -> {
            exportActivity();
            return false;
        });
    }

    private void setPreferenceAvailability() {
        setTabletLayoutPreference();
        setPatchInformationPreference();
        setRYDPreference();
        setShortsPreference();
        setSpoofStreamingDataPreference();
        setWhitelistPreference();
    }

    private void disablePreferences(boolean isAvailable, Setting<?>... unavailableEnum) {
        if (isAvailable) {
            for (Setting<?> setting : unavailableEnum) {
                final Preference preference = findPreference(setting.key);
                if (preference != null) {
                    preference.setEnabled(false);
                }
            }
        }
    }

    /**
     * Enable/Disable Preferences not working in tablet layout
     */
    private void setTabletLayoutPreference() {
        final boolean isAvailable = ExtendedUtils.isTablet() &&
                !ChangeFormFactorPatch.phoneLayoutEnabled();

        disablePreferences(
                isAvailable,
                Settings.DISABLE_ENGAGEMENT_PANEL,
                Settings.HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS,
                Settings.HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS,
                Settings.HIDE_MIX_PLAYLISTS,
                Settings.SHOW_VIDEO_TITLE_SECTION
        );
    }

    /**
     * Set patch information preference summary
     */
    private void setPatchInformationPreference() {
        Preference appNamePreference = findPreference("revanced_app_name");
        if (appNamePreference != null) {
            appNamePreference.setSummary(ExtendedUtils.getAppLabel());
        }
        Preference appVersionPreference = findPreference("revanced_app_version");
        if (appVersionPreference != null) {
            appVersionPreference.setSummary(ExtendedUtils.getAppVersionName());
        }
        Preference patchesVersion = findPreference("revanced_patches_version");
        if (patchesVersion != null) {
            patchesVersion.setSummary(PatchVersion());
        }
        Preference patchedDatePreference = findPreference("revanced_patched_date");
        if (patchedDatePreference != null) {
            long patchedTime = PatchedTime();
            Date date = new Date(patchedTime);
            patchedDatePreference.setSummary(date.toLocaleString());
        }
    }

    /**
     * Enable/Disable Preference related to RYD settings
     */
    private void setRYDPreference() {
        if (!(findPreference(Settings.RYD_ENABLED.key) instanceof SwitchPreference enabledPreference)) {
            return;
        }
        if (!(findPreference(Settings.RYD_SHORTS.key) instanceof SwitchPreference shortsPreference)) {
            return;
        }
        if (!(findPreference(Settings.RYD_DISLIKE_PERCENTAGE.key) instanceof SwitchPreference percentagePreference)) {
            return;
        }
        if (!(findPreference(Settings.RYD_COMPACT_LAYOUT.key) instanceof SwitchPreference compactLayoutPreference)) {
            return;
        }
        final Preference.OnPreferenceChangeListener clearAllUICaches = (pref, newValue) -> {
            ReturnYouTubeDislike.clearAllUICaches();

            return true;
        };
        enabledPreference.setOnPreferenceChangeListener((pref, newValue) -> {
            ReturnYouTubeDislikePatch.onRYDStatusChange();

            return true;
        });
        String shortsSummary = ReturnYouTubeDislikePatch.IS_SPOOFING_TO_NON_LITHO_SHORTS_PLAYER
                ? str("revanced_ryd_shorts_summary_on")
                : str("revanced_ryd_shorts_summary_on_disclaimer");
        shortsPreference.setSummaryOn(shortsSummary);
        percentagePreference.setOnPreferenceChangeListener(clearAllUICaches);
        compactLayoutPreference.setOnPreferenceChangeListener(clearAllUICaches);
    }

    /**
     * Enable/Disable Preference related to Shorts settings
     */
    private void setShortsPreference() {
        if (!PatchStatus.VideoPlayback()) {
            disablePreferences(
                    true,
                    Settings.SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG
            );
            Settings.SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG.save(false);
        }
    }

    private void setSpoofStreamingDataPreference() {
        if (findPreference(Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.key) instanceof ListPreference listPreference
                && findPreference(SPOOF_STREAMING_DATA_USE_JS.key) instanceof SwitchPreference switchPreference) {
            boolean useJS = SPOOF_STREAMING_DATA_USE_JS.get() || switchPreference.isChecked();

            String entriesKey = useJS
                    ? "revanced_spoof_streaming_data_default_client_with_js_entries"
                    : "revanced_spoof_streaming_data_default_client_entries";
            String entryValueKey = useJS
                    ? "revanced_spoof_streaming_data_default_client_with_js_entry_values"
                    : "revanced_spoof_streaming_data_default_client_entry_values";

            listPreference.setEntries(ResourceUtils.getArrayIdentifier(entriesKey));
            listPreference.setEntryValues(ResourceUtils.getArrayIdentifier(entryValueKey));
        }
    }

    private void setWhitelistPreference() {
        final boolean enabled = PatchStatus.VideoPlayback() || PatchStatus.SponsorBlock();
        final String[] whitelistKey = {Settings.OVERLAY_BUTTON_WHITELIST.key, "revanced_whitelist_settings"};

        for (String key : whitelistKey) {
            final Preference preference = findPreference(key);
            if (preference != null) {
                preference.setEnabled(enabled);
            }
        }
    }
}
