package io.rong.imkit.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.PublicServiceProfileList;

/**
 * Created by zhjchen on 4/19/15.
 */

public class PublicServiceSubscribeListFragment extends DispatchResultFragment {

    private PublicServiceListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rc_fr_public_service_sub_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView mListView = view.findViewById(R.id.rc_list);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PublicServiceProfile info = mAdapter.getItem(position);

                RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                String[] item = new String[1];
                final PublicServiceProfile info = mAdapter.getItem(position);
                if (info.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE) {
                    if (getActivity() == null) {
                        return false;
                    }
                    item[0] = getActivity().getString(R.string.rc_pub_service_info_unfollow);
                    OptionsPopupDialog.newInstance(view.getContext(), item).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
                        @Override
                        public void onOptionsItemClicked(int which) {
                            Conversation.PublicServiceType publicServiceType = null;
                            if (info.getConversationType() == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                                publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                            else if (info.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE)
                                publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                            else
                                System.err.print("the public service type is error!!");

                            RongIMClient.getInstance().unsubscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    mAdapter.remove(position);
                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {

                                }
                            });
                        }
                    }).show();
                }
                return true;
            }
        });

        mAdapter = new PublicServiceListAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        getDBData();

        RongContext.getInstance().getEventBus().register(this);
    }


    private void getDBData() {

        RongIM.getInstance().getPublicServiceList(new RongIMClient.ResultCallback<PublicServiceProfileList>() {
            @Override
            public void onSuccess(PublicServiceProfileList infoList) {
                for (PublicServiceProfile info : infoList.getPublicServiceData()) {
                    RongUserInfoManager.getInstance().setPublicServiceProfile(info);
                }

                mAdapter.clear();
                mAdapter.addCollection(infoList.getPublicServiceData());
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {

            }
        });
    }

    @Override
    protected void initFragment(Uri uri) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    private class PublicServiceListAdapter extends io.rong.imkit.widget.adapter.BaseAdapter<PublicServiceProfile> {

        LayoutInflater mInflater;

        public PublicServiceListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        protected View newView(Context context, int position, ViewGroup group) {
            View view = mInflater.inflate(R.layout.rc_item_public_service_list, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.portrait = view.findViewById(R.id.portrait);
            viewHolder.name = view.findViewById(R.id.name);
            viewHolder.introduction = view.findViewById(R.id.introduction);
            view.setTag(viewHolder);

            return view;
        }

        @Override
        protected void bindView(View v, int position, PublicServiceProfile data) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();

            if (data != null) {
                viewHolder.portrait.setResource(data.getPortraitUri());
                viewHolder.name.setText(data.getName());
                viewHolder.introduction.setText(data.getIntroduction());
            }
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public PublicServiceProfile getItem(int position) {
            return super.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }


        class ViewHolder {
            AsyncImageView portrait;
            TextView name;
            TextView introduction;
        }
    }

    public void onEvent(Event.PublicServiceFollowableEvent event) {
        if (event != null) {
            getDBData();
        }
    }


    @Override
    public void onDestroyView() {
        RongContext.getInstance().getEventBus().unregister(this);
        super.onDestroyView();
    }
}
