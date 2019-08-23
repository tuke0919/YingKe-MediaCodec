package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.utils.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * 功能：使用MedaiCodec做多个视频拼接
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/20
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaMuxerThread extends Thread implements OnMuxerListener {

    public static final String TAG = "MediaMuxerThread";

    private final Object lock = new Object();

    private String mOutputFilePath;
    private MediaMuxer mMediaMuxer;

    // 混合多个音频线程
    private MixAudioThread mAudioThread;
    // 混合多个视频线程
    private MixVideoThread mVideoThread;
    // 音频轨道
    private int mMuxerAudioTrack;
    // 视频轨道
    private int mMuxerVideoTrack;
    // 音频是否添加混合器
    private volatile boolean mIsAudioAdded = false;
    // 视频是否添加混合器
    private volatile boolean mIsVideoAdded = false;
    // 视频结束
    private boolean mIsVideoEnd = false;
    // 音频结束
    private boolean mIsAudioEnd = false;

    // 混合器是否开始
    private boolean mIsMuxerStarted = false;

    // 输出文件信息
    private List<VideoInfo> mInputVideos;


    /**
     * 设置要合并的视频信息和输出文件path
     * @param inputVideos
     * @param outputFilePath
     */
    public void setVideoFiles(List<VideoInfo> inputVideos, String outputFilePath) {
        mInputVideos = inputVideos;
        mOutputFilePath = outputFilePath;
    }

    @Override
    public void run() {
        super.run();

        if (mListener != null) {
            mListener.onStart();
        }

        initMuxer();
        mAudioThread = new MixAudioThread(mInputVideos, this);
        mVideoThread = new MixVideoThread(mInputVideos, this);
        mAudioThread.start();
        mVideoThread.start();
    }

    /**
     * 初始化混合器
     */
    private void initMuxer() {
        checkUseful();

        try {
            mMediaMuxer = CodecUtil.createMuxer(mOutputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查各项参数是否正确
     */
    private void checkUseful() {

        if (mInputVideos == null || mInputVideos.size() == 0) {
            throw new IllegalStateException(" 必须先设置要处理的视频");
        }

        if (TextUtils.isEmpty(mOutputFilePath)) {
            throw new IllegalStateException(" 必须设置视频输出路径");
        }
    }

    @Override
    public void addTrackFormat(int mediaType, MediaFormat mediaFormat) {
        PlayerLog.e(TAG, "---addTrackFormat---");
        synchronized (lock) {
            if (mMediaMuxer == null) {
                return;
            }
            switch (mediaType) {
                case MediaType.MEDIA_TYPE_AUDIO:
                    PlayerLog.e(TAG, "---addTrackFormat 音频---");

                    mMuxerAudioTrack = mMediaMuxer.addTrack(mediaFormat);
                    mIsAudioAdded = true;
                    break;

                case MediaType.MEDIA_TYPE_VIDEO:
                    PlayerLog.e(TAG, "---addTrackFormat 视频---");

                    mMuxerVideoTrack = mMediaMuxer.addTrack(mediaFormat);
                    mIsVideoAdded = true;
                    break;
            }
            if (mIsAudioAdded && mIsVideoAdded) {
                PlayerLog.e(TAG, "---addTrackFormat 视频--- mIsAudioAdded = " + mIsAudioAdded + " mIsVideoAdded = " + mIsVideoAdded );

                mMediaMuxer.start();
                mIsMuxerStarted = true;
                lock.notify();

                PlayerLog.e(TAG, " addTrackFormat start media muxer waiting for data...");
            }

        }
    }

    @Override
    public void writeSampleData(int mediaType, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        PlayerLog.e(TAG, "---writeSampleData--- mIsMuxerStarted = " + mIsMuxerStarted);

        // 等待 混合器 开始
        if (!mIsMuxerStarted) {
            synchronized (lock) {
                if (!mIsMuxerStarted) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        switch (mediaType) {
            case MediaType.MEDIA_TYPE_AUDIO:
                PlayerLog.e(TAG, "---writeSampleData 音频---");
                mMediaMuxer.writeSampleData(mMuxerAudioTrack, buffer, bufferInfo);
                break;

            case MediaType.MEDIA_TYPE_VIDEO:
                PlayerLog.e(TAG, "---writeSampleData 视频---");
                mMediaMuxer.writeSampleData(mMuxerVideoTrack, buffer, bufferInfo);
                break;
        }
    }

    @Override
    public void writeAudioEnd() {
        PlayerLog.e(TAG, "---writeAudioEnd---");
        mIsAudioEnd = true;
        finish();
    }

    @Override
    public void writeVideoEnd() {
        PlayerLog.e(TAG, "---writeVideoEnd---");
        mIsVideoEnd = true;
        finish();
    }


    /**
     * 视频和音频写入完成
     */
    public void finish(){
        PlayerLog.e(TAG, "---writeVideoEnd--- mIsAudioEnd = " + mIsAudioEnd + " mIsVideoEnd = " + mIsVideoEnd);

        synchronized (lock){
            if(mIsAudioEnd && mIsVideoEnd){
                stopMediaMuxer();
                if (mListener != null) {
                    mListener.onFinish();
                }
            }
        }
    }
    /**
     * 停止MediaMuxer
     */
    private void stopMediaMuxer() {
        PlayerLog.e(TAG, "---stopMediaMuxer---");

        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsAudioEnd = false;
            mIsVideoEnd = false;
            mMediaMuxer = null;
        }
    }

    public ProcessListener mListener;

    public void setListener(ProcessListener listener) {
        mListener = listener;
    }

    public interface ProcessListener {
        void onStart();
        void onFinish();
    }
}
