package io.rong.imkit.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.LoadingDialogFragment;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;

/**
 * Created by zhjchen on 4/18/15.
 */

public class PublicServiceProfileFragment extends DispatchResultFragment {

    public static final String AGS_PUBLIC_ACCOUNT_INFO = "arg_public_account_info";
    PublicServiceProfile mPublicAccountInfo;

    private AsyncImageView mPortraitIV;
    private TextView mNameTV;
    private TextView mAccountTV;
    private TextView mDescriptionTV;
    private Button mEnterBtn;
    private Button mFollowBtn;
    private Button mUnfollowBtn;

    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private String name;
    private LoadingDialogFragment mLoadingDialogFragment;

    @Override
    protected void initFragment(Uri uri) {

        if (getActivity() != null && getActivity().getIntent() != null) {
            mPublicAccountInfo = getActivity().getIntent().getParcelableExtra(AGS_PUBLIC_ACCOUNT_INFO);
        }

        if (uri != null) {
            if (mPublicAccountInfo == null) {
                String pathSeg = uri.getLastPathSegment();
                String typeStr = !TextUtils.isEmpty(pathSeg) ? pathSeg.toUpperCase(Locale.US) : "";
                mConversationType = Conversation.ConversationType.valueOf(typeStr);
                mTargetId = uri.getQueryParameter("targetId");
                name = uri.getQueryParameter("name");
            } else {
                mConversationType = mPublicAccountInfo.getConversationType();
                mTargetId = mPublicAccountInfo.getTargetId();
                name = mPublicAccountInfo.getName();
            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_public_service_inf, container, false);

        mPortraitIV = view.findViewById(R.id.portrait);
        mNameTV = view.findViewById(R.id.name);
        mAccountTV = view.findViewById(R.id.account);
        mDescriptionTV = view.findViewById(R.id.description);
        mEnterBtn = view.findViewById(R.id.enter);
        mFollowBtn = view.findViewById(R.id.follow);
        mUnfollowBtn = view.findViewById(R.id.unfollow);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingDialogFragment = LoadingDialogFragment.newInstance("", getResources().getString(R.string.rc_notice_data_is_loading));

        if (mPublicAccountInfo != null) {
            initData(mPublicAccountInfo);
        } else {
            if (!TextUtils.isEmpty(mTargetId)) {
                Conversation.PublicServiceType publicServiceType = null;
                if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                else
                    System.err.print("the public service type is error!!");

                RongIM.getInstance().getPublicServiceProfile(publicServiceType, mTargetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
                    @Override
                    public void onSuccess(PublicServiceProfile info) {
                        if (info != null) {
                            initData(info);
                            RongUserInfoManager.getInstance().setPublicServiceProfile(info);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        RLog.e("PublicServiceProfileFragment", "Failure to get data!!!");
                    }
                });
            }
        }


    }


    private void initData(final PublicServiceProfile info) {

        if (info != null) {

            mPortraitIV.setResource(info.getPortraitUri());
            mNameTV.setText(info.getName());
            mAccountTV.setText(String.format(getResources().getString(R.string.rc_pub_service_info_account), info.getTargetId()));
            mDescriptionTV.setText(info.getIntroduction());

            boolean isFollow = info.isFollow();
            boolean isGlobal = info.isGlobal();

            if (isGlobal) {
                if (getFragmentManager() != null) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.add(R.id.rc_layout, SetConversationNotificationFragment.newInstance());
                    ft.commitAllowingStateLoss();
                }

                mFollowBtn.setVisibility(View.GONE);
                mEnterBtn.setVisibility(View.VISIBLE);
                mUnfollowBtn.setVisibility(View.GONE);
            } else {

                if (isFollow) {
                    if (getFragmentManager() != null) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.add(R.id.rc_layout, SetConversationNotificationFragment.newInstance());
                        ft.commitAllowingStateLoss();
                    }

                    mFollowBtn.setVisibility(View.GONE);
                    mEnterBtn.setVisibility(View.VISIBLE);
                    mUnfollowBtn.setVisibility(View.VISIBLE);
                } else {
                    mFollowBtn.setVisibility(View.VISIBLE);
                    mEnterBtn.setVisibility(View.GONE);
                    mUnfollowBtn.setVisibility(View.GONE);
                }
            }


            mEnterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                    if (listener != null && listener.onEnterConversationClick(v.getContext(), info)) {
                        return;
                    }
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
                }
            });

            mFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIM.getInstance().subscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mLoadingDialogFragment.dismiss();
                            mFollowBtn.setVisibility(View.GONE);
                            mEnterBtn.setVisibility(View.VISIBLE);
                            mUnfollowBtn.setVisibility(View.VISIBLE);

                            RongUserInfoManager.getInstance().setPublicServiceProfile(info);
                            RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), true));
                            RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                            if (listener != null && listener.onFollowClick(v.getContext(), info)) {
                                return;
                            }
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                            RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            mLoadingDialogFragment.dismiss();
                        }
                    });
                    mLoadingDialogFragment.show(getFragmentManager());
                }
            });

            mUnfollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIM.getInstance().unsubscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mFollowBtn.setVisibility(View.VISIBLE);
                            mEnterBtn.setVisibility(View.GONE);
                            mUnfollowBtn.setVisibility(View.GONE);

                            RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), false));
                            RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                            if (listener != null && listener.onUnFollowClick(v.getContext(), info)) {
                                return;
                            }
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {

                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

}
