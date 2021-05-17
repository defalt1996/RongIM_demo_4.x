package io.rong.imkit.utils;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class ImageDownloadManager {

    private static final String TAG = ImageDownloadManager.class.getSimpleName();

    private static ImageDownloadManager instance;

    private static Context mContext;

    private DownloadManager downloadManager;

    private String savePath = "download";

    private long taskId;

    private String imageString;

    private DownloadStatusListener downloadStatusListener;


    private ImageDownloadManager() {
        if (mContext == null) {
            throw new NullPointerException("mContext is empty ,need invoke init!");
        }
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static void init(Context context) {
        mContext = context;
    }

    public static ImageDownloadManager getInstance() {
        if (instance == null) {
            synchronized (ImageDownloadManager.class) {
                if (instance == null) {
                    instance = new ImageDownloadManager();
                }
            }
        }
        return instance;
    }

    /**
     * 对外暴露接口的核心方法
     *
     * @param remotePath             图片路径
     * @param downloadStatusListener 下载状态监听
     */
    public void downloadImage(String remotePath, DownloadStatusListener downloadStatusListener) {
        if (TextUtils.isEmpty(remotePath) && downloadStatusListener == null) {
            throw new NullPointerException("parameter is error");
        }

        this.downloadStatusListener = downloadStatusListener;

        if (!canDownloadManagerState()) {
            Log.e(TAG, "DownloadManager is disable or Device ROM remove it");
            downloadStatusListener.downloadFailed(DownloadStatusError.DEVICE_DISABLE);
            return;
        }
        //Android 10 版本之后不支持公有目录，所以统一修改为私有目录
        int length = remotePath.length();
        if (length <= 7) {
            Log.e(TAG, "DownloadManager remotePath less than 7");
            return;
        }
        imageString = remotePath.substring(length - 7);
        String path = mContext.getExternalFilesDir(savePath) + "/" + imageString + ".jpg";
        if (new File(path).exists()) {
            downloadStatusListener.downloadSuccess(path, null);
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(remotePath));
            request.setDestinationInExternalFilesDir(mContext, savePath, imageString + ".jpg");
            setTaskId(downloadManager.enqueue(request));
            mContext.registerReceiver(receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Can only download HTTP/HTTPS URIs");
            downloadStatusListener.downloadFailed(DownloadStatusError.DOWNLOAD_FAILED);
        } catch (IllegalStateException e) {
            Log.e(TAG, "The external storage directory cannot be found or created");
            downloadStatusListener.downloadFailed(DownloadStatusError.DOWNLOAD_FAILED);
        } catch (SecurityException e) {
            Log.e(TAG, "downloadImage securityException");
            downloadStatusListener.downloadFailed(DownloadStatusError.DOWNLOAD_FAILED);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkDownloadStatus();//检查下载状态
        }
    };


    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(getTaskId());//筛选下载任务，传入任务ID，可变参数
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    Log.e(TAG, "STATUS_PAUSED");
                    break;
                case DownloadManager.STATUS_PENDING:
                    Log.e(TAG, "STATUS_PENDING");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Log.e(TAG, "STATUS_RUNNING");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    String path = mContext.getExternalFilesDir(savePath) + "/" + imageString + ".jpg";
                    downloadStatusListener.downloadSuccess("file://" + path, null);
                    mContext.unregisterReceiver(receiver);
                    Log.e(TAG, "STATUS_SUCCESSFUL PATH: " + path);
                    break;
                case DownloadManager.STATUS_FAILED:
                    downloadStatusListener.downloadFailed(DownloadStatusError.DOWNLOAD_FAILED);
                    Log.e(TAG, "STATUS_FAILED");
                    break;
            }
        }
    }

    /**
     * 检查下载管理器是否可用
     *
     * @return 是否可用
     */
    private boolean canDownloadManagerState() {
        try {
            int state = mContext.getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads");

            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public interface DownloadStatusListener {
        void downloadSuccess(String localPath, Bitmap bitmap);

        void downloadFailed(DownloadStatusError error);
    }

    public enum DownloadStatusError {
        /**
         * DownloadManager is disable or ROM remove it
         */
        DEVICE_DISABLE,
        /**
         * Download failed
         */
        DOWNLOAD_FAILED
    }


    private long getTaskId() {
        return taskId;
    }

    private void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
