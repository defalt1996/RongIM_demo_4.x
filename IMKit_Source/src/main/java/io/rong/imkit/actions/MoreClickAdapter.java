package io.rong.imkit.actions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.imkit.R;

/**
 * Created by zwfang on 2018/3/29.
 */

public class MoreClickAdapter implements IMoreClickAdapter {

    private MoreActionLayout moreActionLayout;

    @Override
    public void bindView(ViewGroup viewGroup, Fragment fragment, List<IClickActions> actions) {
        if (moreActionLayout == null) {
            Context context = viewGroup.getContext();
            moreActionLayout = (MoreActionLayout) LayoutInflater.from(context).inflate(R.layout.rc_ext_actions_container, null);
            moreActionLayout.setFragment(fragment);
            moreActionLayout.addActions(actions);
            int height = context.getResources().getDimensionPixelOffset(R.dimen.rc_ext_more_layout_height);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            moreActionLayout.setLayoutParams(params);
            viewGroup.addView(moreActionLayout);
        }
        moreActionLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideMoreActionLayout() {
        if (moreActionLayout != null) {
            moreActionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void setMoreActionEnable(boolean enable) {
        if (moreActionLayout != null) {
            moreActionLayout.refreshView(enable);
        }
    }

    @Override
    public boolean isMoreActionShown() {
        return moreActionLayout != null && moreActionLayout.getVisibility() == View.VISIBLE;
    }
}
