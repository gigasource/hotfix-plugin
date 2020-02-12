package io.gigasource.hotfix_plugin;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.jakewharton.processphoenix.ProcessPhoenix;
import com.tencent.tinker.lib.service.DefaultTinkerResultService;
import com.tencent.tinker.lib.service.PatchResult;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.lib.util.TinkerServiceInternals;

import java.io.File;

public class TinkerResultService extends DefaultTinkerResultService {
    private final String TAG = getClass().getSimpleName();

    @SuppressLint("DefaultLocale")
    @Override
    public void onPatchResult(final PatchResult result) {
        if (result == null) {
            TinkerLog.e(TAG, "TinkerResultService received null result!!!!");
            return;
        }
        TinkerLog.i(TAG, "TinkerResultService receive result: %s", result.toString());

        //first, we want to kill the recover process
        TinkerServiceInternals.killTinkerPatchServiceProcess(getApplicationContext());

        if (result.isSuccess) {
            deleteRawPatchFile(new File(result.rawPatchFilePath));

            if (checkIfNeedKill(result)) {
                try {
                    Thread.sleep(30 * 1000); // 30s
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                restartProcess();
            } else {
                TinkerLog.i(TAG, "I have already install the newly patch version!");
            }
        } else {
            TinkerLog.e(TAG, String.format("Update patch failed %d time(s), retry update process...", PatchingUtil.updateCounter));
            if (PatchingUtil.updateCounter < PatchingUtil.MAX_UPDATE_RETRY) {
                PatchingUtil.updateCounter += 1;
                PatchingUtil.downloadAndUpdate(getApplicationContext());
            } else {
                TinkerLog.e(TAG, String.format("Stop update process after %d times retry", PatchingUtil.MAX_UPDATE_RETRY));
            }
        }
    }

    private void restartProcess() {
        TinkerLog.i(TAG, "app is background now, i can kill quietly");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "New update is available. Restarting app", Toast.LENGTH_LONG).show();
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ProcessPhoenix.triggerRebirth(getApplicationContext());
            }
        }, 3000);
    }
}
