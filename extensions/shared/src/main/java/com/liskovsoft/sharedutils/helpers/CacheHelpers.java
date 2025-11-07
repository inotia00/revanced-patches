package com.liskovsoft.sharedutils.helpers;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

import java.io.IOException;
import java.io.InputStream;

import app.revanced.extension.shared.utils.Logger;

public class CacheHelpers {
    public static boolean exists(DiskLruCache cache, String key) {
        try {
            return cache.get(key) != null;
        } catch (IOException e) {
            Logger.printException(() -> "exists failed", e);
        }

        return false;
    }

    public static InputStream saveToCache(DiskLruCache cache, InputStream data, String key) {
        if (data == null) {
            return null;
        }

        String value = Helpers.toString(data);

        try {
            Editor editor = cache.edit(key);
            editor.set(0, value);
            editor.commit();
        } catch (IOException e) {
            Logger.printException(() -> "saveToCache failed", e);
        }

        return Helpers.toStream(value);
    }

    public static InputStream returnFromCache(DiskLruCache cache, String key) {
        try {
            Snapshot snapshot = cache.get(key);
            if (snapshot != null) {
                return snapshot.getInputStream(0);
            }
        } catch (IOException e) {
            Logger.printException(() -> "returnFromCache failed", e);
        }

        return null;
    }

    /**
     * Could cause too much troubles
     * @param cache obj
     */
    private static void close(DiskLruCache cache) {
        try {
            cache.close();
        } catch (IOException e) {
            Logger.printException(() -> "close failed", e);
        }
    }
}
