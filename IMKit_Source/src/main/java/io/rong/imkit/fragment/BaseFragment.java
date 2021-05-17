package io.rong.imkit.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.rong.imkit.R;

public abstract class BaseFragment extends Fragment implements Handler.Callback {
    public static final int UI_RESTORE = 1;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    protected <T extends View> T findViewById(View view, int id) {
        return (T) view.findViewById(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected Handler getHandler() {
        return mHandler;
    }

    public abstract boolean onBackPressed();

    public abstract void onRestoreUI();

    private View obtainView(LayoutInflater inflater, int color, Drawable drawable, final CharSequence notice) {
        View view = inflater.inflate(R.layout.rc_wi_notice, null);
        ((TextView) view.findViewById(android.R.id.message)).setText(notice);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(drawable);
        if (color > 0)
            view.setBackgroundColor(color);

        return view;
    }

    private View obtainView(LayoutInflater inflater, int color, int res, final CharSequence notice) {
        View view = inflater.inflate(R.layout.rc_wi_notice, null);
        ((TextView) view.findViewById(android.R.id.message)).setText(notice);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageResource(res);

        view.setBackgroundColor(color);
        return view;
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {

        if (msg.what == UI_RESTORE) {
            onRestoreUI();
        }
        return true;
    }
}
