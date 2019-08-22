package com.yingke.mediacodec.connect.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yingke.mediacodec.R;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2019 All right reserved </p>
 *
 * @author tuke 时间 2019/8/22
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class VideoConnectFragment extends Fragment {

    public View mRootView;
    public RecyclerView mRecyclerView;

    @Override
    public View onCreateView( LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.activity_media_codec_video_connect, null);
            initViews();
            initDatas();
        }

        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void initViews() {
        mRecyclerView = mRootView.findViewById(R.id.recyclerview);

    }

    public void initDatas() {

    }

    @Override
    public void onViewCreated( View view,  Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
