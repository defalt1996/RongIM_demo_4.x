package io.rong.imkit.widget;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;

import io.rong.common.RLog;

/**
 * Created by zhjchen on 4/20/15.
 */

public class LoadingDialogFragment extends BaseDialogFragment {

    private static final String TAG = "LoadingDialogFragment";
    private static final String ARGS_TITLE = "args_title";
    private static final String ARGS_MESSAGE = "args_message";


    public static LoadingDialogFragment newInstance(String title, String message) {

        LoadingDialogFragment dialogFragment = new LoadingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARGS_TITLE, title);
        args.putString(ARGS_MESSAGE, message);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ProgressDialog dialog = new ProgressDialog(getActivity());
        String title = getArguments() != null ? getArguments().getString(ARGS_TITLE) : "";
        String message = getArguments() != null ? getArguments().getString(ARGS_MESSAGE) : "";

        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        if (!TextUtils.isEmpty(title))
            dialog.setTitle(title);

        if (!TextUtils.isEmpty(message))
            dialog.setMessage(message);

        return dialog;
    }

    public void show(FragmentManager manager) {
        try {
            //在每个add事务前增加一个remove事务，防止连续的add
            manager.beginTransaction().remove(this).commit();
            super.show(manager, "LoadingDialogFragment");
        } catch (Exception e) {
            //同一实例使用不同的tag会异常,这里捕获一下
            RLog.e(TAG, "show", e);
        }
    }
}
