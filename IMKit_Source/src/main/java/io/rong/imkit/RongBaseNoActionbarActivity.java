package io.rong.imkit;

import android.content.Context;
import androidx.fragment.app.FragmentActivity;

import io.rong.imkit.utilities.LangUtils;

public class RongBaseNoActionbarActivity extends FragmentActivity {


    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LangUtils.getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }
}
