package com.yingke.mediacodec.recorder.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/11
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaMuxerManager {

    private static final String TAG = MediaMuxerManager.class.getSimpleName();

    private static final String DIR_NAME = "MediaCodec";
    private static final String DIR_NAME_1 = "MediaCodecRecord";

    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    // 输出文件路径
    private String mOutputPath;
    // 复用器
    private MediaMuxer mMediaMuxer;
    // 编码器数量
    private int mEncoderCount;
    // 开始的数量 ？？？
    private int mStartedCount;
    // 是否开始
    private boolean mIsStarted;
    // 视频编码器
    private MediaEncoder mVideoEncoder;
    // 音频编码器
    private MediaEncoder mAudioEncoder;


    public MediaMuxerManager() {

    }

    public MediaMuxerManager(String suffix) throws IOException {
        if (TextUtils.isEmpty(suffix)) {
            suffix = ".mp4";
        }
        try {
            // 输出文件路径
            mOutputPath = getCaptureFile(suffix).toString();
            //
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        // 复用器
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        // 编码器数量
        mEncoderCount = mStartedCount = 0;
        // 未开始
        mIsStarted = false;

    }

    public String getOutputPath() {
        return mOutputPath;
    }

    /**
     * 准备，主要是初始化编码器
     *
     * @throws IOException
     */
    public void prepare() throws IOException {
        if (mVideoEncoder != null) {
            mVideoEncoder.prepare();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
    }

    /**
     * 开始录制
     */
    public void startRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.startRecording();
        }

        if (mAudioEncoder != null) {
            mAudioEncoder.startRecording();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stopRecording();
            mVideoEncoder = null;
        }

        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
            mAudioEncoder = null;
        }
    }

    /**
     * 添加视频编码器和 音频编码器
     *
     * @param encoder {@link MediaAudioEncoder},{@link MediaVideoEncoder}
     */
    public void addEncoder(final MediaEncoder encoder) {
        // 视频编码器
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;

        }
        // 音频编码器
        else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mAudioEncoder = encoder;
        }

        else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        // 编码器数量
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }



    /**
     * 请求开始录，从编码器
     *
     * @return muxer 准备写入时 返回true
     */
    public synchronized boolean start() {

        // 会被调用多次，等于mEncoderCount 开始
        mStartedCount++;
        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
        }
        return mIsStarted;
    }

    /**
     * 当编码器收到EOS后，请求停止录
     */
    public synchronized void stop() {

        mStartedCount--;
        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
        }
    }

    /**
     * 视频，音频编码器输出添加轨道
     *
     * @param format
     * @return minus value indicate error
     */
    public synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        final int trackIx = mMediaMuxer.addTrack(format);

        return trackIx;
    }

    /**
     * write encoded data to muxer
     * 写入数据
     *
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }

    /**
     * @return
     */
    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    /**
     * 获取输出文件路径
     *
     * @param suffix .mp4 - video，.m4a - audio
     * @return 没有写权限 返回null
     */
    public static final File getCaptureFile(final String suffix) {
        final File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME + "/" + DIR_NAME_1);
        if (!dir.getParentFile().exists()) {
            dir.getParentFile().mkdir();
        }
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, "record" + getDateTimeString() + suffix);
        }
        return null;
    }

    /**
     * 获取当前时间的格式化形式
     * @return
     */
    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

}
