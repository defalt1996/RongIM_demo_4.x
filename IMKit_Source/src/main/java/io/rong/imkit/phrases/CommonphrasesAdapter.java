package io.rong.imkit.phrases;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;

/**
 * 聊天页面 底部栏 快捷回复模块
 */
public class CommonphrasesAdapter {
    private final static String TAG = CommonphrasesAdapter.class.getSimpleName();

    private ViewGroup mPhrasesPager;
    private List<String> phrasesList;
    private boolean mInitialized;
    private IPhrasesClickListener mPhrasesClickListener;

    public CommonphrasesAdapter() {
        phrasesList = new ArrayList<>();
    }

    public void setOnPhrasesClickListener(IPhrasesClickListener clickListener) {
        mPhrasesClickListener = clickListener;
    }

    public void addPhrases(List<String> phrases) {
        phrasesList.addAll(phrases);
    }


    public void bindView(ViewGroup viewGroup) {
        mInitialized = true;
        initView(viewGroup.getContext(), viewGroup);
    }

    private void initView(Context context, ViewGroup viewGroup) {
        LayoutInflater mLayoutInflater = LayoutInflater.from(context);
        mPhrasesPager = (ViewGroup) mLayoutInflater.inflate(R.layout.rc_ext_phrases_pager, null);
        int height = (int) context.getResources().getDimension(R.dimen.rc_extension_board_height);
        mPhrasesPager.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        viewGroup.addView(mPhrasesPager);
        ListView mListView = mPhrasesPager.findViewById(R.id.rc_list);
        PhrasesAdapter mAdapter = new PhrasesAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPhrasesClickListener.onClick(phrasesList.get(position), position);
            }
        });
    }

    public void setVisibility(int visibility) {
        if (mPhrasesPager != null) {
            mPhrasesPager.setVisibility(visibility);
        }
    }

    public int getVisibility() {
        return mPhrasesPager != null ? mPhrasesPager.getVisibility() : View.GONE;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private class PhrasesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return phrasesList.size();
        }

        @Override
        public Object getItem(int position) {
            return phrasesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_ext_phrases_list_item, null);
            }
            TextView tvPhrases = convertView.findViewById(R.id.rc_phrases_tv);
            tvPhrases.setText(phrasesList.get(position));
            return convertView;
        }
    }
}
