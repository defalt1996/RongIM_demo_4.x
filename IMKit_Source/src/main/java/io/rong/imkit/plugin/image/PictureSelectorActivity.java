package io.rong.imkit.plugin.image;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.collection.ArrayMap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongBaseNoActionbarActivity;
import io.rong.imkit.utilities.KitStorageUtils;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imlib.IMLibExtensionModuleManager;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.HardwareResource;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;


public class PictureSelectorActivity extends RongBaseNoActionbarActivity {

    private final static String TAG = PictureSelectorActivity.class.getSimpleName();
    static public final int REQUEST_PREVIEW = 0;
    static public final int REQUEST_CAMERA = 1;
    static public final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    static public final int SIGHT_DEFAULT_DURATION_LIMIT = 300;  //默认值最好和NavigationCacheHelper中DEFAULT_VIDEO_TIME保持一致

    private GridView mGridView;
    private ImageButton mBtnBack;
    private Button mBtnSend;
    private PicTypeBtn mPicType;
    private PreviewBtn mPreviewBtn;
    private View mCatalogView;
    private ListView mCatalogListView;

    private List<MediaItem> mAllItemList = new ArrayList<>();
    private Map<String, List<MediaItem>> mItemMap = new ArrayMap<>();
    private ArrayList<Uri> mAllSelectedItemList = new ArrayList<>();
    private List<String> mCatalogList = new ArrayList<>();
    private String mCurrentCatalog = "";
    private Uri mTakePictureUri;
    private boolean mSendOrigin = false;
    private int perWidth;
    private int perHeight;
    private ExecutorService pool;

    private Handler bgHandler;
    private Handler uiHandler;
    private HandlerThread thread;


    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_picsel_activity);

        thread = new HandlerThread(TAG);
        thread.start();
        bgHandler = new Handler(thread.getLooper());
        uiHandler = new Handler(getMainLooper());


        mGridView = findViewById(R.id.gridlist);
        mBtnBack = findViewById(R.id.back);
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnSend = findViewById(R.id.send);
        mPicType = findViewById(R.id.pic_type);
        mPicType.init(this);
        mPicType.setEnabled(false);

        mPreviewBtn = findViewById(R.id.preview);
        mPreviewBtn.init(this);
        mPreviewBtn.setEnabled(false);
        mCatalogView = findViewById(R.id.catalog_window);
        mCatalogListView = findViewById(R.id.catalog_listview);

        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        initView();
    }

    interface IExecutedCallback {
        void executed();
    }


    private void initView() {
        updatePictureItems(new IExecutedCallback() {
            @Override
            public void executed() {
                if (uiHandler != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            initWidget();
                        }
                    });
                }
            }
        });
    }

    private void initWidget() {
        mGridView.setAdapter(new GridViewAdapter());
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    return;
                }

                ArrayList<MediaItem> itemList = new ArrayList<>();
                if (mCurrentCatalog.isEmpty()) {
                    itemList.addAll(mAllItemList);
                    PicItemHolder.itemList = itemList;
                    PicItemHolder.itemSelectedList = null;
                } else {
                    Map<String, List<MediaItem>> itemMap = mItemMap;
                    if (itemMap == null) {
                        return;
                    }
                    List<MediaItem> currentMediaItems = itemMap.get(mCurrentCatalog);
                    if (currentMediaItems != null) {
                        itemList.addAll(currentMediaItems);
                        PicItemHolder.itemList = itemList;
                    }
                    ArrayList<MediaItem> itemSelectList = new ArrayList<>();
                    for (String key : itemMap.keySet()) {
                        List<MediaItem> mediaItems = itemMap.get(key);
                        if (!key.equals(mCurrentCatalog) && mediaItems != null) {
                            for (MediaItem item : mediaItems) {
                                if (item.selected) {
                                    itemSelectList.add(item);
                                }
                            }
                        }
                    }
                    PicItemHolder.itemSelectedList = itemSelectList;
                }
                Intent intent = new Intent(PictureSelectorActivity.this, PicturePreviewActivity.class);
                intent.putExtra("index", position - 1);
                intent.putExtra("sendOrigin", mSendOrigin);
                startActivityForResult(intent, REQUEST_PREVIEW);
            }
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkedHashMap<String, Integer> mLinkedHashMap = new LinkedHashMap<>();
                for (Map.Entry<String, List<MediaItem>> entry : mItemMap.entrySet()) {
                    for (MediaItem item : entry.getValue()) {
                        if (item.selected) {
                            if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                String filePath;
                                String fileName = FileUtils.getFileNameWithPath(item.uri);
                                if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                    filePath = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + fileName;
                                } else if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                    filePath = KitStorageUtils.getVideoSavePath(PictureSelectorActivity.this) + File.separator + fileName;
                                } else {
                                    filePath = KitStorageUtils.getFileSavePath(PictureSelectorActivity.this) + File.separator + fileName;
                                }
                                boolean result = FileUtils.copyFile(PictureSelectorActivity.this.getApplicationContext(), Uri.parse(item.uri_sdk29), filePath);
                                if (result) {
                                    mLinkedHashMap.put("file://" + filePath, item.mediaType);
                                }
                            } else {
                                mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                            }
                        }
                    }
                }
                Gson gson = new Gson();
                String mediaList = gson.toJson(mLinkedHashMap);

                Intent data = new Intent();
                data.putExtra("sendOrigin", mSendOrigin);
                data.putExtra(Intent.EXTRA_RETURN_RESULT, mediaList);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mPicType.setEnabled(true);
        mPicType.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
        mPicType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCatalogView.setVisibility(View.VISIBLE);
            }
        });

        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PicItemHolder.itemList = new ArrayList<>();
                for (String key : mItemMap.keySet()) {
                    for (MediaItem item : mItemMap.get(key)) {
                        if (item.selected) {
                            PicItemHolder.itemList.add(item);
                        }
                    }
                }
                if (mAllSelectedItemList != null && PicItemHolder.itemList != null) {
                    for (int i = 0; i < mAllSelectedItemList.size(); i++) {
                        Uri imageUri = mAllSelectedItemList.get(i);
                        for (int j = i + 1; j < PicItemHolder.itemList.size(); j++) {
                            MediaItem mediaItem = PicItemHolder.itemList.get(j);
                            if (mediaItem != null && imageUri.toString().contains(mediaItem.uri)) {
                                PicItemHolder.itemList.remove(j);
                                PicItemHolder.itemList.add(i, mediaItem);
                            }
                        }
                    }
                }
                PicItemHolder.itemSelectedList = null;
                Intent intent = new Intent(PictureSelectorActivity.this, PicturePreviewActivity.class);
                intent.putExtra("sendOrigin", mSendOrigin);
                startActivityForResult(intent, REQUEST_PREVIEW);
            }
        });

        mCatalogView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && mCatalogView.getVisibility() == View.VISIBLE) {
                    mCatalogView.setVisibility(View.GONE);
                }
                return true;
            }
        });

        mCatalogListView.setAdapter(new CatalogAdapter());
        mCatalogListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String catalog;
                if (position == 0) {
                    catalog = "";
                } else {
                    catalog = mCatalogList.get(position - 1);
                }
                if (catalog.equals(mCurrentCatalog)) {
                    mCatalogView.setVisibility(View.GONE);
                    return;
                }

                mCurrentCatalog = catalog;
                TextView textView = view.findViewById(R.id.name);
                mPicType.setText(textView.getText().toString());
                mCatalogView.setVisibility(View.GONE);
                ((CatalogAdapter) mCatalogListView.getAdapter()).notifyDataSetChanged();
                ((GridViewAdapter) mGridView.getAdapter()).notifyDataSetChanged();
            }
        });

        perWidth = ((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth() / 3; // 3 column
        perHeight = ((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getHeight() / 5; // 5 row
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                initView();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.rc_permission_grant_needed), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (resultCode == RESULT_CANCELED) {
            return;
        } else if (resultCode == PicturePreviewActivity.RESULT_SEND) {
            setResult(RESULT_OK, data);
            finish();
            return;
        }

        switch (requestCode) {
            case REQUEST_PREVIEW: {
                mSendOrigin = data.getBooleanExtra("sendOrigin", false);
                GridViewAdapter gridViewAdapter = (GridViewAdapter) mGridView.getAdapter();
                if (gridViewAdapter != null) {
                    gridViewAdapter.notifyDataSetChanged();
                }

                CatalogAdapter catalogAdapter = (CatalogAdapter) mCatalogListView.getAdapter();
                if (catalogAdapter != null) {
                    catalogAdapter.notifyDataSetChanged();
                }
                updateToolbar();
            }
            break;
            case REQUEST_CAMERA: {
                if (mTakePictureUri == null)
                    break;

                PicItemHolder.itemList = new ArrayList<>();
                MediaItem item = new MediaItem();
                item.uri = mTakePictureUri.getPath();
                item.mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
                PicItemHolder.itemList.add(item);
                PicItemHolder.itemSelectedList = null;
                item.uri_sdk29 = mTakePictureUri.toString();
                Intent intent = new Intent(PictureSelectorActivity.this, PicturePreviewActivity.class);
                startActivityForResult(intent, REQUEST_PREVIEW);

                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{mTakePictureUri.getPath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        updatePictureItems(null);
                    }
                });
            }
            break;
            default:
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCatalogView != null && mCatalogView.getVisibility() == View.VISIBLE) {
                mCatalogView.setVisibility(View.GONE);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void requestCamera() {
        //判断正在视频通话和语音通话中不调用拍照
        if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.VIDEO)) {
            Toast.makeText(PictureSelectorActivity.this, getString(R.string.rc_voip_call_video_start_fail), Toast.LENGTH_LONG).show();
            return;
        } else if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.AUDIO)) {
            Toast.makeText(PictureSelectorActivity.this, getString(R.string.rc_voip_call_audio_start_fail), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resInfoList.size() <= 0) {
            Toast.makeText(this, getResources().getString(R.string.rc_voip_cpu_error), Toast.LENGTH_SHORT).show();
            return;
        }

        File path;

        if (KitStorageUtils.isBuildAndTargetForQ(this)) {
            String name = String.valueOf(System.currentTimeMillis());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.TITLE, name);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = this.getContentResolver();

            Uri insertUri = resolver.insert(external, values);
            mTakePictureUri = insertUri;

            intent.putExtra(MediaStore.EXTRA_OUTPUT, insertUri);
        } else {
            String name = System.currentTimeMillis() + ".jpg";

            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!path.exists())
                path.mkdirs();
            File file = new File(path, name);
            mTakePictureUri = Uri.fromFile(file);

            Uri uri;
            try {
                uri = FileProvider.getUriForFile(this, getPackageName() + getString(R.string.rc_authorities_fileprovider), file);
            } catch (Exception e) {
                RLog.e(TAG, "requestCamera", e);
                throw new RuntimeException("Please check IMKit Manifest FileProvider config. Please refer to http://support.rongcloud.cn/kb/NzA1");
            }

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void updatePictureItems(final IExecutedCallback iExecutedCallback) {
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                // A list of which columns to return.
                String[] projection = {
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.DATE_ADDED,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.MIME_TYPE,
                        MediaStore.Files.FileColumns.TITLE,
                        MediaStore.Video.Media.DURATION
                };

                String selection;
                Class clazz = null;
                try {
                    clazz = Class.forName("io.rong.sight.SightExtensionModule");
                } catch (ClassNotFoundException e) {
                    RLog.e(TAG, "updatePictureItems", e);
                }

                if (clazz == null) {
                    // Return only image metadata.
                    selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
                } else {
                    if (getResources().getBoolean(R.bool.rc_media_selector_contain_video)) {
                        // Return video and image metadata.
                        selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                                + " OR "
                                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                    } else {
                        // Return only image metadata.
                        selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
                    }
                }
                Uri queryUri = MediaStore.Files.getContentUri("external");

                CursorLoader cursorLoader = new CursorLoader(
                        PictureSelectorActivity.this,
                        queryUri,
                        projection,
                        selection,
                        null, // Selection args (none).
                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC" // Sort order.
                );

                Cursor cursor = cursorLoader.loadInBackground();
                mItemMap.clear();
                mAllItemList.clear();
                mCatalogList.clear();
                mAllSelectedItemList.clear();
                PicItemHolder.itemAllSelectedMediaItemList = new ArrayList<>();
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            MediaItem item = new MediaItem();
                            item.name = cursor.getString(5);
                            item.mediaType = cursor.getInt(3);
                            item.mimeType = cursor.getString(4);
                            item.uri = cursor.getString(1);//低版本uri路径继续获取，为生成相册列表所用
                            item.duration = cursor.getInt(6);
                            //Android Q 版本以上系统，新增uri_sdk29来存放通过MediaStore获取的uri路径，为后续获取Bitmap所用
                            Uri imageUri = ContentUris.withAppendedId(queryUri, cursor.getLong(0));
                            item.uri_sdk29 = imageUri.toString();
                            if (item.uri == null) {
                                continue;
                            }

                            if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                                    && item.duration == 0) {
                                continue;
                            }
                            // 屏蔽非 mp4 格式的视频文件
                            if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                                    && !"video/mp4".equals(item.mimeType)) {
                                continue;
                            }

                            File file = new File(item.uri);
                            if (!file.exists() || file.length() == 0) {
                                continue;
                            }

                            mAllItemList.add(item);

                            int last = item.uri.lastIndexOf("/");
                            String catalog;
                            if (last == -1) {
                                continue;
                            } else if (last == 0) {
                                catalog = "/";
                            } else {
                                int secondLast = item.uri.lastIndexOf("/", last - 1);
                                catalog = item.uri.substring(secondLast + 1, last);
                            }

                            // Add item to mItemList.
                            if (mItemMap.containsKey(catalog)) {
                                mItemMap.get(catalog).add(item);
                            } else {
                                ArrayList<MediaItem> itemList = new ArrayList<>();
                                itemList.add(item);
                                mItemMap.put(catalog, itemList);
                                mCatalogList.add(catalog);
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                    if (iExecutedCallback != null) {
                        iExecutedCallback.executed();
                    }
                }
            }
        });
    }

    private int getTotalSelectedNum() {
        int sum = 0;
        for (String key : mItemMap.keySet()) {
            List<MediaItem> mediaItemList = mItemMap.get(key);
            if (mediaItemList != null) {
                List<MediaItem> tempList = new ArrayList<>(mediaItemList);
                for (MediaItem item : tempList) {
                    if (item.selected) {
                        sum++;
                    }
                }
            }
        }
        return sum;
    }

    private void updateToolbar() {
        int sum = getTotalSelectedNum();
        if (sum == 0) {
            mBtnSend.setEnabled(false);
            mBtnSend.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
            mBtnSend.setText(R.string.rc_picsel_toolbar_send);
            mPreviewBtn.setEnabled(false);
            mPreviewBtn.setText(R.string.rc_picsel_toolbar_preview);
        } else if (sum <= 9) {
            mBtnSend.setEnabled(true);
            mBtnSend.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
            mBtnSend.setText(String.format(getResources().getString(R.string.rc_picsel_toolbar_send_num), sum));
            mPreviewBtn.setEnabled(true);
            mPreviewBtn.setText(String.format(getResources().getString(R.string.rc_picsel_toolbar_preview_num), sum));
        }
    }

    private MediaItem getItemAt(int index) {
        int sum = 0;
        for (String key : mItemMap.keySet()) {
            for (MediaItem item : mItemMap.get(key)) {
                if (sum == index) {
                    return item;
                }
                sum++;
            }
        }
        return null;
    }

    private MediaItem getItemAt(String catalog, int index) {
        if (!mItemMap.containsKey(catalog)) {
            return null;
        }
        int sum = 0;
        for (MediaItem item : mItemMap.get(catalog)) {
            if (sum == index) {
                return item;
            }
            sum++;
        }
        return null;
    }

    private MediaItem findByUri(String uri) {
        for (String key : mItemMap.keySet()) {
            for (MediaItem item : mItemMap.get(key)) {
                if (item.uri.equals(uri)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void setImageViewBackground(String imagePath, ImageView imageView, int position) {
        Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(imagePath, perWidth, perHeight, new AlbumBitmapCacheHelper.ILoadImageCallback() {
            @Override
            public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                if (bitmap == null) {
                    return;
                }
                BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
                View v = mGridView.findViewWithTag(path1);
                if (v != null)
                    v.setBackgroundDrawable(bd);
            }
        }, position);

        if (bitmap != null) {
            BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
            imageView.setBackgroundDrawable(bd);
        } else {
            imageView.setBackgroundResource(R.drawable.rc_grid_image_default);
        }
    }

    private class GridViewAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public GridViewAdapter() {
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            int sum = 1;
            if (mCurrentCatalog.isEmpty()) {
                for (String key : mItemMap.keySet()) {
                    List<MediaItem> mediaItems = mItemMap.get(key);
                    if (mediaItems != null) {
                        sum += mediaItems.size();
                    }
                }
            } else {
                List<MediaItem> mediaItems = mItemMap.get(mCurrentCatalog);
                if (mediaItems != null) {
                    sum += mediaItems.size();
                }
            }
            return sum;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @TargetApi(23)
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                View view = mInflater.inflate(R.layout.rc_picsel_grid_camera, parent, false);
                final ImageButton mask = view.findViewById(R.id.camera_mask);
                mask.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[] permissions = {Manifest.permission.CAMERA};
                        if (!PermissionCheckUtil.checkPermissions(PictureSelectorActivity.this, permissions)) {
                            PermissionCheckUtil.requestPermissions(PictureSelectorActivity.this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
                            return;
                        }
                        requestCamera();
                    }
                });
                return view;
            }

            final MediaItem item;
            if (mCurrentCatalog.isEmpty()) {
                item = mAllItemList.get(position - 1);
            } else {
                item = getItemAt(mCurrentCatalog, position - 1);
            }

            View view = convertView;
            final ViewHolder holder;
            if (view == null || view.getTag() == null) {
                view = mInflater.inflate(R.layout.rc_picsel_grid_item, parent, false);
                holder = new ViewHolder();
                holder.image = view.findViewById(R.id.image);
                holder.mask = view.findViewById(R.id.mask);
                holder.checkBox = view.findViewById(R.id.checkbox);
                holder.videoContainer = view.findViewById(R.id.video_container);
                holder.videoDuration = view.findViewById(R.id.video_duration);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (holder.image.getTag() != null) {
                String path = (String) holder.image.getTag();
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
            }

            String thumbImagePath = "";
            if (item == null) {
                return view;
            }
            switch (item.mediaType) {
                case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                    //适配Android Q，thumbImagePath使用uri_sdk29
                    if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                        thumbImagePath = item.uri_sdk29;
                    } else {
                        thumbImagePath = item.uri;
                    }
                    break;
                case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
                    thumbImagePath = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + item.name;
                    if (!new File(thumbImagePath).exists()) {
                        Runnable runnable = new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.P)
                            @Override
                            public void run() {
                                Bitmap videoFrame = null;
                                if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                    try {
                                        MediaMetadataRetriever media = new MediaMetadataRetriever();
                                        media.setDataSource(getApplicationContext(), Uri.parse(item.uri_sdk29));
                                        videoFrame = media.getFrameAtTime();
                                    } catch (Exception e) {
                                        RLog.e(TAG, "video get thumbnail error", e);
                                    }
                                } else {
                                    videoFrame = ThumbnailUtils.createVideoThumbnail(item.uri, MINI_KIND);
                                }
                                if (videoFrame != null) {
                                    final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), item.name);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                        }
                                    });
                                }
                            }
                        };
                        pool.execute(runnable);
                    }
                    break;
                default:
                    break;
            }
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(thumbImagePath);
            holder.image.setTag(thumbImagePath);
            setImageViewBackground(thumbImagePath, holder.image, position);

            if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                holder.videoContainer.setVisibility(View.VISIBLE);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(item.duration);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(item.duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(item.duration));
                holder.videoDuration.setText(String.format(Locale.CHINA,
                        seconds < 10 ? "%d:0%d" : "%d:%d", minutes, seconds));
            } else {
                holder.videoContainer.setVisibility(View.GONE);
            }

            holder.checkBox.setChecked(item.selected);
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        if (TextUtils.isEmpty(holder.videoDuration.getText())) return;
                        int maxDuration = RongIMClient.getInstance().getVideoLimitTime();
                        if (maxDuration < 1) maxDuration = SIGHT_DEFAULT_DURATION_LIMIT;
                        String[] videoTime = holder.videoDuration.getText().toString().split(":");
                        if ((Integer.parseInt(videoTime[0]) * 60 + Integer.parseInt(videoTime[1])) > maxDuration) {
                            new AlertDialog.Builder(PictureSelectorActivity.this)
                                    .setMessage(getResources().getString(R.string.rc_picsel_selected_max_time_span_with_param, maxDuration / 60))
                                    .setPositiveButton(R.string.rc_confirm, null)
                                    .setCancelable(false)
                                    .create()
                                    .show();
                            return;
                        }
                    }

                    if (item.uri.endsWith(".gif")) {
                        int length = RongIMClient.getInstance().getGIFLimitSize() * 1024;
                        File file = new File(item.uri);
                        if (file != null && file.exists() && file.length() > length) {
                            new AlertDialog.Builder(PictureSelectorActivity.this)
                                    .setMessage(getResources().getString(R.string.rc_picsel_selected_max_gif_size_span_with_param))
                                    .setPositiveButton(R.string.rc_confirm, null)
                                    .setCancelable(false)
                                    .create()
                                    .show();
                            return;
                        }
                    }

                    if (!holder.checkBox.getChecked() && getTotalSelectedNum() == 9) {
                        Toast.makeText(PictureSelectorActivity.this, R.string.rc_picsel_selected_max_pic_count, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    holder.checkBox.setChecked(!holder.checkBox.getChecked());
                    item.selected = holder.checkBox.getChecked();
                    if (item.selected) {
                        mAllSelectedItemList.add(Uri.parse("file://" + item.uri));
                        if (PicItemHolder.itemAllSelectedMediaItemList != null) {
                            PicItemHolder.itemAllSelectedMediaItemList.add(item);
                        }
                        holder.mask.setBackgroundColor(getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                    } else {
                        try {
                            mAllSelectedItemList.remove(Uri.parse("file://" + item.uri));
                        } catch (Exception e) {
                            RLog.e(TAG, "GridViewAdapter getView", e);
                        }
                        if (PicItemHolder.itemAllSelectedMediaItemList != null) {
                            PicItemHolder.itemAllSelectedMediaItemList.remove(item);
                        }
                        holder.mask.setBackgroundDrawable(getResources().getDrawable(R.drawable.rc_sp_grid_mask));
                    }
                    updateToolbar();
                }
            });
            if (item.selected) {
                holder.mask.setBackgroundColor(getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
            } else {
                holder.mask.setBackgroundDrawable(getResources().getDrawable(R.drawable.rc_sp_grid_mask));
            }

            return view;
        }

        private class ViewHolder {
            ImageView image;
            View mask;
            SelectBox checkBox;
            View videoContainer;
            TextView videoDuration;
        }
    }

    private class CatalogAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public CatalogAdapter() {
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mItemMap.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.rc_picsel_catalog_listview, parent, false);
                holder = new ViewHolder();
                holder.image = view.findViewById(R.id.image);
                holder.name = view.findViewById(R.id.name);
                holder.number = view.findViewById(R.id.number);
                holder.selected = view.findViewById(R.id.selected);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (holder.image.getTag() != null) {
                String path = (String) holder.image.getTag();
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
            }

            String path = "";
            String name;
            int num = 0;
            boolean showSelected = false;
            if (position == 0) {
                if (mItemMap.size() == 0) {
                    holder.image.setImageResource(R.drawable.rc_picsel_empty_pic);
                } else {
                    List<MediaItem> mediaItems = mItemMap.get(mCatalogList.get(0));
                    if (mediaItems != null && mediaItems.size() > 0) {
                        final MediaItem mediaItem = mediaItems.get(0);
                        if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                            //适配Android Q，thumbImagePath使用uri_sdk29
                            if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                path = mediaItem.uri_sdk29;
                            } else {
                                path = mediaItem.uri;
                            }
                        } else {
                            path = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + mediaItem.name;
                            if (!new File(path).exists()) {
                                new Thread(new Runnable() {
                                    @RequiresApi(api = Build.VERSION_CODES.P)
                                    @Override
                                    public void run() {
                                        Bitmap videoFrame = null;
                                        if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                            try {
                                                MediaMetadataRetriever media = new MediaMetadataRetriever();
                                                media.setDataSource(getApplicationContext(), Uri.parse(mediaItem.uri_sdk29));
                                                videoFrame = media.getFrameAtTime();
                                            } catch (Exception e) {
                                                RLog.e(TAG, "video get thumbnail error", e);
                                            }
                                        } else {
                                            videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItem.uri, MINI_KIND);
                                        }
                                        if (videoFrame != null) {
                                            final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), mediaItem.name);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(path)) {
                        AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                        holder.image.setTag(path);
                        setImageViewBackground(path, holder.image, position);
                    }
                }
                name = getResources().getString(R.string.rc_picsel_catalog_allpic);
                holder.number.setVisibility(View.GONE);
                showSelected = mCurrentCatalog.isEmpty();
            } else {
                final MediaItem mediaItem = mItemMap.get(mCatalogList.get(position - 1)).get(0);
                if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                    //适配Android Q，thumbImagePath使用uri_sdk29
                    if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                        path = mediaItem.uri_sdk29;
                    } else {
                        path = mediaItem.uri;
                    }
                } else {
                    path = KitStorageUtils.getImageSavePath(PictureSelectorActivity.this) + File.separator + mediaItem.name;
                    if (!new File(path).exists()) {
                        new Thread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.P)
                            @Override
                            public void run() {
                                Bitmap videoFrame = null;
                                if (KitStorageUtils.isBuildAndTargetForQ(PictureSelectorActivity.this)) {
                                    try {
                                        MediaMetadataRetriever media = new MediaMetadataRetriever();
                                        media.setDataSource(getApplicationContext(), Uri.parse(mediaItem.uri_sdk29));
                                        videoFrame = media.getFrameAtTime();
                                    } catch (Exception e) {
                                        RLog.e(TAG, "video get thumbnail error", e);
                                    }
                                } else {
                                    videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItem.uri, MINI_KIND);
                                }
                                if (videoFrame != null) {
                                    final File captureImageFile = FileUtils.convertBitmap2File(videoFrame, KitStorageUtils.getImageSavePath(PictureSelectorActivity.this), mediaItem.name);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setImageViewBackground(captureImageFile.getAbsolutePath(), holder.image, position);
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                }
                name = mCatalogList.get(position - 1);
                num = mItemMap.get(mCatalogList.get(position - 1)).size();
                holder.number.setVisibility(View.VISIBLE);
                showSelected = name.equals(mCurrentCatalog);

                AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                holder.image.setTag(path);
                setImageViewBackground(path, holder.image, position);
            }
            holder.name.setText(name);
            holder.number.setText(String.format(getResources().getString(R.string.rc_picsel_catalog_number), num));
            holder.selected.setVisibility(showSelected ? View.VISIBLE : View.INVISIBLE);
            return view;
        }

        private class ViewHolder {
            ImageView image;
            TextView name;
            TextView number;
            ImageView selected;
        }
    }

    static public class MediaItem implements Parcelable {
        String name;
        int mediaType;
        String mimeType;
        String uri;
        boolean selected;
        int duration;
        String uri_sdk29;//适配AndroidQ系统新增uri属性

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.name);
            dest.writeInt(this.mediaType);
            dest.writeString(this.mimeType);
            dest.writeString(this.uri);
            dest.writeByte(this.selected ? (byte) 1 : (byte) 0);
            dest.writeInt(this.duration);
            dest.writeString(this.uri_sdk29);
        }

        public MediaItem() {
        }

        protected MediaItem(Parcel in) {
            this.name = in.readString();
            this.mediaType = in.readInt();
            this.mimeType = in.readString();
            this.uri = in.readString();
            this.selected = in.readByte() != 0;
            this.duration = in.readInt();
            this.uri_sdk29 = in.readString();
        }

        public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
            @Override
            public MediaItem createFromParcel(Parcel source) {
                return new MediaItem(source);
            }

            @Override
            public MediaItem[] newArray(int size) {
                return new MediaItem[size];
            }
        };
    }

    static public class PicTypeBtn extends LinearLayout {

        TextView mText;

        public PicTypeBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(Activity root) {
            mText = (TextView) root.findViewById(R.id.type_text);
        }

        public void setText(String text) {
            mText.setText(text);
        }

        public void setTextColor(int color) {
            mText.setTextColor(color);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isEnabled()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mText.setVisibility(View.INVISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        mText.setVisibility(View.VISIBLE);
                        break;
                    default:
                }
            }
            return super.onTouchEvent(event);
        }
    }

    static public class PreviewBtn extends LinearLayout {

        private TextView mText;

        public PreviewBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(Activity root) {
            mText = (TextView) root.findViewById(R.id.preview_text);
        }

        public void setText(int id) {
            mText.setText(id);
        }

        public void setText(String text) {
            mText.setText(text);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            int color = enabled ? R.color.rc_picsel_toolbar_send_text_normal
                    : R.color.rc_picsel_toolbar_send_text_disable;
            mText.setTextColor(getResources().getColor(color));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isEnabled()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mText.setVisibility(View.INVISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        mText.setVisibility(View.VISIBLE);
                        break;
                    default:
                }
            }
            return super.onTouchEvent(event);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        initView();
                    } else if (permissions[0].equals(Manifest.permission.CAMERA)) {
                        requestCamera();
                    }
                } else if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.rc_permission_grant_needed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.rc_permission_grant_needed), Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    static public class SelectBox extends ImageView {

        private boolean mIsChecked;

        public SelectBox(Context context, AttributeSet attrs) {
            super(context, attrs);
            setImageResource(R.drawable.rc_select_check_nor);
        }

        public void setChecked(boolean check) {
            mIsChecked = check;
            setImageResource(mIsChecked ? R.drawable.rc_select_check_sel : R.drawable.rc_select_check_nor);
        }

        public boolean getChecked() {
            return mIsChecked;
        }
    }

    @Override
    protected void onDestroy() {
        PicItemHolder.itemList = null;
        PicItemHolder.itemSelectedList = null;
        PicItemHolder.itemAllSelectedMediaItemList = null;
        thread.quit();
        bgHandler.removeCallbacks(thread);
        bgHandler = null;
        uiHandler = null;
        // shutdownAndAwaitTermination(pool);
        super.onDestroy();
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    static class PicItemHolder {
        static ArrayList<MediaItem> itemList;
        //从某个子Catalog预览时需要传入当前已经选择的图片且不包含子Catalog中选择的图片
        static ArrayList<MediaItem> itemSelectedList;
        //被选中图片条目保存 list
        static ArrayList<MediaItem> itemAllSelectedMediaItemList;
    }


    private String formatSize(long length) {
        if (length > 1024 * 1024) { // M
            float size = Math.round(length / (1024f * 1024f) * 100) / 100f;
            return size + "M";
        } else if (length > 1024) {
            float size = Math.round(length / (1024f) * 100) / 100f;
            return size + "KB";
        } else {
            return length + "B";
        }
    }
}
