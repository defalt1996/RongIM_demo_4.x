package io.rong.imkit.activity;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.view.Window;
import android.view.WindowManager;

import io.rong.imkit.R;
import io.rong.imkit.RongBaseNoActionbarActivity;
import io.rong.imkit.fragment.FileListFragment;

/**
 * Created by tiankui on 16/7/30.
 */
public class FileListActivity extends RongBaseNoActionbarActivity {

    private int fragmentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        setContentView(R.layout.rc_ac_file_list);

        if (getSupportFragmentManager().findFragmentById(R.id.rc_ac_fl_storage_folder_list_fragment) == null) {
            FileListFragment fileListFragment = new FileListFragment();
            showFragment(fileListFragment);
        }

    }

    public void showFragment(Fragment fragment) {
        fragmentCount++;
        getSupportFragmentManager()
        .beginTransaction()
        .addToBackStack(fragmentCount + "")
        .replace(R.id.rc_ac_fl_storage_folder_list_fragment, fragment)
        .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if (--fragmentCount == 0) {
            FragmentManager fm = getSupportFragmentManager();
            for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(i);
                Fragment fragment = fm.findFragmentByTag(entry.getName());
                if (fragment != null) {
                    fragment.onDestroy();
                }
            }
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
