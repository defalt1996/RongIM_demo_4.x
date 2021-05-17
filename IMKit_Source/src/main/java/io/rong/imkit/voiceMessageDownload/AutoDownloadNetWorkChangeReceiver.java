package io.rong.imkit.voiceMessageDownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AutoDownloadNetWorkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        try {
            networkInfo = cm.getActiveNetworkInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean networkAvailable = networkInfo != null && (networkInfo.isAvailable() && networkInfo.isConnected());
        if ((ConnectivityManager.CONNECTIVITY_ACTION).equals(intent.getAction()) && networkAvailable) {
            HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
        } else {
            HQVoiceMsgDownloadManager.getInstance().pauseDownloadService();
        }
    }
}
