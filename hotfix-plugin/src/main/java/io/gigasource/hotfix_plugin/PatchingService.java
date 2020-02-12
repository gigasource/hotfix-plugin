package io.gigasource.hotfix_plugin;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.tencent.tinker.lib.util.TinkerLog;

public class PatchingService extends FirebaseMessagingService {
    private final String TAG = "PatchingService";
    private final String LOAD_PATCH = "load_patch";
    private final String CLEAN_PATCH = "clean_patch";
    private final String LOAD_LIBRARY = "load_library";
    private final String KILL_PROCESS = "kill_process";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String command = remoteMessage.getData().get("command");
        Log.d("Tinker command", command);
        switch (command) {
            case LOAD_PATCH:
                TinkerLog.d(TAG, "Load patch");

                String domain = remoteMessage.getData().get("domain");
                PatchingUtil.checkForUpdate(getApplicationContext(), domain);
                break;
            case LOAD_LIBRARY:
                TinkerLog.d(TAG, "Load library");
                PatchingUtil.loadLibrary(getApplicationContext());
                break;
            case CLEAN_PATCH:
                TinkerLog.d(TAG, "Clean patch");
                PatchingUtil.cleanPatch(getApplicationContext());
                break;
            case KILL_PROCESS:
                TinkerLog.d(TAG, "Kill process");
                PatchingUtil.killProcess(getApplicationContext());
                break;
        }
    }
}
