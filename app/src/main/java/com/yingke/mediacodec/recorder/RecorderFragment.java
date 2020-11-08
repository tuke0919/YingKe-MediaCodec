package com.yingke.mediacodec.recorder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.yingke.mediacodec.R;
import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.recorder.encoder.MediaAudioEncoder;
import com.yingke.mediacodec.recorder.encoder.MediaEncoder;
import com.yingke.mediacodec.recorder.encoder.MediaMuxerManager;
import com.yingke.mediacodec.recorder.encoder.MediaVideoEncoder;
import com.yingke.mediacodec.recorder.glsurface.MediaCodecRecordGlSurfaceView;
import com.yingke.mediacodec.utils.ScreenUtils;
import com.yingke.mediacodec.utils.ToastUtil;
import com.yingke.mediacodec.widget.GlideRoundTransform;
import com.yingke.mediacodec.widget.localmedia.LocalMediaLoader;
import com.yingke.mediacodec.widget.localmedia.config.LocalMediaConfig;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;
import com.yingke.mediacodec.widget.localmedia.mvp.ILocalMediaView;
import com.yingke.mediacodec.widget.localmedia.mvp.LocalMediaPresenter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/10
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class RecorderFragment extends Fragment implements ILocalMediaView {

    private static final String TAG = RecorderFragment.class.getSimpleName();

    private View mRootView;
    private MediaCodecRecordGlSurfaceView mSurfaceView;
    private ImageView mStartRecordBtn;
    private ImageView mStopRecordBtn;
    private LinearLayout mTimeLayout;
    private Chronometer mChronometer;
    private ImageView mAlbumImage;

    private MediaMuxerManager mMediaMuxerManager;

    private String outputPath = "";
    // 本地媒体加载器
    private LocalMediaPresenter mMediaPresenter;
    private List<LocalMediaResource> mMediaResources;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.frag_video_recorder, null);
            initViews();
            initDatas();
        }

        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeAllViews();
        }
        return mRootView;
    }


    private void initViews() {
        mSurfaceView = mRootView.findViewById(R.id.camera_gl_view);
        mStartRecordBtn = mRootView.findViewById(R.id.camera_start_record);
        mStopRecordBtn = mRootView.findViewById(R.id.camera_stop_record);
        mTimeLayout = mRootView.findViewById(R.id.record_time_layout);
        mTimeLayout.setVisibility(View.GONE);
        mChronometer = mRootView.findViewById(R.id.record_time);
        mAlbumImage = mRootView.findViewById(R.id.camera_album);


        mStartRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerLog.e(TAG, "StartRecordBtn click");
                outputPath = "";
                startRecording();
                onStartRecorded();
            }
        });

        mStopRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerLog.e(TAG, "StopRecordBtn click");
                // 刷新相册数据库
                outputPath = mMediaMuxerManager.getOutputPath();
                stopRecording();
                onStopRecorded();
            }
        });
    }

    public void initDatas() {
        mMediaResources = new ArrayList<>();
        getMediaPresenter().requestMedias(false);
    }

    /**
     * @return 本地媒体加载器
     */
    public LocalMediaPresenter getMediaPresenter() {
        if (mMediaPresenter == null) {
            LocalMediaConfig.getInstance().reset();
            mMediaPresenter = new LocalMediaPresenter(getActivity(), this);
        }
        return mMediaPresenter;
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

    /**
     * 开始录制，这里放在了主线程运行(实际应该放在异步线程中运行)
     */
    private void startRecording() {

        try {

            // if you record audio only, ".m4a" is also OK.
            mMediaMuxerManager = new MediaMuxerManager(".mp4");
            // 开始视频录制
            new MediaVideoEncoder(mMediaMuxerManager, mMediaEncoderListener, 720, 1280);
            // 开启音频录制
            new MediaAudioEncoder(mMediaMuxerManager, mMediaEncoderListener);
            // 视频，音频 录制初始化
            mMediaMuxerManager.prepare();
            // 视频，音频 开始录制
            mMediaMuxerManager.startRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始录制之后
     */
    public void onStartRecorded() {
        mStartRecordBtn.setVisibility(View.GONE);
        mStopRecordBtn.setVisibility(View.VISIBLE);
        mTimeLayout.setVisibility(View.VISIBLE);
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer c) {
                Log.d(TAG, "Chronometer ticking");
                long elapsedMillis = SystemClock.elapsedRealtime() - c.getBase();
                if(elapsedMillis > 3600000L && elapsedMillis < 36000000L){
                    c.setFormat("0%s");
                }else{
                    c.setFormat("00:%s");
                }
            }
        });
        mChronometer.start();

    }


    /**
     * 开始录制
     */
    private void stopRecording() {
        if (mMediaMuxerManager != null) {
            mMediaMuxerManager.stopRecording();
            mMediaMuxerManager = null;
        }
    }

    /**
     * 停止录制之后
     */
    private void onStopRecorded() {

        mStopRecordBtn.setVisibility(View.GONE);
        mStartRecordBtn.setVisibility(View.VISIBLE);
        mChronometer.stop();
        mTimeLayout.setVisibility(View.GONE);

        mTimeLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(outputPath)) {
                    getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(outputPath))));
                }

                LocalMediaResource newMedia = LocalMediaLoader.getNewVideoResource(outputPath);
                mMediaResources.add(0, newMedia);
                notifyDataSetChanged();
            }
        }, 1000);

    }

    /**
     * @return 录制输出路径
     */
    public String getOutputPath() {
        return outputPath;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecording();
        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

    }

    /**
     * 视频、音频 开始与结束录制的回调
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {

        @Override
        public void onPrepared(final MediaEncoder encoder) {
            // 开始录制视频
            if (encoder instanceof MediaVideoEncoder) {
                mSurfaceView.setVideoEncoder((MediaVideoEncoder) encoder);
            }
        }

        /**
         * @param encoder
         */
        @Override
        public void onStopped(final MediaEncoder encoder) {
            // 结束录制视频
            if (encoder instanceof MediaVideoEncoder) {
                mSurfaceView.setVideoEncoder(null);
            }
        }
    };

    @Override
    public void onLocalMediaLoaded(List<LocalMediaFolder> localMediaFolders) {
        if (localMediaFolders != null || localMediaFolders.size() > 0) {
            mMediaResources.addAll(localMediaFolders.get(0).getFolderMedias());
            notifyDataSetChanged();
        }
    }

    /**
     * 显示第一个图像
     */
    public void notifyDataSetChanged() {
        if (mMediaResources.size() > 0) {
            LocalMediaResource firstMediaResource = mMediaResources.get(0);
            GlideRoundTransform roundedCorners = new GlideRoundTransform(getContext(), ScreenUtils.dip2px(getContext(), 2));
            RequestOptions options = new RequestOptions()
                    .transform(roundedCorners)
                    .placeholder(R.color.color_f6f6f6)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true);

            Glide.with(getContext())
                    .load(firstMediaResource.getMediaPath())
                    .apply(options)
                    .into(mAlbumImage);
        }
    }

    @Override
    public void onLocalMediaErr(String message) {
        ToastUtil.showToastShort(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getMediaPresenter() != null) {
            getMediaPresenter().onDestroy();
        }
    }


}
