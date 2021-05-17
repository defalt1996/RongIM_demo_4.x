package io.rong.imkit.plugin.image;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import io.rong.common.FileInfo;
import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongBaseNoActionbarActivity;
import io.rong.imkit.plugin.image.PictureSelectorActivity.MediaItem;
import io.rong.imkit.utilities.KitStorageUtils;
import io.rong.imlib.RongIMClient;
import io.rong.subscaleview.ImageSource;
import io.rong.subscaleview.SubsamplingScaleImageView;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;

public class PicturePreviewActivity extends RongBaseNoActionbarActivity {

    private final static String TAG = "PicturePreviewActivity";
    static public final int RESULT_SEND = 1;

    private TextView mIndexTotal;
    private View mWholeView;
    private View mToolbarTop;
    private View mToolbarBottom;
    private ImageButton mBtnBack;
    private Button mBtnSend;
    private CheckButton mUseOrigin;
    private CheckButton mSelectBox;
    private HackyViewPager mViewPager;

    private ArrayList<MediaItem> mItemList;
    private ArrayList<MediaItem> mItemSelectedList;
    private ArrayList<MediaItem> mItemAllSelectedList;
    private int mCurrentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_picprev_activity);
        initView();

        mUseOrigin.setChecked(getIntent().getBooleanExtra("sendOrigin", false));
        mCurrentIndex = getIntent().getIntExtra("index", 0);
        if (mItemList == null) {
            mItemList = PictureSelectorActivity.PicItemHolder.itemList;
            mItemSelectedList = PictureSelectorActivity.PicItemHolder.itemSelectedList;
            mItemAllSelectedList = PictureSelectorActivity.PicItemHolder.itemAllSelectedMediaItemList;
        }
        if (mItemList == null) {
            RLog.e(TAG, "Itemlist is null");
            return;
        }
        mIndexTotal.setText(String.format("%d/%d", mCurrentIndex + 1, mItemList.size()));

        if (Build.VERSION.SDK_INT >= 11) {
            mWholeView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            int margin = getSmartBarHeight(this);
            if (margin > 0) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mToolbarBottom.getLayoutParams();
                lp.setMargins(0, 0, 0, margin);
                mToolbarBottom.setLayoutParams(lp);
            }
        }

        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mToolbarTop.getLayoutParams());
        lp.setMargins(0, result, 0, 0);
        mToolbarTop.setLayoutParams(lp);

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("sendOrigin", mUseOrigin.getChecked());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkedHashMap<String, Integer> mLinkedHashMap = new LinkedHashMap<>();
                if (mItemSelectedList != null) {
                    for (MediaItem item : mItemSelectedList) {
                        if (item.selected) {
                            if (KitStorageUtils.isBuildAndTargetForQ(getApplicationContext())) {
                                String filePath;
                                String fileName = FileUtils.getFileNameWithPath(item.uri);
                                if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                    filePath = KitStorageUtils.getImageSavePath(getApplicationContext()) + File.separator + fileName;
                                } else if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                    filePath = KitStorageUtils.getVideoSavePath(getApplicationContext()) + File.separator + fileName;
                                } else {
                                    filePath = KitStorageUtils.getFileSavePath(getApplicationContext()) + File.separator + fileName;
                                }

                                boolean result = FileUtils.copyFile(v.getContext(), Uri.parse(item.uri_sdk29), filePath);
                                if (result) {
                                    mLinkedHashMap.put("file://" + filePath, item.mediaType);
                                }
                            } else {
                                mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                            }
                        }
                    }
                }
                for (MediaItem item : mItemList) {
                    if (item.selected) {
                        if (KitStorageUtils.isBuildAndTargetForQ(getApplicationContext())) {
                            String filePath;
                            String fileName = FileUtils.getFileNameWithPath(item.uri);
                            if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                filePath = KitStorageUtils.getImageSavePath(getApplicationContext()) + File.separator + fileName;
                            } else if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                filePath = KitStorageUtils.getVideoSavePath(getApplicationContext()) + File.separator + fileName;
                            } else {
                                filePath = KitStorageUtils.getFileSavePath(getApplicationContext()) + File.separator + fileName;
                            }

                            boolean result = FileUtils.copyFile(getApplicationContext(), Uri.parse(item.uri_sdk29), filePath);
                            if (result) {
                                mLinkedHashMap.put("file://" + filePath, item.mediaType);
                            }
                        } else {
                            mLinkedHashMap.put("file://" + item.uri, item.mediaType);
                        }
                    }
                }
                Gson gson = new Gson();
                String mediaList = gson.toJson(mLinkedHashMap);

                Intent data = new Intent();
                data.putExtra("sendOrigin", mUseOrigin.getChecked());
                data.putExtra(Intent.EXTRA_RETURN_RESULT, mediaList);
                setResult(RESULT_SEND, data);
                finish();
            }
        });

        mUseOrigin.setText(R.string.rc_picprev_origin);
        mUseOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MediaItem item = mItemList.get(mCurrentIndex);
                if (item.uri.endsWith(".gif")) {
                    int length = RongIMClient.getInstance().getGIFLimitSize() * 1024;
                    File file = new File(item.uri);
                    if (file != null && file.exists() && file.length() > length) {
                        new AlertDialog.Builder(PicturePreviewActivity.this)
                                .setMessage(getResources().getString(R.string.rc_picsel_selected_max_gif_size_span_with_param))
                                .setPositiveButton(R.string.rc_confirm, null)
                                .setCancelable(false)
                                .create()
                                .show();
                        return;
                    }
                }

                mUseOrigin.setChecked(!mUseOrigin.getChecked());
                if (mUseOrigin.getChecked() && getTotalSelectedNum() == 0) {
                    mSelectBox.setChecked(!mSelectBox.getChecked());
                    mItemList.get(mCurrentIndex).selected = mSelectBox.getChecked();
                    updateToolbar();
                }
            }
        });
        mSelectBox.setText(R.string.rc_picprev_select);
        mSelectBox.setChecked(mItemList.get(mCurrentIndex).selected);
        mSelectBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaItem item = mItemList.get(mCurrentIndex);
                if (item.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    int maxDuration = RongIMClient.getInstance().getVideoLimitTime();
                    if (maxDuration < 1)
                        maxDuration = PictureSelectorActivity.SIGHT_DEFAULT_DURATION_LIMIT;
                    if (TimeUnit.MILLISECONDS.toSeconds(item.duration) > maxDuration) {
                        new AlertDialog.Builder(PicturePreviewActivity.this)
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
                        new AlertDialog.Builder(PicturePreviewActivity.this)
                                .setMessage(getResources().getString(R.string.rc_picsel_selected_max_gif_size_span_with_param))
                                .setPositiveButton(R.string.rc_confirm, null)
                                .setCancelable(false)
                                .create()
                                .show();
                        return;
                    }
                }

                if (!mSelectBox.getChecked() && getTotalSelectedNum() == 9) {
                    Toast.makeText(PicturePreviewActivity.this, R.string.rc_picsel_selected_max_pic_count, Toast.LENGTH_SHORT).show();
                    return;
                }

                mSelectBox.setChecked(!mSelectBox.getChecked());
                mItemList.get(mCurrentIndex).selected = mSelectBox.getChecked();
                if (mItemAllSelectedList != null) {
                    if (mSelectBox.getChecked()) {
                        mItemAllSelectedList.add(mItemList.get(mCurrentIndex));
                    } else {
                        mItemAllSelectedList.remove(mItemList.get(mCurrentIndex));
                    }
                } else {
                    RLog.e(TAG, "mItemAllSelectedList is null");
                }
                updateToolbar();
            }
        });

        mViewPager.setAdapter(new PreviewAdapter());
        mViewPager.setCurrentItem(mCurrentIndex);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentIndex = position;
                mIndexTotal.setText(String.format("%d/%d", position + 1, mItemList.size()));
                mSelectBox.setChecked(mItemList.get(position).selected);
                MediaItem mediaItem = mItemList.get(position);
                updateToolbar();
                if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    mUseOrigin.rootView.setVisibility(View.GONE);
                } else {
                    mUseOrigin.rootView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        updateToolbar();
    }

    private void initView() {
        mToolbarTop = findViewById(R.id.toolbar_top);
        mIndexTotal = findViewById(R.id.index_total);
        mBtnBack = findViewById(R.id.back);
        mBtnSend = findViewById(R.id.send);

        mWholeView = findViewById(R.id.whole_layout);
        mViewPager = findViewById(R.id.viewpager);

        mToolbarBottom = findViewById(R.id.toolbar_bottom);
        mUseOrigin = new CheckButton(findViewById(R.id.origin_check), R.drawable.rc_origin_check_nor, R.drawable.rc_origin_check_sel);
        mSelectBox = new CheckButton(findViewById(R.id.select_check), R.drawable.rc_select_check_nor, R.drawable.rc_select_check_sel);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.putExtra("sendOrigin", mUseOrigin.getChecked());
            setResult(RESULT_OK, intent);
        }
        return super.onKeyDown(keyCode, event);
    }

    private int getTotalSelectedNum() {
        int sum = 0;
        for (int i = 0; i < mItemList.size(); i++) {
            if (mItemList.get(i).selected) {
                sum++;
            }
        }
        if (mItemSelectedList != null) {
            sum += mItemSelectedList.size();
        }
        return sum;
    }

    private String getTotalSelectedSize() {
        float size = 0;
        for (int i = 0; i < mItemList.size(); i++) {
            if (mItemList.get(i).selected) {
                File file = new File(mItemList.get(i).uri);
                size = size + file.length() / 1024f;
            }
        }

        if (mItemSelectedList != null) {
            for (int i = 0; i < mItemSelectedList.size(); i++) {
                if (mItemSelectedList.get(i).selected) {
                    File file = new File(mItemSelectedList.get(i).uri);
                    size = size + file.length() / 1024f;
                }
            }
        }
        String totalSize;
        if (size < 1024) {
            totalSize = String.format("%.0fK", size);
        } else {
            totalSize = String.format("%.1fM", size / 1024);
        }
        return totalSize;
    }

    private String getSelectedSize(int index) {
        float size = 0;

        if (mItemList != null && mItemList.size() > 0) {
            long maxSize = 0;
            if (KitStorageUtils.isBuildAndTargetForQ(this)) {
                FileInfo fileInfo = FileUtils.getFileInfoByUri(this, Uri.parse(mItemList.get(index).uri_sdk29));
                if (fileInfo != null)
                    maxSize = fileInfo.getSize();
            } else {
                maxSize = new File(mItemList.get(index).uri).length();
            }
            size = maxSize / 1024f;
        }

        String returnSize;
        if (size < 1024) {
            returnSize = String.format("%.0fK", size);
        } else {
            returnSize = String.format("%.1fM", size / 1024);
        }
        return returnSize;
    }

    private void updateToolbar() {
        int selNum = getTotalSelectedNum();
        if (mItemList.size() == 1 && selNum == 0) {
            mBtnSend.setText(R.string.rc_picsel_toolbar_send);
            mUseOrigin.setText(R.string.rc_picprev_origin);
            mBtnSend.setEnabled(false);
            mBtnSend.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
            return;
        }

        if (selNum == 0) {
            mBtnSend.setText(R.string.rc_picsel_toolbar_send);
            mUseOrigin.setText(R.string.rc_picprev_origin);
            mUseOrigin.setChecked(false);
            mBtnSend.setEnabled(false);
            mBtnSend.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_disable));
        } else if (selNum <= 9) {
            mBtnSend.setEnabled(true);
            mBtnSend.setTextColor(getResources().getColor(R.color.rc_picsel_toolbar_send_text_normal));
            mBtnSend.setText(String.format(getResources().getString(R.string.rc_picsel_toolbar_send_num), selNum));
        }
        mUseOrigin.setText(String.format(getResources().getString(R.string.rc_picprev_origin_size), getSelectedSize(mCurrentIndex)));
        MediaItem mediaItem = mItemList.get(mCurrentIndex);
        if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            mUseOrigin.rootView.setVisibility(View.GONE);
        } else {
            mUseOrigin.rootView.setVisibility(View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static int getSmartBarHeight(Context context) {
        try {
            Class c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("mz_action_button_min_height");
            int height = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            RLog.e(TAG, "getSmartBarHeight", e);
        }
        return 0;
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */

    public int readPictureDegree(String path, Context context) {
        return FileUtils.readPictureDegree(context, path);
    }

    private static class CheckButton {

        private View rootView;
        private ImageView image;
        private TextView text;

        private boolean checked = false;
        private int nor_resId;
        private int sel_resId;

        public CheckButton(View root, @DrawableRes int norId, @DrawableRes int selId) {
            rootView = root;
            image = (ImageView) root.findViewById(R.id.image);
            text = (TextView) root.findViewById(R.id.text);

            nor_resId = norId;
            sel_resId = selId;
            image.setImageResource(nor_resId);
        }

        public void setChecked(boolean check) {
            checked = check;
            image.setImageResource(checked ? sel_resId : nor_resId);
        }

        public boolean getChecked() {
            return checked;
        }

        public void setText(int resId) {
            text.setText(resId);
        }

        public void setText(CharSequence chars) {
            text.setText(chars);
        }

        public void setOnClickListener(@Nullable View.OnClickListener l) {
            rootView.setOnClickListener(l);
        }

        public void setSelectButtonVisibility(int visibility) {
            image.setVisibility(visibility);
        }
    }

    private class PreviewAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final MediaItem mediaItem = mItemList.get(position);
            View view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.rc_picsel_preview, container, false);
            final SubsamplingScaleImageView subsamplingScaleImageView = view.findViewById(R.id.rc_photoView);
            final ImageView gifview = view.findViewById(R.id.rc_gifview);
            ImageButton playButton = view.findViewById(R.id.rc_play_video);
            container.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            String imagePath;

            if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                imagePath = KitStorageUtils.getImageSavePath(PicturePreviewActivity.this) + File.separator + mediaItem.name;
                if (!new File(imagePath).exists()) {

                    Bitmap videoFrame = null;
                    if (KitStorageUtils.isBuildAndTargetForQ(getApplicationContext())) {
                        try {
                            ParcelFileDescriptor pfd = getApplicationContext().getContentResolver().openFileDescriptor(Uri.parse(mediaItem.uri_sdk29), "r");
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(pfd.getFileDescriptor());
                            videoFrame = retriever.getFrameAtIndex(0);
                        } catch (IOException e) {
                            RLog.e(TAG, "instantiateItem Q is error");
                        }
                    } else {
                        videoFrame = ThumbnailUtils.createVideoThumbnail(mediaItem.uri, MINI_KIND);
                    }

                    if (videoFrame != null) {
                        imagePath = FileUtils.convertBitmap2File(videoFrame,
                                KitStorageUtils.getImageSavePath(PicturePreviewActivity.this), mediaItem.name).toString();
                    } else {
                        imagePath = "";
                    }
                }
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            Uri uri = FileProvider.getUriForFile(v.getContext(), v.getContext().getPackageName()
                                            + v.getContext().getResources().getString(R.string.rc_authorities_fileprovider),
                                    new File(mediaItem.uri));
                            if (KitStorageUtils.isBuildAndTargetForQ(getApplicationContext())) {
                                //Android Q，直接使用MediaStore获得的uri_sdk29即可，不需要通过FileProvider
                                intent.setDataAndType(Uri.parse(mediaItem.uri_sdk29), mediaItem.mimeType);
                            } else {
                                intent.setDataAndType(uri, mediaItem.mimeType);
                            }

                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            intent.setDataAndType(Uri.parse("file://" + mediaItem.uri), mediaItem.mimeType);
                        }
                        startActivity(intent);
                    }
                });
                playButton.setVisibility(View.VISIBLE);
                subsamplingScaleImageView.setImage(ImageSource.uri(imagePath));
                gifview.setVisibility(View.GONE);
                subsamplingScaleImageView.setOrientation(readPictureDegree(imagePath, container.getContext()));
            } else {
                playButton.setVisibility(View.GONE);
                if (KitStorageUtils.isBuildAndTargetForQ(getApplicationContext())) {
                    imagePath = mItemList.get(position).uri_sdk29;
                } else {
                    imagePath = mItemList.get(position).uri;
                }
                if (mItemList.get(position).uri.endsWith(".gif")) {
                    gifview.setVisibility(View.VISIBLE);
                    Glide.with(PicturePreviewActivity.this).asGif().load(imagePath).into(gifview);
                } else {
                    gifview.setVisibility(View.GONE);
                    subsamplingScaleImageView.setImage(ImageSource.uri(imagePath));
                    subsamplingScaleImageView.setOrientation(readPictureDegree(imagePath, container.getContext()));
                }
            }

            AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(imagePath);
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(imagePath);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
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
