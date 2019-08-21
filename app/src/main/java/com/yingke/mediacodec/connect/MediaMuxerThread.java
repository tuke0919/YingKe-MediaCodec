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
 * 功能：使用MedaiCodec做视频拼接
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

    private int mMuxerAudioTrack;
    private int mMuxerVideoTrack;

    private volatile boolean mIsAudioAdded = false;
    private volatile boolean mIsVideoAdded = false;

    private boolean mIsVideoEnd = false;
    private boolean mIsAudioEnd = false;

    private boolean mIsMuxerStarted = false;


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
        synchronized (lock) {
            if (mMediaMuxer == null) {
                return;
            }
            switch (mediaType) {
                case MediaType.MEDIA_TYPE_AUDIO:
                    mMuxerAudioTrack = mMediaMuxer.addTrack(mediaFormat);
                    mIsAudioAdded = true;
                    break;

                case MediaType.MEDIA_TYPE_VIDEO:
                    mMuxerVideoTrack = mMediaMuxer.addTrack(mediaFormat);
                    mIsVideoAdded = true;
                    break;
            }
            if (mIsAudioAdded && mIsVideoAdded) {
                mMediaMuxer.start();
                mIsMuxerStarted = true;
                lock.notify();
                PlayerLog.e(TAG, " addTrackFormat start media muxer waiting for data...");
            }

        }
    }

    @Override
    public void writeSampleData(int mediaType, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
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
                mMediaMuxer.writeSampleData(mMuxerAudioTrack, buffer, bufferInfo);
                break;

            case MediaType.MEDIA_TYPE_VIDEO:
                mMediaMuxer.writeSampleData(mMuxerVideoTrack, buffer, bufferInfo);
                break;
        }
    }

    @Override
    public void writeAudioEnd() {
        mIsAudioEnd = true;
        finish();
    }

    @Override
    public void writeVideoEnd() {
        mIsVideoEnd = true;
        finish();
    }


    /**
     * 视频和音频写入完成
     */
    public void finish(){
        synchronized (lock){
            if(mIsAudioEnd && mIsVideoEnd){
                stopMediaMuxer();
            }
        }
    }
    /**
     * 停止MediaMuxer
     */
    private void stopMediaMuxer() {
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsAudioEnd = false;
            mIsVideoEnd = false;
            mMediaMuxer = null;
        }

    }





}
