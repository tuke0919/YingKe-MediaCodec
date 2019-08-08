package com.yingke.mediacodec.videoplayer.media;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 功能：使用MediaCodec 解码器实现播放视频
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaMoviePlayer {

    private static final int TIMEOUT_USEC = 10000;	// 10msec

    private static final int STATE_STOP = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;

    // request code
    private static final int REQUEST_NON = 0;
    private static final int REQUEST_PREPARE = 1;
    private static final int REQUEST_START = 2;
    private static final int REQUEST_SEEK = 3;
    private static final int REQUEST_STOP = 4;
    private static final int REQUEST_PAUSE = 5;
    private static final int REQUEST_RESUME = 6;
    private static final int REQUEST_QUIT = 9;

    private static final boolean DEBUG = true;
    private static final String TAG_STATIC = "MediaMoviePlayer:";
    private final String TAG = TAG_STATIC + getClass().getSimpleName();

    private Context mContext;

    private final IPlayerListener mCallback;
    private final boolean mAudioEnabled;


    private final Object mSync = new Object();
    private volatile boolean mIsRunning;

    // 播放状态
    private int mPlayerState;
    // 播放请求
    private int mPlayerRequest;
    // 请求时间
    private long mRequestTime;
    // 视频地址
    private String mSourcePath;
    // 时长
    private long mDuration;
    // 媒体信息
    private MediaMetadataRetriever mMediaMetadata;

    // 视频
    private final Object mVideoSync = new Object();
    // 输出Surface
    private Surface mOutputSurface;
    // 视频解码器
    private MediaCodec mVideoMediaCodec;
    protected MediaExtractor mVideoMediaExtractor;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private ByteBuffer[] mVideoInputBuffers;
    private ByteBuffer[] mVideoOutputBuffers;
    // 视频轨道索引
    private volatile int mVideoTrackIndex;
    private volatile boolean mVideoInputDone;
    private volatile boolean mVideoOutputDone;
    private long previousVideoPresentationTimeUs = -1;

    // 视频信息
    private long mVideoStartTime;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mBitrate;
    private float mFrameRate;
    private int mRotation;

    // 音频
    private final Object mAudioSync = new Object();

    // 音频解码器
    private MediaCodec mAudioMediaCodec;
    private MediaExtractor mAudioMediaExtractor;
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private ByteBuffer[] mAudioInputBuffers;
    private ByteBuffer[] mAudioOutputBuffers;

    // 音频轨道索引
    private volatile int mAudioTrackIndex;
    private volatile boolean mAudioInputDone;
    private volatile boolean mAudioOutputDone;
    private long previousAudioPresentationTimeUs = -1;

    // 音频信息
    private long mAudioStartTime;
    private int mAudioChannels;
    private int mAudioSampleRate;
    private int mAudioInputBufSize;
    private boolean mHasAudio;
    private byte[] mAudioOutTempBuf;
    // 播放pcm
    private AudioTrack mAudioTrack;

    public MediaMoviePlayer(Context context,
                            final Surface outputSurface,
                            final IPlayerListener callback,
                            final boolean audioEnable) {

        if (DEBUG){
            Log.v(TAG, "Constructor:");
        }

        mContext = context;
        mOutputSurface = outputSurface;
        mCallback = callback;
        mAudioEnabled = true;
        // 开启播放任务
        new Thread(mMoviePlayerTask, TAG).start();

        synchronized (mSync) {
            try {
                if (!mIsRunning)
                    mSync.wait();
            } catch (final InterruptedException e) {

            }
        }
    }

    /**
     * 请求准备播放
     */
    public final void prepare() {

        if (DEBUG) {
            Log.v(TAG, "prepare:");
        }

        if (TextUtils.isEmpty(mSourcePath)) {
            return;
        }
        synchronized (mSync) {
//            mSourcePath = srcPath;
            mPlayerRequest = REQUEST_PREPARE;
            mSync.notifyAll();
        }
    }

    /**
     * 请求 开始播放
     */
    public final void play() {
        if (DEBUG) {
            Log.v(TAG, "play:");
        }
        synchronized (mSync) {
            if (mPlayerState == STATE_PLAYING) {
                return;
            }
            mPlayerRequest = REQUEST_START;
            mSync.notifyAll();
        }
    }

    /**
     * 请求seek到具体的时间帧
     * @param newTime 微妙
     */
    public final void seek(final long newTime) {
        if (DEBUG) {
            Log.v(TAG, "seek");
        }
        synchronized (mSync) {
            mPlayerRequest = REQUEST_SEEK;
            mRequestTime = newTime;
            mSync.notifyAll();
        }
    }

    /**
     * 请求停止播放
     */
    public final void stop() {
        if (DEBUG) {
            Log.v(TAG, "stop:");
        }

        synchronized (mSync) {
            if (mPlayerState != STATE_STOP) {
                mPlayerRequest = REQUEST_STOP;
                mSync.notifyAll();

                try {
                    mSync.wait(50);
                } catch (final InterruptedException e) {

                }
            }
        }
    }

    /**
     * 请求暂停
     */
    public final void pause() {
        if (DEBUG) {
            Log.v(TAG, "pause:");
        }
        synchronized (mSync) {
            mPlayerRequest = REQUEST_PAUSE;
            mSync.notifyAll();
        }
    }

    /**
     * 请求恢复播放
     */
    public final void resume() {
        if (DEBUG) {
            Log.v(TAG, "resume:");
        }
        synchronized (mSync) {
            mPlayerRequest = REQUEST_RESUME;
            mSync.notifyAll();
        }
    }

    /**
     * 请求释放所有资源 退出
     */
    public final void release() {
        if (DEBUG) {
            Log.v(TAG, "release:");
        }
        stop();
        synchronized (mSync) {
            mPlayerRequest = REQUEST_QUIT;
            mSync.notifyAll();
        }
    }


    /**
     * 总播放控制任务
     */
    private final Runnable mMoviePlayerTask = new Runnable() {
        @Override
        public final void run() {
            boolean localIsRunning = false;
            int localRequest;
            try {
                synchronized (mSync) {
                    localIsRunning = mIsRunning = true;
                    mPlayerState = STATE_STOP;
                    mPlayerRequest = REQUEST_NON;
                    mRequestTime = -1;
                    mSync.notifyAll();
                }
                for ( ; localIsRunning ; ) {
                    try {

                        synchronized (mSync) {
                            localIsRunning = mIsRunning;
                            localRequest = mPlayerRequest;
                            mPlayerRequest = REQUEST_NON;
                        }

                        if (localIsRunning) {
                            switch (mPlayerState) {
                                case STATE_STOP:
                                    localIsRunning = processStop(localRequest);
                                    break;
                                case STATE_PREPARED:
                                    localIsRunning = processPrepared(localRequest);
                                    break;
                                case STATE_PLAYING:
                                    localIsRunning = processPlaying(localRequest);
                                    break;
                                case STATE_PAUSED:
                                    localIsRunning = processPaused(localRequest);
                                    break;
                            }
                        }

                    } catch (final InterruptedException e) {
                        break;
                    } catch (final Exception e) {
                        Log.e(TAG, "MoviePlayerTask:", e);
                        break;
                    }
                }

            } finally {
                if (DEBUG) Log.v(TAG, "player task finished:local_isRunning=" + localIsRunning);
                handleStop();
            }
        }
    };

    /**
     * 当前为停止状态时 处理请求
     * @param localRequest
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private final boolean processStop(final int localRequest) throws InterruptedException, IOException {
        boolean localIsRunning = true;
        switch (localRequest) {
            case REQUEST_PREPARE:
                // 处理准备请求
                handlePrepare(mSourcePath);
                break;
            case REQUEST_START:
            case REQUEST_PAUSE:
            case REQUEST_RESUME:
                throw new IllegalStateException("processStop invalid localRequest:" + localRequest);
            case REQUEST_QUIT:
                // 处理退出
                localIsRunning = false;
                break;
            default:

                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        }
        synchronized (mSync) {
            localIsRunning &= mIsRunning;
        }
        return localIsRunning;
    }

    /**
     * 当前为准备状态时 处理请求
     * @param localRequest 请求
     * @return
     * @throws InterruptedException
     */
    private final boolean processPrepared(final int localRequest) throws InterruptedException {
        boolean localIsRunning = true;
        switch (localRequest) {
            case REQUEST_START:
                // 请求开始
                handleStart();
                break;
            case REQUEST_PAUSE:
            case REQUEST_RESUME:
                throw new IllegalStateException("processPrepared invalid localRequest:" + localRequest);
            case REQUEST_STOP:
                // 请求停止
                handleStop();
                break;
            case REQUEST_QUIT:
                // 请求退出
                localIsRunning = false;
                break;
            default:

                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        }

        synchronized (mSync) {
            localIsRunning &= mIsRunning;
        }
        return localIsRunning;
    }

    /**
     * 当前状态是 播放状态时，处理请求
     * @param localRequest
     * @return
     */
    private final boolean processPlaying(final int localRequest) {
        boolean localIsRunning = true;
        switch (localRequest) {
            case REQUEST_PREPARE:
            case REQUEST_START:
            case REQUEST_RESUME:
                throw new IllegalStateException("processPlaying invalid localRequest:" + localRequest);
            case REQUEST_SEEK:
                // 处理seek请求
                handleSeek(mRequestTime);
                break;
            case REQUEST_STOP:
                // 处理停止请求
                handleStop();
                break;
            case REQUEST_PAUSE:
                // 处理暂停请求
                handlePause();
                break;
            case REQUEST_QUIT:
                // 处理退出请求
                localIsRunning = false;
                break;
            default:
                handleLoop(mCallback);
                break;
        }

        synchronized (mSync) {
            localIsRunning &= mIsRunning;
        }
        return localIsRunning;
    }

    /**
     * 当前状态是 暂停状态时，处理请求
     * @param localRequest
     * @return
     * @throws InterruptedException
     */
    private final boolean processPaused(final int localRequest) throws InterruptedException {
        boolean localIsRunning = true;
        switch (localRequest) {
            case REQUEST_PREPARE:
            case REQUEST_START:
                throw new IllegalStateException("processPaused invalid localRequest:" + localRequest);
            case REQUEST_SEEK:
                // 处理seek请求
                handleSeek(mRequestTime);
                break;
            case REQUEST_STOP:
                // 处理停止请求
                handleStop();
                break;
            case REQUEST_RESUME:
                // 处理恢复播放请求
                handleResume();
                break;
            case REQUEST_QUIT:
                // 处理退出请求
                localIsRunning = false;
                break;
            default:
                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        }

        synchronized (mSync) {
            localIsRunning &= mIsRunning;
        }
        return localIsRunning;
    }

    /**
     * 处理准备状态
     * @param sourceFile
     * @throws IOException
     */
    private final void handlePrepare(final String sourceFile) throws IOException {
        if (DEBUG) Log.v(TAG, "handlePrepare:" + sourceFile);

        synchronized (mSync) {
            // 准备状态的上一个状态必须是停止状态
            if (mPlayerState != STATE_STOP) {
                throw new RuntimeException(" handlePrepare invalid state:" + mPlayerState);
            }
        }
        final File src = new File(sourceFile);
        if (TextUtils.isEmpty(sourceFile) || !src.canRead()) {
            throw new FileNotFoundException("Unable to read " + sourceFile);
        }

        // 创建媒体信息
        mMediaMetadata = new MediaMetadataRetriever();
        mMediaMetadata.setDataSource(sourceFile);

        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;



        // 更新视频信息
        updateMovieInfo();
        // 准备视频解码
        mVideoTrackIndex = internalPrepareVideo(sourceFile);
        // 准备音频解码
        if (mAudioEnabled) {
            mAudioTrackIndex = internalPrepareAudio(sourceFile);
        }

        // 是否有音频
        mHasAudio = mAudioTrackIndex >= 0;
        if ((mVideoTrackIndex < 0) && (mAudioTrackIndex < 0)) {
            throw new RuntimeException("No video and audio track found in " + sourceFile);
        }

        // 进入准备状态
        synchronized (mSync) {
            mPlayerState = STATE_PREPARED;
        }

        // 回到
        if (mCallback != null) {
            mCallback.onPrepared();
        }
    }


    /**
     * 处理开始请求
     */
    private final void handleStart() {
        if (DEBUG) {
            Log.v(TAG, "handleStart:");
        }
        synchronized (mSync) {
            // 上一个状态必须是 准备状态
            if (mPlayerState != STATE_PREPARED)
                throw new RuntimeException("invalid state:" + mPlayerState);

            mPlayerState = STATE_PLAYING;
        }
        if (mRequestTime > 0) {
            handleSeek(mRequestTime);
        }

        // 视频解码
        previousVideoPresentationTimeUs = -1;

        mVideoInputDone = true;
        mVideoOutputDone = true;

        // 视频解码线程
        Thread videoThread = null;

        if (mVideoTrackIndex >= 0) {
            final MediaCodec videoDecoder = internalStartVideo(mVideoMediaExtractor, mVideoTrackIndex);
            if (videoDecoder != null) {
                mVideoMediaCodec = videoDecoder;
                mVideoBufferInfo = new MediaCodec.BufferInfo();
                // 解码器的输如输出缓存
                mVideoInputBuffers = videoDecoder.getInputBuffers();
                mVideoOutputBuffers = videoDecoder.getOutputBuffers();
            }
            mVideoInputDone = false;
            mVideoOutputDone = false;
            // 开始视频解码任务
            videoThread = new Thread(mVideoTask, "VideoTask");
        }

        // 音频解码
        Thread audioThread = null;
        previousAudioPresentationTimeUs = -1;
        mAudioInputDone = true;
        mAudioOutputDone = true;
        if (mAudioTrackIndex >= 0) {
            final MediaCodec audioDecoder = internalStartAudio(mAudioMediaExtractor, mAudioTrackIndex);
            if (audioDecoder != null) {
                mAudioMediaCodec = audioDecoder;
                mAudioBufferInfo = new MediaCodec.BufferInfo();
                mAudioInputBuffers = audioDecoder.getInputBuffers();
                mAudioOutputBuffers = audioDecoder.getOutputBuffers();
            }
            mAudioInputDone = false;
            mAudioOutputDone = false;
            // 开始音频解码任务
            audioThread = new Thread(mAudioTask, "AudioTask");
        }
        if (videoThread != null) {
            videoThread.start();
        }
        if (audioThread != null){
            audioThread.start();
        }
    }


    /**
     * 更新媒体信息
     */
    protected void updateMovieInfo() {
        mVideoWidth = mVideoHeight = mRotation = mBitrate = 0;
        mDuration = 0;
        mFrameRate = 0;
        // 视频宽
        String value = mMediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        if (!TextUtils.isEmpty(value)) {
            mVideoWidth = Integer.parseInt(value);
        }
        // 视频高
        value = mMediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if (!TextUtils.isEmpty(value)) {
            mVideoHeight = Integer.parseInt(value);
        }
        // 视频旋转角度
        value = mMediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (!TextUtils.isEmpty(value)) {
            mRotation = Integer.parseInt(value);
        }
        // 视频比特率
        value = mMediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        if (!TextUtils.isEmpty(value)) {
            mBitrate = Integer.parseInt(value);
        }
        // 视频时长
        value = mMediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (!TextUtils.isEmpty(value)) {
            mDuration = Long.parseLong(value) * 1000;
        }
    }

    /**
     *
     */
    private final void handlePause() {
        if (DEBUG) {
            Log.v(TAG, "handlePause:");
        }
    }

    /**
     *
     */
    private final void handleResume() {
        if (DEBUG) {
            Log.v(TAG, "handleResume:");
        }
    }

    /**
     * 处理停止请求
     */
    private final void handleStop() {
        if (DEBUG) {
            Log.v(TAG, "handleStop:");
        }
        synchronized (mVideoTask) {
            if (mVideoTrackIndex >= 0) {
                mVideoOutputDone = true;

                for ( ; !mVideoInputDone ;)
                    try {
                        mVideoTask.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }

                internalStopVideo();
                mVideoTrackIndex = -1;
            }

            mVideoOutputDone = mVideoInputDone = true;
        }
        synchronized (mAudioTask) {
            if (mAudioTrackIndex >= 0) {
                mAudioOutputDone = true;

                for ( ; !mAudioInputDone ;)
                    try {
                        mAudioTask.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                internalStopAudio();
                mAudioTrackIndex = -1;
            }
            mAudioOutputDone = mAudioInputDone = true;
        }

        if (mVideoMediaCodec != null) {
            mVideoMediaCodec.stop();
            mVideoMediaCodec.release();
            mVideoMediaCodec = null;
        }

        if (mAudioMediaCodec != null) {
            mAudioMediaCodec.stop();
            mAudioMediaCodec.release();
            mAudioMediaCodec = null;
        }

        if (mVideoMediaExtractor != null) {
            mVideoMediaExtractor.release();
            mVideoMediaExtractor = null;
        }

        if (mAudioMediaExtractor != null) {
            mAudioMediaExtractor.release();
            mAudioMediaExtractor = null;
        }

        mVideoBufferInfo = mAudioBufferInfo = null;
        mVideoInputBuffers = mVideoOutputBuffers = null;
        mAudioInputBuffers = mAudioOutputBuffers = null;

        if (mMediaMetadata != null) {
            mMediaMetadata.release();
            mMediaMetadata = null;
        }

        synchronized (mSync) {
            mPlayerState = STATE_STOP;
        }

        if(mCallback != null) {
            mCallback.onFinished();
        }
    }


    /**
     * @param frameCallback
     */
    private final void handleLoop(final IPlayerListener frameCallback) {
		if (DEBUG) {
		    Log.d(TAG, "handleLoop");
        }

        synchronized (mSync) {
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        if (mVideoInputDone && mVideoOutputDone && mAudioInputDone && mAudioOutputDone) {
            if (DEBUG) {
                Log.d(TAG, "Reached EOS, looping check");
            }
            handleStop();
        }
    }


    /**
     *
     */
    protected void internalStopVideo() {
        if (DEBUG) {
            Log.v(TAG, "internalStopVideo:");
        }
    }

    /**
     *
     */
    protected void internalStopAudio() {
        if (DEBUG) {
            Log.v(TAG, "internalStopAudio:");
        }
        if (mAudioTrack != null) {
            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED){
                mAudioTrack.stop();
            }
            mAudioTrack.release();
            mAudioTrack = null;
        }
        mAudioOutTempBuf = null;
    }



    /**
     * 选择视频轨道
     * @param sourceFile
     * @return first video track index, -1 if not found
     */
    protected int internalPrepareVideo(final String sourceFile) {
        int trackIndex = -1;
        mVideoMediaExtractor = new MediaExtractor();
        try {
            mVideoMediaExtractor.setDataSource(sourceFile);
            // 选择轨道
            trackIndex = selectTrack(mVideoMediaExtractor, "video/");

            if (trackIndex >= 0) {
                mVideoMediaExtractor.selectTrack(trackIndex);
                // 轨道的媒体格式
                final MediaFormat mediaFormat = mVideoMediaExtractor.getTrackFormat(trackIndex);
                // 宽高时长
                mVideoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                mVideoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                mDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                mFrameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);


                if (DEBUG) {
                    Log.v(TAG, String.format("format:size(%d,%d),duration=%d,bps=%d,framerate=%f,rotation=%d",
                            mVideoWidth,
                            mVideoHeight,
                            mDuration,
                            mBitrate,
                            mFrameRate,
                            mRotation));
                }
            }

        } catch (final IOException e) {
            Log.w(TAG, e);
        }
        return trackIndex;
    }


    /**
     * 选择音频轨道
     * @param sourceFile
     * @return first audio track index, -1 if not found
     */
    protected int internalPrepareAudio(final String sourceFile) {
        int trackIndex = -1;
        mAudioMediaExtractor = new MediaExtractor();
        try {
            mAudioMediaExtractor.setDataSource(sourceFile);
            // 选择轨道
            trackIndex = selectTrack(mAudioMediaExtractor, "audio/");

            if (trackIndex >= 0) {
                mAudioMediaExtractor.selectTrack(trackIndex);
                // 音频轨道格式
                final MediaFormat mediaFormat = mAudioMediaExtractor.getTrackFormat(trackIndex);
                mAudioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                mAudioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//                mBitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                // 最小输入大小
                final int minBufferSize = AudioTrack.getMinBufferSize(mAudioSampleRate,
                        (mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT);
                // 最大输入大小
                final int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

                mAudioInputBufSize =  minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                if (mAudioInputBufSize > maxInputSize) {
                    mAudioInputBufSize = maxInputSize;
                }

                final int frameSizeInBytes = mAudioChannels * 2;
                mAudioInputBufSize = (mAudioInputBufSize / frameSizeInBytes) * frameSizeInBytes;

                if (DEBUG) {
                    Log.v(TAG, String.format("getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d",
                            minBufferSize,
                            maxInputSize,
                            mAudioInputBufSize));
                }
                //
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mAudioSampleRate,
                        (mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT,
                        mAudioInputBufSize,
                        AudioTrack.MODE_STREAM);
                try {
                    // 播放状态
                    mAudioTrack.play();
                } catch (final Exception e) {
                    Log.e(TAG, "failed to start audio track playing", e);
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
            }
        } catch (final IOException e) {
            Log.w(TAG, e);
        }
        return trackIndex;
    }

    /**
     * 内部开始解码，生成配置视频解码器
     * @param mediaExtractor
     * @param trackIndex
     * @return
     */
    protected MediaCodec internalStartVideo(final MediaExtractor mediaExtractor, final int trackIndex) {
        if (DEBUG) {
            Log.v(TAG, "internalStartVideo:");
        }
        MediaCodec videoDecoder = null;
        if (trackIndex >= 0) {
            final MediaFormat mediaFormat = mediaExtractor.getTrackFormat(trackIndex);
            final String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            try {
                videoDecoder = MediaCodec.createDecoderByType(mime);
                // 配置 surface
                videoDecoder.configure(mediaFormat, mOutputSurface, null, 0);
                videoDecoder.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
                videoDecoder = null;
            }
            if (DEBUG) {
                Log.v(TAG, "internalStartVideo:videoDecoder started");
            }
        }
        return videoDecoder;
    }

    /**
     * 内部开始解码，生成配置音频解码器
     * @param mediaExtractor
     * @param trackIndex
     * @return
     */
    protected MediaCodec internalStartAudio(final MediaExtractor mediaExtractor, final int trackIndex) {
        if (DEBUG) {
            Log.v(TAG, "internalStartAudio:");
        }
        MediaCodec audioDecoder = null;
        if (trackIndex >= 0) {
            final MediaFormat mediaFormat = mediaExtractor.getTrackFormat(trackIndex);
            final String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            try {
                audioDecoder = MediaCodec.createDecoderByType(mime);
                audioDecoder.configure(mediaFormat, null, null, 0);
                audioDecoder.start();
                if (DEBUG) {
                    Log.v(TAG, "internalStartAudio:audioDecoder started");
                }
                // 解码器的输出buffers
                final ByteBuffer[] buffers = audioDecoder.getOutputBuffers();
                int sz = buffers[0].capacity();
                if (sz <= 0)
                    sz = mAudioInputBufSize;
                if (DEBUG) {
                    Log.v(TAG, "AudioOutputBufSize:" + sz);
                }

                mAudioOutTempBuf = new byte[sz];
            } catch (final IOException e) {
                Log.w(TAG, e);
                audioDecoder = null;
            }
        }
        return audioDecoder;
    }

    /**
     * 处理seek
     * @param newTime
     */
    private final void handleSeek(final long newTime) {
        if (DEBUG) Log.d(TAG, "handleSeek");
        if (newTime < 0) return;

        if (mVideoTrackIndex >= 0) {
            mVideoMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mVideoMediaExtractor.advance();
        }
        if (mAudioTrackIndex >= 0) {
            mAudioMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mAudioMediaExtractor.advance();
        }
        mRequestTime = -1;
    }



    /**
     * 视频播放任务
     */
    private final Runnable mVideoTask = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.v(TAG, "VideoTask:start");
            }
            for (; mIsRunning && !mVideoInputDone && !mVideoOutputDone ;) {
                // 循环
                try {
                    // 读一帧数据给 解码器
                    if (!mVideoInputDone) {
                        handleInputVideo();
                    }
                    // 输出一帧数据到 surface
                    if (!mVideoOutputDone) {
                        handleOutputVideo(mCallback);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "VideoTask:", e);
                    break;
                }
            }

            if (DEBUG) Log.v(TAG, "VideoTask:finished");
            synchronized (mVideoTask) {
                mVideoInputDone = mVideoOutputDone = true;
                mVideoTask.notifyAll();
            }
        }
    };

    /**
     * 音频播放任务
     */
    private final Runnable mAudioTask = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "AudioTask:start");
            for (; mIsRunning && !mAudioInputDone && !mAudioOutputDone ;) {
                // 循环
                try {
                    if (!mAudioInputDone) {
                        handleInputAudio();
                    }
                    if (!mAudioOutputDone) {
                        handleOutputAudio(mCallback);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "VideoTask:", e);
                    break;
                }
            }
            if (DEBUG) Log.v(TAG, "AudioTask:finished");
            synchronized (mAudioTask) {
                mAudioInputDone = mAudioOutputDone = true;
                mAudioTask.notifyAll();
            }
        }
    };


    /**
     * 处理输入视频：读一帧视频数据给解码器
     */
    private final void handleInputVideo() {
        final long presentationTimeUs = mVideoMediaExtractor.getSampleTime();
/*		if (presentationTimeUs < previousVideoPresentationTimeUs) {
    		presentationTimeUs += previousVideoPresentationTimeUs - presentationTimeUs; // + EPS;
    	}
    	previousVideoPresentationTimeUs = presentationTimeUs; */
        // 读一帧视频数据给解码器
        final boolean b = internalProcessInput(mVideoMediaCodec, mVideoMediaExtractor, mVideoInputBuffers, presentationTimeUs, false);
        if (!b) {
            // 读结束
            if (DEBUG) {
                Log.i(TAG, "video track input reached EOS");
            }
            while (mIsRunning) {
                // 向解码器 写入 flag = END_OF_STREAM的Buffer
                final int inputBufIndex = mVideoMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);

                if (inputBufIndex >= 0) {
                    mVideoMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (DEBUG) {
                        Log.v(TAG, "sent input EOS:" + mVideoMediaCodec);
                    }
                    break;
                }
            }
            synchronized (mVideoTask) {
                mVideoInputDone = true;
                mVideoTask.notifyAll();
            }
        }
    }
    /**
     * 读一帧视频数据给解码器
     * @param codec
     * @param extractor
     * @param inputBuffers
     * @param presentationTimeUs
     * @param isAudio
     */
    protected boolean internalProcessInput(final MediaCodec codec, final MediaExtractor extractor, final ByteBuffer[] inputBuffers, final long presentationTimeUs, final boolean isAudio) {
		if (DEBUG) Log.v(TAG, "internalProcessInput:presentationTimeUs=" + presentationTimeUs);
        boolean result = true;
        while (mIsRunning) {
            // 获得一个输入缓存
            final int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }

            if (inputBufIndex >= 0) {
                // 读数据到intputBuffer
                final int size = extractor.readSampleData(inputBuffers[inputBufIndex], 0);

                if (size > 0) {
                    // 把 buffer数据送入解码器 解码
                    codec.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
                }
                // false 表示没有数据可读
                result = extractor.advance();
                break;
            }
        }
        return result;
    }

    /**
     * 输出一帧数据到 surface
     * @param frameCallback
     */
    private final void handleOutputVideo(final IPlayerListener frameCallback) {
    	if (DEBUG) {
    	    Log.v(TAG, "handleOutputVideo:");
        }
        while (mIsRunning && !mVideoOutputDone) {

            // 获取 解码器输出buffer-有数据，信息在BufferInfo里
            final int outputBufferIndex = mVideoMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return;

            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                // 输出缓存数组改变，需要使用新的缓存数据
                mVideoOutputBuffers = mVideoMediaCodec.getOutputBuffers();
                if (DEBUG) {
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
                }

            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                // 新的格式
                final MediaFormat newFormat = mVideoMediaCodec.getOutputFormat();
                if (DEBUG) {
                    Log.d(TAG, "video decoder output format changed: " + newFormat);
                }

            } else if (outputBufferIndex < 0) {

                throw new RuntimeException("unexpected result from video decoder.dequeueOutputBuffer: " + outputBufferIndex);

            } else {

                boolean doRender = false;
                if (mVideoBufferInfo.size > 0) {
                    doRender = !internalWriteVideo(mVideoOutputBuffers[outputBufferIndex], 0, mVideoBufferInfo.size, mVideoBufferInfo.presentationTimeUs);

                    if (doRender) {
                        // 调整显示时间，不太懂
                        if (!frameCallback.onFrameAvailable(mVideoBufferInfo.presentationTimeUs))
                            mVideoStartTime = adjustPresentationTime(mVideoSync, mVideoStartTime, mVideoBufferInfo.presentationTimeUs);
                    }
                }

                // 释放输出缓存，第二个参数是true会渲染到surface
                mVideoMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // 输出结束
                    if (DEBUG) {
                        Log.d(TAG, "video:output EOS");
                    }
                    synchronized (mVideoTask) {
                        mVideoOutputDone = true;
                        mVideoTask.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * @param buffer
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return
     */
    protected boolean internalWriteVideo(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
		if (DEBUG) Log.v(TAG, "internalWriteVideo");
        return false;
    }

    /**
     * 调整显示时间
     * adjusting frame rate
     * @param sync
     * @param startTime
     * @param presentationTimeUs
     * @return startTime
     */
    protected long adjustPresentationTime(final Object sync, final long startTime, final long presentationTimeUs) {
        if (startTime > 0) {
            for (long t = presentationTimeUs - (System.nanoTime() / 1000 - startTime); t > 0; t = presentationTimeUs - (System.nanoTime() / 1000 - startTime)) {
                synchronized (sync) {
                    try {
                        sync.wait(t / 1000, (int)((t % 1000) * 1000));
                    } catch (final InterruptedException e) {

                    }
                    if ((mPlayerState == REQUEST_STOP) || (mPlayerState == REQUEST_QUIT))
                        break;
                }
            }
            return startTime;
        } else {
            return System.nanoTime() / 1000;
        }
    }

    /**
     * 读一帧音频数据给解码器
     */
    private final void handleInputAudio() {
        final long presentationTimeUs = mAudioMediaExtractor.getSampleTime();
/*		if (presentationTimeUs < previousAudioPresentationTimeUs) {
    		presentationTimeUs += previousAudioPresentationTimeUs - presentationTimeUs; //  + EPS;
    	}
    	previousAudioPresentationTimeUs = presentationTimeUs; */
        // 读一帧音频数据给解码器
        final boolean b = internalProcessInput(mAudioMediaCodec, mAudioMediaExtractor, mAudioInputBuffers, presentationTimeUs, true);
        if (!b) {
            // 读结束
            if (DEBUG) {
                Log.i(TAG, "audio track input reached EOS");
            }
            while (mIsRunning) {
                // 写入eos
                final int inputBufIndex = mAudioMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    mAudioMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (DEBUG) {
                        Log.v(TAG, "sent input EOS:" + mAudioMediaCodec);
                    }
                    break;
                }
            }
            synchronized (mAudioTask) {
                mAudioInputDone = true;
                mAudioTask.notifyAll();
            }
        }
    }

    /**
     * 输出一帧数据到 audioTrack
     * @param frameCallback
     */
    private final void handleOutputAudio(final IPlayerListener frameCallback) {
		if (DEBUG) {
		    Log.v(TAG, "handleOutputAudio:");
        }
        while (mIsRunning && !mAudioOutputDone) {

            final int dequeueOutputBufferIndex = mAudioMediaCodec.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
            if (dequeueOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return;

            } else if (dequeueOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                // 需要使用 新的缓存数组
                mAudioOutputBuffers = mAudioMediaCodec.getOutputBuffers();
                if (DEBUG) {
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
                }

            } else if (dequeueOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                // 需要使用新格式
                final MediaFormat newFormat = mAudioMediaCodec.getOutputFormat();
                if (DEBUG){
                    Log.d(TAG, "audio decoder output format changed: " + newFormat);
                }

            } else if (dequeueOutputBufferIndex < 0) {
                throw new RuntimeException("unexpected result from audio decoder.dequeueOutputBuffer: " + dequeueOutputBufferIndex);

            } else {

                if (mAudioBufferInfo.size > 0) {
                    // 把audiobuffer写到 audioTrack
                    internalWriteAudio(mAudioOutputBuffers[dequeueOutputBufferIndex], 0, mAudioBufferInfo.size, mAudioBufferInfo.presentationTimeUs);

                    if (!frameCallback.onFrameAvailable(mAudioBufferInfo.presentationTimeUs)) {
                        mAudioStartTime = adjustPresentationTime(mAudioSync, mAudioStartTime, mAudioBufferInfo.presentationTimeUs);
                    }
                }

                // 释放输出缓存
                mAudioMediaCodec.releaseOutputBuffer(dequeueOutputBufferIndex, false);

                if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // 输出六结束
                    if (DEBUG) {
                        Log.d(TAG, "audio:output EOS");
                    }
                    synchronized (mAudioTask) {
                        mAudioOutputDone = true;
                        mAudioTask.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * 把audiobuffer写出 audioTrack
     * @param buffer
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return ignored
     */
    protected boolean internalWriteAudio(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
		if (DEBUG) {
		    Log.d(TAG, "internalWriteAudio");
        }
        if (mAudioOutTempBuf.length < size) {
            mAudioOutTempBuf = new byte[size];
        }
        buffer.position(offset);
        buffer.get(mAudioOutTempBuf, 0, size);
        buffer.clear();
        if (mAudioTrack != null)
            mAudioTrack.write(mAudioOutTempBuf, 0, size);
        return true;
    }


    /**
     * 选择轨道
     * @param extractor
     * @param mimeType
     * @return
     */
    protected static final int selectTrack(final MediaExtractor extractor, final String mimeType) {
        final int numTracks = extractor.getTrackCount();
        MediaFormat format;
        String mime;
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                if (DEBUG) {
                    Log.d(TAG_STATIC, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }


    /**
     * 视频宽
     * @return
     */
    public final int getWidth() {
        return mVideoWidth;
    }

    /**
     * 视频高
     * @return
     */
    public final int getHeight() {
        return mVideoHeight;
    }

    /**
     * 比特率
     * @return
     */
    public final int getBitRate() {
        return mBitrate;
    }

    /**
     * 帧率
     * @return
     */
    public final float getFrameRate() {
        return mFrameRate;
    }

    /**
     * 旋转角度
     * @return 0, 90, 180, 270
     */
    public final int getRotation() {
        return mRotation;
    }

    /**
     * get duration time as micro seconds
     * @return
     */
    public final long getDurationUs() {
        return mDuration;
    }

    /**
     * get audio sampling rate[Hz]
     * @return
     */
    public final int getSampleRate() {
        return mAudioSampleRate;
    }

    public final boolean hasAudio() {
        return mHasAudio;
    }

    /**
     * 设置地址
     * @param sourcePath
     */
    public void setSourcePath(String sourcePath) {
        this.mSourcePath = sourcePath;
    }

    /**
     * 是否在播放
     * @return
     */
    public boolean isPlaying() {
        return mIsRunning;
    }



}
