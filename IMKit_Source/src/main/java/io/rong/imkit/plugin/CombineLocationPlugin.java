package io.rong.imkit.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import io.rong.common.LibStorageUtils;
import io.rong.imkit.R;
import io.rong.imkit.RongExtension;

import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imkit.utilities.PermissionCheckUtil;

public class CombineLocationPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_location);
    }

    @Override
    public void onClick(final Fragment currentFragment, final RongExtension extension) {
        String[] permissions;
        //如果是 android 10 需要多加一个 ACCESS_BACKGROUND_LOCATION 后台定位权限
        if (LibStorageUtils.isBuildAndTargetForQ(currentFragment.getContext())) {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};
        }
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            sendOrShareLocation(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(permissions, IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN, this);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    private void sendOrShareLocation(final Fragment currentFragment, final RongExtension extension) {
        String[] items = {
                currentFragment.getString(R.string.rc_plugin_location_message),
                currentFragment.getString(R.string.rc_plugin_location_sharing)
        };

        OptionsPopupDialog.newInstance(currentFragment.getActivity(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                if (which == 0) {
                    Intent intent;
                    if (extension.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                        intent = new Intent(currentFragment.getActivity(), io.rong.imkit.plugin.location.AMapLocationActivity2D.class);
                    } else {
                        intent = new Intent(currentFragment.getActivity(), io.rong.imkit.plugin.location.AMapLocationActivity.class);
                    }
                    extension.startActivityForPluginResult(intent, 1, CombineLocationPlugin.this);
                } else if (which == 1) {
                    int result;
                    if (extension.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                        result = io.rong.imkit.plugin.location.LocationManager2D.getInstance().joinLocationSharing();
                    } else {
                        result = io.rong.imkit.plugin.location.LocationManager.getInstance().joinLocationSharing();
                    }
                    if (result == 0) {
                        Intent intent;
                        if (extension.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                            intent = new Intent(currentFragment.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity2D.class);
                        } else {
                            intent = new Intent(currentFragment.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity.class);
                        }
                        if (currentFragment.getActivity() != null) {
                            currentFragment.getActivity().startActivity(intent);
                        }
                    } else if (result == 1) {
                        Toast.makeText(currentFragment.getActivity(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
                    } else if (result == 2) {
                        Toast.makeText(currentFragment.getActivity(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).show();
    }

    @Override
    public boolean onRequestPermissionResult(Fragment fragment, RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            sendOrShareLocation(fragment, extension);
        } else {
            if (fragment.getActivity() != null) {
                extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(fragment.getActivity(), permissions, grantResults));
            }
        }
        return true;
    }
}
