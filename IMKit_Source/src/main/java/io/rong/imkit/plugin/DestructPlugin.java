package io.rong.imkit.plugin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import io.rong.imkit.R;
import io.rong.imkit.RongExtension;
import io.rong.imkit.dialog.BurnHintDialog;

public class DestructPlugin implements IPluginModule {

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ext_plugin_fire_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_destruct);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension) {
        if (!BurnHintDialog.isFirstClick(currentFragment.getContext())) {
            new BurnHintDialog().show(currentFragment.getFragmentManager());
        }
        extension.enterBurnMode();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

}
