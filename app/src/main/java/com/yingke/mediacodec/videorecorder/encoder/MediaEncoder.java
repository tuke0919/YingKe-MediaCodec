package com.yingke.mediacodec.videorecorder.encoder;

import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.support.annotation.MainThread;

import com.yingke.mediacodec.videoplayer.PlayerLog;

import java.io.IOException;
import java.nio.ByteBuffer;

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
public abstract class MediaEncoder implements Runnable{

    private static final String TAG = MediaEncoder.class.getSimpleName();
    private String TAG_1 ;

    // 10[msec]
    protected static final int TIMEOUT_USEC = 10000;

    // 同步锁
    protected final Object mSync = new Object();

    // 是否正在进行录制
    protected volatile boolean mIsCapturing;
    // 结束录制的标识
    protected volatile boolean mRequestStop;

    // 可用数据帧数量（可以去muxer）
    private int mRequestDrainEncoderCount;

    // 是否写入EOS帧
    protected boolean mIsEndOfStream;
    // muxer是否正在运行
    protected boolean mMuxerStarted;
    // 轨道数量
    protected int mTrackIndex;

    // 视频，音频编码器
    protected MediaCodec mMediaCodec;
    // 输出buffer信息
    private MediaCodec.BufferInfo mBufferInfo;
    // 复用器
    protected MediaMuxerManager mMuxerManager;
    // 编码器回调
    protected MediaEncoderListener mMediaEncoderListener;


    /**
     * 构造方法
     *
     * @param mediaMuxerManager
     * @param mediaEncoderListener
     */
    public MediaEncoder(String tag,
                        final MediaMuxerManager mediaMuxerManager,
                        final MediaEncoderListener mediaEncoderListener) {
        TAG_1 = tag;
        PlayerLog.d(TAG, TAG_1 + "---MediaEncoder construtor ---");

        if (mediaEncoderListener == null) {
            throw new NullPointerException(TAG_1 +"MediaEncoderListener is null");
        }
        if (mediaMuxerManager == null) {
            throw new NullPointerException(TAG_1 +"MediaMuxerManager is null");
        }
        this.mMuxerManager = mediaMuxerManager;
        this.mMediaEncoderListener = mediaEncoderListener;
        // 添加解码器
        this.mMuxerManager.addEncoder(MediaEncoder.this);

        PlayerLog.d(TAG ,TAG_1 +"---MediaEncoder synchronized (mSync) before begin---");
        synchronized (mSync) {
            PlayerLog.d(TAG, TAG_1 +"---MediaEncoder synchronized (mSync) begin---");

            // 创建bufferInfo
            mBufferInfo = new MediaCodec.BufferInfo();
            // 开启 解码器线程
            new Thread(this, getClass().getSimpleName()).start();

            // 本解码器线程等待
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        PlayerLog.d(TAG, TAG_1 +"---MediaEncoder synchronized (mSync) end---");
    }


    /**
     * 目前主线程调用
     */
    @MainThread
    public void startRecording() {

        PlayerLog.d(TAG,TAG_1 +"---startRecording synchronized (mSync) before begin---");
        synchronized (mSync) {
            PlayerLog.d(TAG,TAG_1 +"---startRecording synchronized (mSync) begin---");
            // 正在录制标识
            mIsCapturing = true;
            // 停止标识 置false
            mRequestStop = false;
            //
            mSync.notifyAll();
        }
        PlayerLog.d(TAG,TAG_1 +"---startRecording synchronized (mSync) end---");

    }


    /**
     * 停止录制(目前在主线程调用)
     */
    @MainThread
    public void stopRecording() {
        PlayerLog.d(TAG,TAG_1 + "---stopRecording synchronized (mSync) before begin---");
        synchronized (mSync) {
            PlayerLog.d(TAG,TAG_1 + "---stopRecording synchronized (mSync) begin---");
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;
            mSync.notifyAll();
        }
        PlayerLog.d(TAG,TAG_1 + "---stopRecording synchronized (mSync) end---");
    }

    /**
     * 目前在主线程被调用，子类实现
     *
     * @throws IOException
     */
    @MainThread
    public abstract void prepare() throws IOException;


    /**
     * 表明帧数据 已经可用
     *
     * @return true 如果编码器可以编码
     */
    public boolean frameAvailableSoon() {
        PlayerLog.d(TAG, TAG_1 + "---frameAvailableSoon---");

        PlayerLog.d(TAG, TAG_1 + "---mSync before begin---");
        synchronized (mSync) {
            PlayerLog.d(TAG, TAG_1 + "---mSync begin---");

            if (!mIsCapturing || mRequestStop) {
                PlayerLog.d(TAG, TAG_1 + "mIsCapturing: " + mIsCapturing);
                PlayerLog.d(TAG, TAG_1 + "mRequestStop: " + mRequestStop);
                PlayerLog.d(TAG, TAG_1 + "return false");
                return false;
            }
            mRequestDrainEncoderCount++;
            PlayerLog.d(TAG, TAG_1 + "mRequestDrainEncoderCount: "+mRequestDrainEncoderCount);
            mSync.notifyAll();
        }
        PlayerLog.d(TAG, TAG_1 + "---mSync end---");
        PlayerLog.d(TAG, TAG_1 + "return true");
        return true;
    }

    @Override
    public void run() {
        PlayerLog.d(TAG,TAG_1 + "---run---");

        PlayerLog.d(TAG,TAG_1 + "---run synchronized (mSync) before begin---");
        // 线程开启
        synchronized (mSync) {
            PlayerLog.d(TAG,TAG_1 + "---run synchronized (mSync) begin---");

            mRequestStop = false;
            mRequestDrainEncoderCount = 0;

            // 唤醒等待的线程
            mSync.notify();
        }
        PlayerLog.d(TAG,TAG_1 + "---run synchronized (mSync) end---");

        // 线程开启
        final boolean isRunning = true;
        // 接受的停止请求
        boolean localRequestStop;
        // 可以muxer编码器输出数据
        boolean localRequestDrainEncoderFlag;

        // 死循环
        while (isRunning) {

            // 检查循环条件 是否成立
            PlayerLog.d(TAG,TAG_1 + "---run2 synchronized (mSync) before begin---");
            synchronized (mSync) {
                PlayerLog.d(TAG,TAG_1 + "---run2 synchronized (mSync) begin---");

                localRequestStop = mRequestStop;
                localRequestDrainEncoderFlag = (mRequestDrainEncoderCount > 0);
                if (localRequestDrainEncoderFlag) {
                    mRequestDrainEncoderCount--;
                }
            }
            PlayerLog.d(TAG,TAG_1 + "---run2 synchronized (mSync) end---");

            // 停止录制时，调用
            if (localRequestStop) {

                // 编码器输出数据，写入Muxer
                drainEncoder();
                // 写EOS帧
                signalEndOfInputStream();
                // 对EOS帧 处理输出数据
                drainEncoder();
                // 释放所有对象
                release();

                break;
            }

            // 需要Muxer
            if (localRequestDrainEncoderFlag) {
                drainEncoder();

            } else {

                // ------不需要录制时，线程进入等待状态---------
                PlayerLog.d(TAG,TAG_1 + "---run3 synchronized (mSync) before begin---");
                synchronized (mSync) {
                    PlayerLog.d(TAG,TAG_1 + "---run3 synchronized (mSync) begin---");
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                PlayerLog.d(TAG,TAG_1 + "---run3 synchronized (mSync) end---");
            }
        }

        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }


    /**
     * mEncoder从缓冲区取数据，然后交给mMuxer复用
     */
    protected void drainEncoder() {
        if (mMediaCodec == null || mMuxerManager == null) {
            return;
        }
        int count = 0;

        // 拿到输出缓冲区,用于取到编码后的数据
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

        LOOP:
        while (mIsCapturing) {
            // 拿到输出缓冲区的索引
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                PlayerLog.d(TAG,TAG_1 + "---drainEncoder encoderStatus == INFO_TRY_AGAIN_LATER---");

                // 还没有可用的输出
                if (!mIsEndOfStream) {
                    // 大于5次没有可用的输出，就退出
                    if (++count > 5) {
                        // 结束循环
                        break LOOP;
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                PlayerLog.d(TAG,TAG_1 + "---drainEncoder encoderStatus == INFO_OUTPUT_BUFFERS_CHANGED---");

                // this shoud not come when encoding
                // 拿到输出缓冲区,用于取到编码后的数据
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                PlayerLog.d(TAG,TAG_1 + "---drainEncoder encoderStatus == INFO_OUTPUT_FORMAT_CHANGED---");
                // 输出帧格式改变，应该在接受buffer之前返回，只会发生一次

                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                // 得到解码器的输出格式，传给复用器muxer
                final MediaFormat format = mMediaCodec.getOutputFormat();
                // muxer添加轨道
                mTrackIndex = mMuxerManager.addTrack(format);
                //
                mMuxerStarted = true;
                //
                if (!mMuxerManager.start()) {
                    // 循环等待muxer开始
                    synchronized (mMuxerManager) {
                        while (!mMuxerManager.isStarted())
                            try {
                                mMuxerManager.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status

            } else {
                PlayerLog.d(TAG,TAG_1 + "---drainEncoder encoderStatus == 编码器输出位置" + encoderStatus);

                // 获取解码后的数据
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    count = 0;
                    if (!mMuxerStarted) {
                        throw new RuntimeException(TAG_1 + "drain:muxer hasn't started");
                    }
                    // 写编码数据到muxer，显示时间需要调整
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    // 写muxer
                    mMuxerManager.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    // 上一个bufferInfo显示时间
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // 释放buffer给编码器
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                // 视频流结束帧
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    mIsCapturing = false;
                    break;
                }
            }
        }
    }

    /**
     * 向编码器写入 视频流结束帧
     */
    public void signalEndOfInputStream() {
        encode(null, 0, getPTSUs());
    }

    /**
     * 向编码器写入buffer数据，开始H264编码
     * 此处主要是Audio的pcm编码，video通过surface传给编码器
     *
     * @param buffer
     * @param length             buffer长度，eos帧是0
     * @param presentationTimeUs buffer显示时间
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) {
            return;
        }
        // 编码器输入buffers
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

        // 循环写入
        while (mIsCapturing) {
            // 获取一个输出Buffer
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                // 放入数据
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }

                if (length <= 0) {
                    // 向编码器 写入EOS帧
                    mIsEndOfStream = true;
                    mMediaCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    // 将buffer传递给编码器
                    mMediaCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            length,
                            presentationTimeUs,
                            0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

            }
        }
    }


    /**
     * // 释放所有对象
     */
    public void release() {

        // 回调停止
        try {
            if (mMediaEncoderListener != null) {
                mMediaEncoderListener.onStopped(MediaEncoder.this);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // 设置标识 停止录制
        mIsCapturing = false;

        // ------释放mediacodec--------
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        // ----------释放muxer-----------
        if (mMuxerStarted) {
            if (mMuxerManager != null) {
                try {
                    mMuxerManager.stop();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // mBufferInfo置空
        mBufferInfo = null;
    }


    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }


    /**
     * 编码器回调
     */
    public interface MediaEncoderListener {
        /**
         * @param encoder
         */
        void onPrepared(MediaEncoder encoder);

        /**
         * @param encoder
         */
        void onStopped(MediaEncoder encoder);
    }



}
