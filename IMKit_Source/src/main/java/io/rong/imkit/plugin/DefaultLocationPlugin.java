package io.rong.imkit.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.rong.imkit.R;
import io.rong.imkit.RongExtension;
import io.rong.imkit.utilities.PermissionCheckUtil;

public class DefaultLocationPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_location);
    }

    @Override
    public void onClick(final Fragment currentFragment, final RongExtension extension) {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            startLocationActivity(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(permissions, IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN, this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    private void startLocationActivity(Fragment fragment, RongExtension extension) {
        Intent intent;
        if (extension.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
            intent = new Intent(fragment.getActivity(), io.rong.imkit.plugin.location.AMapLocationActivity2D.class);
        } else {
            intent = new Intent(fragment.getActivity(), io.rong.imkit.plugin.location.AMapLocationActivity.class);
        }
        extension.startActivityForPluginResult(intent, 1, this);
    }

    @Override
    public boolean onRequestPermissionResult(Fragment fragment, RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            startLocationActivity(fragment, extension);
        } else {
            if (fragment.getActivity() != null)
                extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(fragment.getActivity(), permissions, grantResults));
        }
        return true;
    }
}
