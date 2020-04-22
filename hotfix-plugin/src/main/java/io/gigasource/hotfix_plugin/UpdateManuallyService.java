package io.gigasource.hotfix_plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.tencent.tinker.lib.util.TinkerLog;

public class UpdateManuallyService extends Service {
    private final String TAG = getClass().getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TinkerLog.d(TAG, "Load patch");
        new Thread(new Runnable() {
            @Override
            public void run() {
                PatchingUtil.checkForUpdate(getApplicationContext());
            }
        }).start();

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
