package com.yingke.mediacodec.recorder.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.yingke.mediacodec.player.PlayerLog;

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
public class MediaAudioEncoder extends MediaEncoder {


    private static final String TAG = MediaAudioEncoder.class.getSimpleName();
    private static final String MIME_TYPE = "audio/mp4a-latm";
    // 44.1[KHz] 采样率
    private static final int SAMPLE_RATE = 44100;
    // 码率
    private static final int BIT_RATE = 64000;
    // AAC, 每帧采样数
    public static final int SAMPLES_PER_FRAME = 1024;
    // AAC, 每个buffer的帧数
    public static final int FRAMES_PER_BUFFER = 25;

    // 音频录制线程
    private AudioThread mAudioThread = null;

    // 音频源
    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    public MediaAudioEncoder(MediaMuxerManager mediaMuxerManager, MediaEncoderListener mediaEncoderListener) {
        super(TAG, mediaMuxerManager, mediaEncoderListener);
    }

    @Override
    public void prepare() throws IOException {
        mTrackIndex = -1;
        mMuxerStarted = mIsEndOfStream = false;

        // mediaFormat配置
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        // 单声道
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        // 比特率
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        // 声道数
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        // 音频编码器
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        if (mMediaEncoderListener != null) {
            try {
                mMediaEncoderListener.onPrepared(this);
            } catch (final Exception e) {
                PlayerLog.e(TAG, TAG + "prepare:" +  e);
            }
        }
    }

    @Override
    public void startRecording() {
        super.startRecording();
        // 使用内部麦克风创建和执行音频捕获线程
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            // 开始录音线程
            mAudioThread.start();
        }
    }

    @Override
    public void release() {
        mAudioThread = null;
        super.release();
    }


    /**
     * 线程：从内部麦克风捕获音频数据作为未压缩的16位PCM数据，并将其写入Mediacodec编码器
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int minBufferSize = AudioRecord.getMinBufferSize(
                        // 采样率
                        SAMPLE_RATE,
                        // 但声道
                        AudioFormat.CHANNEL_IN_MONO,
                        // 编码方式
                        AudioFormat.ENCODING_PCM_16BIT);

                int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (bufferSize < minBufferSize) {
                    bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
                }
                // 录音
                AudioRecord audioRecord = null;
                // 找一个音频源
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source,
                                // 采样率
                                SAMPLE_RATE,
                                // 单声道
                                AudioFormat.CHANNEL_IN_MONO,
                                // 编码方式
                                AudioFormat.ENCODING_PCM_16BIT,
                                // 缓存大小
                                bufferSize);

                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            audioRecord = null;
                        }
                    } catch (final Exception e) {
                        audioRecord = null;
                    }

                    if (audioRecord != null) {
                        break;
                    }
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {

                            // 分配buffer
                            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                            int readBytes;
                            // 开始录音
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing && !mRequestStop && !mIsEndOfStream; ) {
                                    // 从mic 读音频数据
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                    if (readBytes > 0) {
                                        // 设置音频pcm数据到编码器
                                        buf.position(readBytes);
                                        buf.flip();
                                        // 向编码器写入buffer数据
                                        encode(buf, readBytes, getPTSUs());
                                        // 一帧音频可用
                                        frameAvailableSoon();
                                    }
                                }
                                // 一帧音频可用
                                frameAvailableSoon();
                            } finally {
                                // 停止录音
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        // 停止录音
                        audioRecord.release();
                    }
                } else {
                    PlayerLog.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                PlayerLog.e(TAG, "AudioThread#run" +  e);
            }

        }
    }




}
