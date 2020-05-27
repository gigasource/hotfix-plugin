package io.gigasource.hotfix_plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.tencent.tinker.lib.library.TinkerLoadLibrary;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.tinker.TinkerLoadResult;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PatchingUtil {
    private static final int MAX_DOWNLOAD_RETRY = 12;
    public static final int MAX_UPDATE_RETRY = 12;
    public static int updateCounter = 0;

    public static void loadLibrary(Context context) {
        TinkerLoadLibrary.installNavitveLibraryABI(context, "armeabi");
        System.loadLibrary("stlport_shared");
    }

    public static void cleanPatch(Context context) {
        Tinker.with(context).cleanPatch();
    }

    public static void killProcess(Context context) {
        ShareTinkerInternals.killAllOtherProcess(context);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static void initUrl(Context context, String domain) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.TINKER, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.DOMAIN_KEY, domain).apply();
        Tinker tinker = Tinker.with(context);
        if (!tinker.isTinkerLoaded()) {
            sharedPreferences.edit().putString("originalVersion", BuildConfig.VERSION_NAME).apply();
        }

        String version = sharedPreferences.getString("originalVersion", BuildConfig.VERSION_NAME);

        String patchUrl = String.format("%s/static-apk/%s/%s/%s", domain, getBuildConfigValue(context, "TOPIC"), version, Constants.APK_NAME);
        String patchPath = context.getFilesDir().getAbsolutePath() +"/" + Constants.APK_NAME;
        String md5Url = String.format("%s/md5/%s/%s/%s", domain, getBuildConfigValue(context, "TOPIC"), version, Constants.APK_NAME);

        setUrlPreferences(context, patchUrl, patchPath, md5Url);
    }

    private static void setUrlPreferences(Context context, String patchUrl, String patchPath, String md5Url) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.TINKER, Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putString(Constants.PATCH_URL_KEY, patchUrl);
        preferencesEditor.putString(Constants.PATCH_PATH_KEY, patchPath);
        preferencesEditor.putString(Constants.MD5_URL_KEY, md5Url);
        preferencesEditor.apply();
    }

    public static void downloadAndUpdate(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.TINKER, Context.MODE_PRIVATE);
        String patchUrl = sharedPreferences.getString(Constants.PATCH_URL_KEY, "");
        String patchPath = sharedPreferences.getString(Constants.PATCH_PATH_KEY, "");

        downloadAndUpdate(context, patchUrl, patchPath, 1);
    }

    private static void downloadAndUpdate(final Context context, final String patchUrl, final String patchPath, final int retryDownloadCounter) {
        if (retryDownloadCounter > MAX_DOWNLOAD_RETRY) {
            TinkerLog.e("PatchingUtil", "Tinker patch: Reached maximum retry, exiting...");
            return;
        }
        Log.d("PatchingUtils", patchPath + " " + patchUrl);
        downloadApk(new DownloadTask() {
            @Override
            public void onFinish(boolean success) {
                if (success) {
                    TinkerInstaller.onReceiveUpgradePatch(context, patchPath);
                } else {
                    TinkerLog.e("PatchingUtil", "Download APK failed");
                    downloadAndUpdate(context, patchUrl, patchPath, retryDownloadCounter+1);
                }
            }
        }, patchUrl, patchPath);
    }

    private static void downloadApk(DownloadTask downloadTask, String url, String savePath) {
        BufferedInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new BufferedInputStream(new URL(url).openStream());
            outStream = new FileOutputStream(savePath);
            byte [] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inStream.read(dataBuffer, 0, 1024)) != -1) {
                outStream.write(dataBuffer, 0, bytesRead);
            }
            inStream.close();
            outStream.close();
            downloadTask.onFinish(true);
        } catch (IOException e) {
            e.printStackTrace();
            downloadTask.onFinish(false);
        } finally {
            try {
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private interface DownloadTask {
        void onFinish(boolean success);
    }

    public static Object getBuildConfigValue(Context context, String fieldName) {
        try {
            Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
