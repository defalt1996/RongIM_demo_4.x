package io.rong.imkit.plugin.location;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.rong.common.RLog;
import io.rong.imageloader.core.DisplayImageOptions;
import io.rong.imageloader.core.ImageLoader;
import io.rong.imageloader.core.assist.LoadedFrom;
import io.rong.imageloader.core.display.CircleBitmapDisplayer;
import io.rong.imageloader.core.imageaware.ImageViewAware;
import io.rong.imkit.R;
import io.rong.imkit.RongBaseNoActionbarActivity;
import io.rong.imkit.utilities.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.UserInfo;

public class AMapRealTimeActivity extends RongBaseNoActionbarActivity implements
        ILocationChangedListener,
        IUserInfoProvider.UserInfoCallback {

    private static final String TAG = "AMapRealTimeActivity";

    private MapView mAMapView;
    private ViewGroup mTitleBar;
    private TextView mUserText;
    private Handler mHandler;
    private AMap mAMap;
    private Map<String, UserTarget> mUserTargetMap;
    private ArrayList<String> mParticipants;
    private boolean mHasAnimate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_location_real_time_activity);
        mHandler = new Handler();
        mUserTargetMap = new HashMap<>();
        mAMapView = findViewById(R.id.rc_ext_amap);
        mAMapView.onCreate(savedInstanceState);
        View exitView = findViewById(R.id.rc_toolbar_close);
        exitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PromptPopupDialog dialog = PromptPopupDialog.newInstance(v.getContext(), "",
                        getString(R.string.rc_ext_exit_location_sharing),
                        getString(R.string.rc_ext_exit_location_sharing_confirm));
                dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                    @Override
                    public void onPositiveButtonClicked() {
                        LocationManager.getInstance().quitLocationSharing();
                        finish();
                    }
                });
                dialog.show();
            }
        });
        View closeView = findViewById(R.id.rc_toolbar_hide);
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTitleBar = findViewById(R.id.rc_user_icons);
        mUserText = findViewById(R.id.rc_user_text);

        mParticipants = getIntent().getStringArrayListExtra("participants");
        if (mParticipants == null) {
            mParticipants = new ArrayList<>();
            mParticipants.add(RongIMClient.getInstance().getCurrentUserId());
        }

        initMap();
        LocationManager.getInstance().setLocationChangedListener(this);
        LocationManager.getInstance().setMyLocationChangedListener(new IMyLocationChangedListener() {
            @Override
            public void onMyLocationChanged(AMapLocationInfo locInfo) {
                updateParticipantMarker(locInfo.getLat(), locInfo.getLng(), RongIMClient.getInstance().getCurrentUserId());
            }
        });
        LocationManager.getInstance().updateMyLocationInLoop(5);
    }

    private void initMap() {
        mAMap = mAMapView.getMap();
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);

        for (String userId : mParticipants) {
            UserTarget userTarget = createUserTargetById(userId);
            mUserTargetMap.put(userId, userTarget);
            LocationManager.getInstance().getUserInfo(userId, this);
            updateParticipantTitleText();
        }
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(17f);
        mAMap.animateCamera(zoom, null);
    }

    @Override
    public void onGotUserInfo(UserInfo userInfo) {
        String userId = userInfo.getUserId();
        UserTarget userTarget = mUserTargetMap.get(userId);
        if (userTarget != null) {
            setAvatar(userTarget.getTargetView(), userInfo.getPortraitUri() != null ? userInfo.getPortraitUri().toString() : null);

            View iconView = LayoutInflater.from(AMapRealTimeActivity.this).inflate(R.layout.rc_icon_rt_location_marker, null);
            ImageView imageView = iconView.findViewById(android.R.id.icon);
            ImageView locImageView = iconView.findViewById(android.R.id.icon1);
            setAvatar(imageView, userInfo.getPortraitUri() != null ? userInfo.getPortraitUri().toString() : null);
            if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                locImageView.setImageResource(R.drawable.rc_rt_loc_myself);
            } else {
                locImageView.setImageResource(R.drawable.rc_rt_loc_other);
            }
            userTarget.getTargetMarker().setIcon(BitmapDescriptorFactory.fromView(iconView));
        }
    }

    @Override
    public void onLocationChanged(double latitude, double longitude, String userId) {
        updateParticipantMarker(latitude, longitude, userId);
    }

    @Override
    public void onParticipantJoinSharing(String userId) {
        if (mUserTargetMap.get(userId) != null) {
            return;
        }
        if (!mParticipants.contains(userId)) {
            mParticipants.add(userId);
        }
        UserTarget userTarget = createUserTargetById(userId);
        mUserTargetMap.put(userId, userTarget);
        LocationManager.getInstance().getUserInfo(userId, this);
        updateParticipantTitleText();
    }

    @Override
    public void onParticipantQuitSharing(String userId) {
        UserTarget userTarget = mUserTargetMap.get(userId);
        mParticipants.remove(userId);
        if (userTarget != null) {
            mUserTargetMap.remove(userId);
            removeParticipantTitleIcon(userTarget);
            updateParticipantTitleText();
            removeParticipantMarker(userTarget);
        }
    }

    @Override
    public void onError(RealTimeLocationConstant.RealTimeLocationErrorCode code) {
        Toast.makeText(this, R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
        LocationManager.getInstance().quitLocationSharing();
        finish();
    }

    @Override
    public void onSharingTerminated() {

    }

    private DisplayImageOptions createDisplayImageOptions(int defaultResId) {
        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        if (defaultResId != 0) {
            Drawable defaultDrawable = getResources().getDrawable(defaultResId);
            builder.showImageOnLoading(defaultDrawable);
            builder.showImageForEmptyUri(defaultDrawable);
            builder.showImageOnFail(defaultDrawable);
        }
        builder.displayer(new CircleBitmapDisplayer());
        return builder.resetViewBeforeLoading(false)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    private void setAvatar(ImageView imageView, String url) {
        DisplayImageOptions options = createDisplayImageOptions(url == null ? R.drawable.rc_ext_realtime_default_avatar : 0);
        ImageViewAware imageViewAware = new ImageViewAware(imageView);
        if (url == null) {
            Drawable drawable = options.getImageForEmptyUri(null);
            try {
                Bitmap bitmap = drawableToBitmap(drawable);
                options.getDisplayer().display(bitmap, imageViewAware, LoadedFrom.DISC_CACHE);
            } catch (Exception e) {
                RLog.e(TAG, "setAvatar", e);
            }
        } else {
            File file = ImageLoader.getInstance().getDiskCache().get(url);
            if (file != null && file.exists()) {
                try {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inSampleSize = 1;
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
                    options.getDisplayer().display(bitmap, imageViewAware, LoadedFrom.DISC_CACHE);
                } catch (Exception e) {
                    RLog.e(TAG, "setAvatar", e);
                }
            } else {
                if (!TextUtils.isEmpty(url)) {
                    ImageLoader.getInstance().displayImage(url, imageViewAware, options, null, null);
                } else {
                    RLog.e(TAG, "SetAvatar, url is empty");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        RLog.d(TAG, "onDestroy()");
        mAMapView.onDestroy();
        LocationManager.getInstance().setLocationChangedListener(null);
        LocationManager.getInstance().setMyLocationChangedListener(null);
        super.onDestroy();
    }

    private UserTarget createUserTargetById(final String userId) {
        if (mUserTargetMap.get(userId) != null) {
            return mUserTargetMap.get(userId);
        }
        UserTarget userTarget = new UserTarget();

        // set target icon view.
        userTarget.setTargetView(new ImageView(this));
        userTarget.getTargetView().setTag(userId);
        userTarget.getTargetView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = (String) v.getTag();
                UserTarget user = mUserTargetMap.get(userId);
                LatLng latLng = null;
                if (user != null) {
                    latLng = user.getTargetMarker().getPosition();
                }
                if (latLng != null) {
                    CameraUpdate update = CameraUpdateFactory.changeLatLng(latLng);
                    mAMap.animateCamera(update, null);
                }
            }
        });
        float scale = Resources.getSystem().getDisplayMetrics().density;
        int hw = (int) (40 * scale + 0.5f);
        int pd = (int) (2 * scale + 0.5f);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(hw, hw);
        userTarget.getTargetView().setLayoutParams(lp);
        userTarget.getTargetView().setPadding(pd, pd, pd, pd);
        setAvatar(userTarget.getTargetView(), null);
        mTitleBar.addView(userTarget.getTargetView());

        // set target aMap marker.
        View iconView = LayoutInflater.from(AMapRealTimeActivity.this).inflate(R.layout.rc_icon_rt_location_marker, null);
        ImageView locImageView = iconView.findViewById(android.R.id.icon1);

        ImageView avatar = iconView.findViewById(android.R.id.icon);
        setAvatar(avatar, null);
        if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
            locImageView.setImageResource(R.drawable.rc_rt_loc_myself);
        } else {
            locImageView.setImageResource(R.drawable.rc_rt_loc_other);
        }
        MarkerOptions markerOptions = new MarkerOptions().anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromView(iconView));
        userTarget.setTargetMarker(mAMap.addMarker(markerOptions));
        return userTarget;
    }

    private void removeParticipantTitleIcon(final UserTarget userTarget) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitleBar.removeView(userTarget.getTargetView());
            }
        });
    }

    private void updateParticipantTitleText() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mUserTargetMap.size() == 0) {
                    RLog.e(TAG, "mUserTargetMap size is 0 ");
                } else {
                    mUserText.setText(String.format(getResources().getString(R.string.rc_others_are_sharing_location2), mUserTargetMap.size()));
                }
            }
        });
    }

    private void updateParticipantMarker(final double latitude, final double longitude, final String userId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                UserTarget target = mUserTargetMap.get(userId);
                if (target == null) {
                    target = createUserTargetById(userId);
                    mUserTargetMap.put(userId, target);
                    LocationManager.getInstance().getUserInfo(userId, AMapRealTimeActivity.this);
                    if (!mParticipants.contains(userId)) {
                        mParticipants.add(userId);
                    }
                }

                target.getTargetMarker().setPosition(new LatLng(latitude, longitude));
                updateParticipantTitleText();
                if (userId.equals(RongIMClient.getInstance().getCurrentUserId())
                        && !mHasAnimate
                        && latitude != 0
                        && longitude != 0) {
                    CameraUpdate update = CameraUpdateFactory.changeLatLng(new LatLng(latitude, longitude));
                    mAMap.animateCamera(update, null);
                    mHasAnimate = true;
                }
            }
        });
    }

    private void removeParticipantMarker(final UserTarget userTarget) {
        userTarget.getTargetMarker().remove();
    }

    private class UserTarget {
        private ImageView targetView;
        private Marker targetMarker;

        public ImageView getTargetView() {
            return targetView;
        }

        public void setTargetView(ImageView targetView) {
            this.targetView = targetView;
        }

        public Marker getTargetMarker() {
            return targetMarker;
        }

        public void setTargetMarker(Marker targetMarker) {
            this.targetMarker = targetMarker;
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mAMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mAMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAMapView.onSaveInstanceState(outState);
    }
}
