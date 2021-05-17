package io.rong.imkit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.LangUtils;

public class RongBaseActivity extends Activity {
    private ViewFlipper mContentView;
    protected ViewGroup titleContainer;
    protected ImageView searchButton;
    protected TextView title;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LangUtils.getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rc_base_activity_layout);
        titleContainer = findViewById(R.id.rc_ac_ll_base_title);
        searchButton = findViewById(R.id.rc_search);
        title = findViewById(R.id.rc_action_bar_title);
        View back = findViewById(R.id.rc_action_bar_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mContentView = findViewById(R.id.rc_base_container);
    }

    protected void onCreateActionbar(ActionBar actionBar) {
    }

    @Override
    public void setContentView(int resId) {
        View view = LayoutInflater.from(this).inflate(resId, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        mContentView.addView(view, lp);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(this, PermissionCheckUtil.getNotGrantedPermissionMsg(this, permissions, grantResults));
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public class ActionBar {
        public View setActionBar(int res) {
            titleContainer.removeAllViews();
            titleContainer.setBackgroundColor(Color.TRANSPARENT);
            return LayoutInflater.from(RongBaseActivity.this).inflate(res, titleContainer);
        }
    }
}
