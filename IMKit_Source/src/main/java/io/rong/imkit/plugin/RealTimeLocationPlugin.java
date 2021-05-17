package io.rong.imkit.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import io.rong.imkit.R;
import io.rong.imkit.RongExtension;


public class RealTimeLocationPlugin implements IPluginModule {

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_location_sharing);
    }

    @Override
    public void onClick(final Fragment currentFragment, final RongExtension extension) {
        if (currentFragment.getActivity() == null) {
            return;
        }
        int perCoarse = currentFragment.getActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int perFine = currentFragment.getActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        int perNetwork = currentFragment.getActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
        if (perCoarse == PackageManager.PERMISSION_DENIED || perFine == PackageManager.PERMISSION_DENIED || perNetwork == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(currentFragment.getActivity(), currentFragment.getActivity().getResources().getString(R.string.rc_ext_location_permission_failed), Toast.LENGTH_SHORT).show();
            return;
        }
        //int result = LocationManager.getInstance().joinLocationSharing();
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
            currentFragment.getActivity().startActivity(intent);
        } else if (result == 1) {
            Toast.makeText(currentFragment.getActivity(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
        } else if (result == 2) {
            Toast.makeText(currentFragment.getActivity(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
}
