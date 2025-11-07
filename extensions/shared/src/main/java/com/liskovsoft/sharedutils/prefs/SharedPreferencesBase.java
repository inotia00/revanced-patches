package com.liskovsoft.sharedutils.prefs;

import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.io.File;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public class SharedPreferencesBase {
    private static final long PREF_MAX_SIZE_MB = 5;
    private final SharedPreferences mPrefs;

    public SharedPreferencesBase(String prefName) {
        this(prefName, -1, false);
    }

    public SharedPreferencesBase(String prefName, boolean limitMaxSize) {
        this(prefName, -1, limitMaxSize);
    }

    public SharedPreferencesBase(String prefName, int defValResId) {
        this(prefName, defValResId, false);
    }

    public SharedPreferencesBase() {
        this(null, -1, false);
    }

    public SharedPreferencesBase(int defValResId) {
        this(null, defValResId, false);
    }

    public SharedPreferencesBase(String prefName, int defValResId, boolean limitMaxSize) {
        Context context = Utils.getContext();
        if (limitMaxSize) {
            limitMaxSize(context, prefName);
        }

        if (prefName != null) {
            mPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        } else {
            prefName = context.getPackageName() + "_preferences";
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        if (defValResId != -1) {
            try {
                PreferenceManager.setDefaultValues(context, prefName, Context.MODE_PRIVATE, defValResId, true);
            } catch (NoSuchMethodError e) {
                // NoSuchMethodError: No interface method putBoolean in class SharedPreferences$Editor (Android 7.0)
                e.printStackTrace();
            }
        }
    }

    /**
     * Delete prefs which size exceeds a limit to prevent unconditional behavior
     */
    private void limitMaxSize(Context context, String prefName) {
        File sharedPrefs = new File(context.getApplicationInfo().dataDir, "shared_prefs" + "/" + prefName + ".xml");

        if (sharedPrefs.exists() && sharedPrefs.isFile()) {
            long sizeMB = sharedPrefs.length() / 1024 / 1024;

            if (sizeMB > PREF_MAX_SIZE_MB) {
                Logger.printException(() -> "Shared preference max size exceeded. Deleting...");
                sharedPrefs.delete();
            }
        }
    }

    public void putLong(String key, long val) {
        mPrefs.edit()
                .putLong(key, val)
                .apply();
    }

    public long getLong(String key, long defVal) {
        return mPrefs.getLong(key, defVal);
    }

    public void putInt(String key, int val) {
        mPrefs.edit()
                .putInt(key, val)
                .apply();
    }

    public int getInt(String key, int defVal) {
        return mPrefs.getInt(key, defVal);
    }

    public void putBoolean(String key, boolean val) {
        mPrefs.edit()
                .putBoolean(key, val)
                .apply();
    }

    public boolean getBoolean(String key, boolean defVal) {
        return mPrefs.getBoolean(key, defVal);
    }

    public void putString(String key, String  val) {
        mPrefs.edit()
                .putString(key, val)
                .apply();
    }

    public String getString(String key, String defVal) {
        return mPrefs.getString(key, defVal);
    }

    public void clear() {
        mPrefs.edit()
                .clear()
                .apply();
    }
}
