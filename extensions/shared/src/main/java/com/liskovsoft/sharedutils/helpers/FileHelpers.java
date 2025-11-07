package com.liskovsoft.sharedutils.helpers;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Environment;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import app.revanced.extension.shared.utils.Logger;

public class FileHelpers {
    public static File getDownloadDir(Context context) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static File getCacheDir(Context context) {
        // Android 6.0 fix (providers not supported)
        File cacheDir = getExternalCacheDir(context);

        if (cacheDir == null) {
            // Android 7.0 and above (supports install from internal dirs)
            cacheDir = getInternalCacheDir(context);
        }

        return cacheDir;
    }

    public static File getInternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }

        return context.getCacheDir();
    }

    public static File getExternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }

        File cacheDir = context.getExternalCacheDir();

        if (cacheDir == null || !cacheDir.canWrite()) {
            // No storage, try to use internal one
            cacheDir = Environment.getExternalStorageDirectory();

            if (cacheDir == null || !cacheDir.canWrite()) {
                cacheDir = null;
            }
        }

        return cacheDir;
    }

    public static File getExternalFilesDir(Context context) {
        if (context == null) {
            return null;
        }

        File filesDir = context.getExternalFilesDir(null);

        if (filesDir == null || !filesDir.canWrite()) {
            // No storage, try to use internal one
            filesDir = Environment.getExternalStorageDirectory();

            if (filesDir == null || !filesDir.canWrite()) {
                filesDir = null;
            }
        }

        return filesDir;
    }

    public static File getBackupDir(Context context) {
        return new File(Environment.getExternalStorageDirectory(), String.format("data/%s", context.getPackageName()));
    }

    /**
     * NOTE: App should have permission to access device storage
     */
    public static boolean isEmpty(File dir) {
        return dir == null || listFileTree(dir).size() == 0;
    }

    /**
     * NOTE: App should have permission to access device storage
     */
    public static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<>();

        if (dir == null || dir.listFiles() == null){
            return fileTree;
        }

        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                fileTree.add(entry);
            } else {
                fileTree.addAll(listFileTree(entry));
            }
        }

        return fileTree;
    }

    /**
     * Deletes cache of the app
     */
    public static void deleteCache(Context context) {
        deleteContent(getInternalCacheDir(context));
        deleteContent(getExternalCacheDir(context));
    }

    public static boolean delete(String filePath) {
        return filePath != null && delete(new File(filePath));
    }

    public static boolean delete(File sourceLocation) {
        return deleteRecursive(sourceLocation, true);
    }

    public static boolean deleteContent(File sourceLocation) {
        return deleteRecursive(sourceLocation, false);
    }

    private static boolean deleteRecursive(File sourceLocation, boolean deleteRoot) {
        return deleteRecursive(sourceLocation, deleteRoot, 0);
    }

    /**
     * Use level to prevent StackOverflowError
     */
    private static boolean deleteRecursive(File sourceLocation, boolean deleteRoot, int level) {
        if (sourceLocation != null && sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            if (children != null && level < 10) { // Android 4.4 fix, prevent stack overflow
                for (String child : children) {
                    boolean success = deleteRecursive(new File(sourceLocation, child), true, level + 1);
                    if (!success) {
                        return false;
                    }
                }
            }
            return deleteRoot ? sourceLocation.delete() : true;
        } else if (sourceLocation != null && sourceLocation.isFile()) {
            return sourceLocation.delete();
        } else {
            return false;
        }
    }

    public static void deleteByPrefix(File directory, String prefix) {
        if (directory == null) {
            return;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.isDirectory()) {
                deleteByPrefix(file, prefix);
            } else if (file != null && file.isFile() && file.getName().startsWith(prefix)) {
                file.delete();
            }
        }
    }

    public static void copy(File sourceLocation, File targetLocation) {
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            try {
                copyFile(sourceLocation, targetLocation);
            } catch (IOException e) {
                Logger.printException(() -> "Failed to copy: File", e);
            }
        }
    }

    private static void copyDirectory(File source, File target) {
        if (!target.exists()) {
            target.mkdirs();
        }

        String[] list = source.list();

        if (list == null) {
            Logger.printDebug(() -> "Seems that read permissions not granted for file: " + source.getAbsolutePath());
            return;
        }

        for (String f : list) {
            copy(new File(source, f), new File(target, f));
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        try (
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target)
        ) {
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    public static void streamToFile(InputStream is, File destination) {
        if (is == null || destination == null) {
            return;
        }

        FileOutputStream fos = null;

        try {
            if (destination.getParentFile() != null) {
                destination.getParentFile().mkdirs(); // create dirs tree
            }
            destination.createNewFile(); // create empty file

            fos = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];
            int len1;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
        } catch (FileNotFoundException ex) {
            Logger.printException(() -> "Open file failed: Seemed EACCES (Permission denied)", ex);
        } catch (IOException ex) {
            Logger.printException(() -> "streamToFile failed", ex);
        } finally {
            closeStream(fos);
            closeStream(is);
        }
    }

    public static void stringToFile(String is, File destination) {
        streamToFile(toStream(is), destination);
    }

    public static String toString(InputStream in) {
        try {
            int bufsize = 8196;
            char[] cbuf = new char[bufsize];
            StringBuilder buf = new StringBuilder(bufsize);
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");

            int readBytes;
            while ((readBytes = reader.read(cbuf, 0, bufsize)) != -1) {
                buf.append(cbuf, 0, readBytes);
            }

            return buf.toString();
        } catch (IOException e) {
            Logger.printException(() -> "toString failed", e);
        }

        return null;
    }

    ///**
    // * Converts with respect to charset encoding.<br/>
    // * More optimized than alt method below?<br/>
    // * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
    // */
    //public static String toString(InputStream content) {
    //    if (content == null) {
    //        return null;
    //    }
    //
    //    String result = null;
    //
    //    try {
    //        result = IOUtils.toString(content, "UTF-8");
    //        content.close();
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //        Log.d(TAG, e.getMessage());
    //    }
    //
    //    return result;
    //}

    ///**
    // * Converts with respect to charset encoding.<br/>
    // * Use alt methods carefully.<br/>
    // * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
    // */
    //public static String toStringAlt(InputStream content) {
    //    if (content == null) {
    //        return null;
    //    }
    //
    //    String result = null;
    //    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
    //        BufferedInputStream bis = new BufferedInputStream(content);
    //        byte[] buffer = new byte[1024];
    //        int length;
    //        while ((length = bis.read(buffer)) != -1) {
    //            outputStream.write(buffer, 0, length);
    //        }
    //        // StandardCharsets.UTF_8.name() > JDK 7
    //        result = outputStream.toString("UTF-8");
    //        bis.close();
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //        Log.d(TAG, e.getMessage());
    //    }
    //
    //    return result;
    //}

    //public static String toStringEfficient(InputStream content) {
    //    if (content == null) {
    //        return null;
    //    }
    //
    //    StringBuilder sb = new StringBuilder();
    //
    //    try (BufferedReader in
    //            = new BufferedReader(new InputStreamReader(content, "UTF-8"))) {
    //        char[] buffer = new char[1024];
    //
    //        while (in.read(buffer) != -1) {
    //            sb.append(buffer);
    //        }
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //        Log.d(TAG, e.getMessage());
    //    }
    //
    //    return sb.toString();
    //}

    //public static String toStringOld(InputStream content) {
    //    if (content == null) {
    //        return null;
    //    }
    //
    //    Scanner s = new Scanner(content, "UTF-8").useDelimiter("\\A");
    //    String result = s.hasNext() ? s.next() : "";
    //
    //    s.close();
    //
    //    return result;
    //}

    public static InputStream toStream(String content) {
        if (content == null) {
            return null;
        }

        return new ByteArrayInputStream(content.getBytes(Charset.forName("UTF8")));
    }

    public static void closeStream(Closeable fos) {
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // NOTE: Android 7.0 fix
    @Nullable
    public static Uri getFileUri(Context context, String filePath) {
        // If your targetSdkVersion is 24 (Android 7.0) or higher, we have to use FileProvider class
        // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        if (VERSION.SDK_INT >= 24) {
            try {
                return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".update_provider", new File(filePath));
            } catch (IllegalArgumentException e) {
                // Failed to find configured root that contains /storage/emulated/0/Android/data/com.liskovsoft.smarttubetv.beta/cache/update.apk
                return null;
            }
        } else {
            return Uri.fromFile(new File(filePath));
        }
    }

    public static Uri getFileUri(Context context, File filePath) {
        if (filePath == null) {
            return null;
        }

        return getFileUri(context, filePath.getAbsolutePath());
    }

    public static InputStream appendStream(InputStream first, InputStream second) {
        if (first == null && second == null) {
            return null;
        }

        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return new SequenceInputStream(first, second);
    }

    /**
     * Can read and write the media
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Can at least read the media
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return isExternalStorageWritable() || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isFileExists(String path) {
        if (path == null) {
            return false;
        }

        return new File(path).exists();
    }

    public static boolean isFileExists(File path) {
        if (path == null) {
            return false;
        }

        return path.exists();
    }

    public static void ensureFileExists(File file) {
        if (file == null) {
            return;
        }

        try {
            if (!file.exists()) {
                if (file.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                }

                file.createNewFile();
            }
        } catch (IOException e) {
            Logger.printException(() -> "ensureFileExists", e);
        }
    }

    /**
     * Test that file is created within specified time period
     */
    public static boolean isFreshFile(String path, int freshTimeMS) {
        if (path == null) {
            return false;
        }

        File file = new File(path);

        if (!file.exists()) {
            return false;
        }

        int fileSizeKB = Integer.parseInt(String.valueOf(file.length() / 1024));

        if (fileSizeKB < 1_000) { // 1MB
            return false;
        }

        return System.currentTimeMillis() - file.lastModified() < freshTimeMS;
    }

    public static String getFileContents(File source) {
        if (source == null) {
            return null;
        }

        String result = null;
        try {
            result = toString(new FileInputStream(source));
        } catch (FileNotFoundException e) {
            Logger.printException(() -> "File not found: " + source.getAbsolutePath(), e);
        }
        return result;
    }

    /**
     * Gets size in bytes!
     */
    public static long getDirSize(File dir) {
        if (dir == null) {
            return 0;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            return 0;
        }

        long size = 0;
        for (File file : files) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }
}
