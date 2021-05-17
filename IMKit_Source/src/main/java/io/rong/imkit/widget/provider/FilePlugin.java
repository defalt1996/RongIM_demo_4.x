package io.rong.imkit.widget.provider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import java.util.HashSet;

import io.rong.common.LibStorageUtils;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongExtension;
import io.rong.imkit.RongIM;
import io.rong.imkit.activity.FileManagerActivity;
import io.rong.imkit.model.FileInfo;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imkit.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;

public class FilePlugin implements IPluginModule, IPluginRequestPermissionResultCallback {

    private static final String TAG = "FileInputProvider";
    private static final int REQUEST_FILE = 100;
    private static final int REQUEST_FILE_Q = 101;
    // 发送消息间隔
    private static final int TIME_DELAY = 400;

    private Conversation.ConversationType conversationType;
    private String targetId;


    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ic_files_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugins_files);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension) {
        conversationType = extension.getConversationType();
        targetId = extension.getTargetId();
        //Android10 使用系统文件浏览器，10 之前使用 FileManagerActivity
        if (LibStorageUtils.isBuildAndTargetForQ(currentFragment.getContext())) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            extension.startActivityForPluginResult(intent, REQUEST_FILE_Q, this);
        } else {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if (PermissionCheckUtil.checkPermissions(currentFragment.getContext(), permissions)) {
                Intent intent = new Intent(currentFragment.getActivity(), FileManagerActivity.class);
                extension.startActivityForPluginResult(intent, REQUEST_FILE, this);
            } else {
                extension.requestPermissionForPluginResult(permissions, IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN, this);
            }
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE) {
            if (data != null) {
                HashSet<FileInfo> selectedFileInfos = (HashSet<FileInfo>) data.getSerializableExtra("sendSelectedFiles");
                SendMediaMessageThread thread = new SendMediaMessageThread(conversationType, targetId, selectedFileInfos);
                thread.start();
            }
        } else if (requestCode == REQUEST_FILE_Q) {
            if (data != null) {
                Uri uri = data.getData();
                SendMediaMessageThreadForQ thread = new SendMediaMessageThreadForQ(conversationType, targetId, uri);
                thread.start();
            }
        }
    }

    @Override
    public boolean onRequestPermissionResult(Fragment currentFragment, RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            Intent intent = new Intent(currentFragment.getActivity(), FileManagerActivity.class);
            extension.startActivityForPluginResult(intent, REQUEST_FILE, this);
        } else {
            extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(currentFragment.getActivity(), permissions, grantResults));
        }

        return true;
    }

    private static class SendMediaMessageThreadForQ extends Thread {
        private Conversation.ConversationType conversationType;
        private String targetId;
        private Uri uri;

        private SendMediaMessageThreadForQ(Conversation.ConversationType conversationType, String targetId, Uri uri) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.uri = uri;
        }

        @Override
        public void run() {
            FileMessage fileMessage = FileMessage.obtain(RongIM.getInstance().getApplicationContext(), uri);
            if (fileMessage != null) {
                final Message message = Message.obtain(targetId, conversationType, fileMessage);
                RongIM.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    RLog.e(TAG, "sendMediaMessage e:" + e.toString());
                    Thread.currentThread().interrupt();
                }
            }

        }
    }

    private static class SendMediaMessageThread extends Thread {
        private Conversation.ConversationType conversationType;
        private String targetId;
        private HashSet<FileInfo> selectedFileInfos;


        private SendMediaMessageThread(Conversation.ConversationType conversationType, String targetId, HashSet<FileInfo> selectedFileInfos) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.selectedFileInfos = selectedFileInfos;
        }

        @Override
        public void run() {
            for (FileInfo fileInfo : selectedFileInfos) {
                Uri filePath = Uri.parse("file://" + fileInfo.getFilePath());
                FileMessage fileMessage = FileMessage.obtain(filePath);
                if (fileMessage != null) {
                    fileMessage.setType(fileInfo.getSuffix());
                    final Message message = Message.obtain(targetId, conversationType, fileMessage);
                    RongIM.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
                    try {
                        Thread.sleep(TIME_DELAY);
                    } catch (InterruptedException e) {
                        RLog.e(TAG, "sendMediaMessage e:" + e.toString());
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
