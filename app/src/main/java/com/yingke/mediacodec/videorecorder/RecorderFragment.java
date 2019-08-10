package com.yingke.mediacodec.videorecorder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.videorecorder.glsurface.CameraGlSurfaceView;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/10
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class RecorderFragment extends Fragment {

    private View mRootView;
    private CameraGlSurfaceView mSurfaceView;
    private ImageView mStartRecordBtn;
    private ImageView mStopRecordBtn;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.frag_video_recorder, null);
            initViews();
        }

        return mRootView;
    }


    private void initViews() {
        mSurfaceView = mRootView.findViewById(R.id.camera_gl_view);
        mStartRecordBtn = mRootView.findViewById(R.id.camera_start_record);
        mStopRecordBtn = mRootView.findViewById(R.id.camera_stop_record);

        mStartRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mStopRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSurfaceView != null) {
            mSurfaceView.onResume();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
