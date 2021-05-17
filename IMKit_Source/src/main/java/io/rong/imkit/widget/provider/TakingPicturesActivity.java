package io.rong.imkit.widget.provider;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.message.utils.BitmapUtil;

/**
 * Created by AMing on 2015/4/18.
 */
public class TakingPicturesActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "TakingPicturesActivity";
    private final static int REQUEST_CAMERA = 0x2;
    private static int COMPRESSED_SIZE = 1080;
    private ImageView mImage;
    private Uri mSavedPicUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_ac_camera);
        Button cancel = findViewById(R.id.rc_back);
        Button send = findViewById(R.id.rc_send);
        mImage = findViewById(R.id.rc_img);
        cancel.setOnClickListener(this);
        send.setOnClickListener(this);

        RLog.d(TAG, "onCreate savedInstanceState : " + savedInstanceState);

        try {
            Resources resources = getApplicationContext().getResources();
            COMPRESSED_SIZE = resources.getInteger(resources.getIdentifier("rc_image_size", "integer", getApplicationContext().getPackageName()));
        } catch (Exception e) {
            RLog.e(TAG, "onCreate", e);
        }

        if (savedInstanceState == null) {
            startCamera();
        } else {
            String str = savedInstanceState.getString("photo_uri");
            if (str != null) {
                mSavedPicUri = Uri.parse(str);
                try {
                    mImage.setImageBitmap(BitmapUtil.getNewResizedBitmap(this, mSavedPicUri, COMPRESSED_SIZE));
                } catch (IOException e) {
                    RLog.e(TAG, "onCreate", e);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View v) {
        final File file = new File(mSavedPicUri.getPath());

        if (!file.exists()) {
            finish();
        }

        if (v.getId() == R.id.rc_send) {
            if (mSavedPicUri != null) {
                Intent data = new Intent();
                data.setData(mSavedPicUri);
                setResult(RESULT_OK, data);
            }
            finish();
        } else if (v.getId() == R.id.rc_back) {
            finish();
        }
    }

    private void startCamera() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!path.exists())
            path.mkdirs();
        String name = System.currentTimeMillis() + ".jpg";
        File file = new File(path, name);
        mSavedPicUri = Uri.fromFile(file);
        RLog.d(TAG, "startCamera output pic uri =" + mSavedPicUri);

        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(this, getPackageName() + getString(R.string.rc_authorities_fileprovider), file);
        } catch (Exception e) {
            throw new RuntimeException("Please check IMKit Manifest FileProvider config.");
        }
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (SecurityException e) {
            Log.e("TakingPicturesActivity", "REQUEST_CAMERA SecurityException!!!");
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        RLog.d(TAG, "onActivityResult resultCode = " + resultCode + ", intent=" + data);

        if (resultCode != Activity.RESULT_OK) {
            finish();
            return;
        }

        if (requestCode == REQUEST_CAMERA) {
            if (mSavedPicUri != null) {
                try {
                    mImage.setImageBitmap(BitmapUtil.getNewResizedBitmap(this, mSavedPicUri, 1080));
                } catch (IOException e) {
                    RLog.e(TAG, "onActivityResult", e);
                }
            }
        } else {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //还原
        Log.e("TakingPicturesActivity", "onRestoreInstanceState");
        mSavedPicUri = Uri.parse(savedInstanceState.getString("photo_uri"));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //保存
        Log.e("TakingPicturesActivity", "onSaveInstanceState");
        outState.putString("photo_uri", mSavedPicUri.toString());
        super.onSaveInstanceState(outState);
    }
}
