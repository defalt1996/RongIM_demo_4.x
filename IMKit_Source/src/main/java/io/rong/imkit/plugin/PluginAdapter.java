package io.rong.imkit.plugin;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;

public class PluginAdapter {
    private final static String TAG = "PluginAdapter";

    private LinearLayout mIndicator;
    private int currentPage = 0;
    private LayoutInflater mLayoutInflater;
    private ViewGroup mPluginPager;
    private List<IPluginModule> mPluginModules;
    private boolean mInitialized;
    private IPluginClickListener mPluginClickListener;
    private View mCustomPager;
    private ViewPager mViewPager;
    private PluginPagerAdapter mPagerAdapter;

    private int mPluginCountPerPage;

    public PluginAdapter() {
        mPluginModules = new ArrayList<>();
    }

    public void setOnPluginClickListener(IPluginClickListener clickListener) {
        mPluginClickListener = clickListener;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public int getPluginPosition(IPluginModule pluginModule) {
        return mPluginModules.indexOf(pluginModule);
    }

    public IPluginModule getPluginModule(int position) {
        if (position >= 0 && position < mPluginModules.size())
            return mPluginModules.get(position);
        else
            return null;
    }

    public List<IPluginModule> getPluginModules() {
        return mPluginModules;
    }

    public void addPlugins(List<IPluginModule> plugins) {
        for (int i = 0; plugins != null && i < plugins.size(); i++) {
            IPluginModule pluginModule = plugins.get(i);
            // 去除转账 plugin
            if (!"com.jrmf360.rylib.rp.extend.TransferAccountPlugin".equals(pluginModule.getClass().getName())) {
                mPluginModules.add(pluginModule);
            }
        }
    }

    public void addPlugin(IPluginModule pluginModule) {
        mPluginModules.add(pluginModule);
        int count = mPluginModules.size();
        if (mPagerAdapter != null && count > 0 && mIndicator != null) {
            int rem = count % mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }
            int pages = count / mPluginCountPerPage + rem;
            mPagerAdapter.setPages(pages);
            mPagerAdapter.setItems(count);
            mPagerAdapter.notifyDataSetChanged();
            mIndicator.removeAllViews();
            initIndicator(pages, mIndicator);
        }
    }

    public void removePlugin(IPluginModule pluginModule) {
        mPluginModules.remove(pluginModule);
        if (mPagerAdapter != null && mViewPager != null) {
            int count = mPluginModules.size();
            if (count > 0) {
                int rem = count % mPluginCountPerPage;
                if (rem > 0) {
                    rem = 1;
                }
                int pages = count / mPluginCountPerPage + rem;
                mPagerAdapter.setPages(pages);
                mPagerAdapter.setItems(count);
                mPagerAdapter.notifyDataSetChanged();
                removeIndicator(pages, mIndicator);
            }
        }
    }

    public void bindView(ViewGroup viewGroup) {
        mInitialized = true;
        initView(viewGroup.getContext(), viewGroup);
    }

    private void initView(Context context, ViewGroup viewGroup) {
        mLayoutInflater = LayoutInflater.from(context);
        mPluginPager = (ViewGroup) mLayoutInflater.inflate(R.layout.rc_ext_plugin_pager, null);
        int height = (int) context.getResources().getDimension(R.dimen.rc_extension_board_height);
        mPluginPager.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        try {
            mPluginCountPerPage = context.getResources().getInteger(context.getResources().getIdentifier("rc_extension_plugin_count_per_page", "integer", context.getPackageName()));
        } catch (Exception e) {
            mPluginCountPerPage = 8;
        }
        viewGroup.addView(mPluginPager);
        mViewPager = mPluginPager.findViewById(R.id.rc_view_pager);
        mIndicator = mPluginPager.findViewById(R.id.rc_indicator);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onIndicatorChanged(currentPage, position);
                currentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        int pages = 0;
        int count = mPluginModules.size();
        if (count > 0) {
            int rem = count % mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }
            pages = count / mPluginCountPerPage + rem;
        }
        mPagerAdapter = new PluginPagerAdapter(pages, count);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);
        initIndicator(pages, mIndicator);
        onIndicatorChanged(-1, 0);
    }

    public void setVisibility(int visibility) {
        if (mPluginPager != null) {
            mPluginPager.setVisibility(visibility);
            if (mCustomPager != null) mCustomPager.setVisibility(View.GONE);
        }
    }

    public int getVisibility() {
        return mPluginPager != null ? mPluginPager.getVisibility() : View.GONE;
    }

    private class PluginPagerAdapter extends PagerAdapter {
        int pages;
        int items;

        public PluginPagerAdapter(int pages, int items) {
            this.pages = pages;
            this.items = items;
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            GridView gridView = (GridView) mLayoutInflater.inflate(R.layout.rc_ext_plugin_grid_view, null);
            gridView.setAdapter(new PluginItemAdapter(position * mPluginCountPerPage, items));
            container.addView(gridView);
            return gridView;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return pages;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            View layout = (View) object;
            container.removeView(layout);
        }

        public void setPages(int value) {
            this.pages = value;
        }

        public void setItems(int value) {
            this.items = value;
        }
    }

    private class PluginItemAdapter extends BaseAdapter {
        int count;
        int index;

        class ViewHolder {
            ImageView imageView;
            TextView textView;
        }

        public PluginItemAdapter(int index, int count) {
            this.count = Math.min(mPluginCountPerPage, count - index);
            this.index = index;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mLayoutInflater.inflate(R.layout.rc_ext_plugin_item, null);
                holder.imageView = convertView.findViewById(R.id.rc_ext_plugin_icon);
                holder.textView = convertView.findViewById(R.id.rc_ext_plugin_title);
                convertView.setTag(holder);
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IPluginModule plugin = mPluginModules.get(currentPage * mPluginCountPerPage + position);
                    mPluginClickListener.onClick(plugin, currentPage * mPluginCountPerPage + position);
                }
            });
            holder = (ViewHolder) convertView.getTag();
            IPluginModule plugin = mPluginModules.get(position + index);
            holder.imageView.setImageDrawable(plugin.obtainDrawable(context));
            holder.textView.setText(plugin.obtainTitle(context));
            return convertView;
        }
    }

    private void initIndicator(int pages, LinearLayout indicator) {
        for (int i = 0; i < pages; i++) {
            ImageView imageView = (ImageView) mLayoutInflater.inflate(R.layout.rc_ext_indicator, null);
            imageView.setImageResource(R.drawable.rc_ext_indicator);
            indicator.addView(imageView);
            if (pages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            } else {
                indicator.setVisibility(View.VISIBLE);
            }
        }
    }

    private void removeIndicator(int totalPages, LinearLayout indicator) {
        int index = indicator.getChildCount();
        if (index > totalPages && index - 1 >= 0) {
            indicator.removeViewAt(index - 1);
            onIndicatorChanged(index, index - 1);
            if (totalPages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void onIndicatorChanged(int pre, int cur) {
        int count = mIndicator.getChildCount();
        if (count > 0 && pre < count && cur < count) {
            if (pre >= 0) {
                ImageView preView = (ImageView) mIndicator.getChildAt(pre);
                preView.setImageResource(R.drawable.rc_ext_indicator);
            }
            if (cur >= 0) {
                ImageView curView = (ImageView) mIndicator.getChildAt(cur);
                curView.setImageResource(R.drawable.rc_ext_indicator_hover);
            }
        }
    }

    public void addPager(View v) {
        mCustomPager = v;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mPluginPager.addView(v, params);
    }

    public View getPager() {
        return mCustomPager;
    }

    public void removePager(View view) {
        if (mCustomPager != null && mCustomPager == view) {
            mPluginPager.removeView(view);
            mCustomPager = null;
        }
    }
}