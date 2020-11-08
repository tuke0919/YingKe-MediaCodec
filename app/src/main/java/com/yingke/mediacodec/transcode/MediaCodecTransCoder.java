package com.yingke.mediacodec.transcode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;


import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.transcode.listener.SlimProgressListener;
import com.yingke.mediacodec.transcode.opengl.CodecInputSurface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * trans code for video to mp4
 * mediacodec support  3gp and mp4
 */
public class MediaCodecTransCoder {

    public static final String TAG = "MediaCodecTransCoder";
    private static final boolean VERBOSE = true;

    private final static String MIME_TYPE = "video/avc";

    private static final int OUTPUT_FRAME_RATE = 25;               // 25fps
    private static final int OUTPUT_IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int TIMEOUT_USEC = 2500;


    private String mSrcVideoPath;
    private String mOutputVideoPath;

    private MediaMuxer mMediaMuxer;
    private MediaCodec mVideoEncoder;
    private MediaCodec mVideoDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mMuxerVideoTrackIndex;
    private CodecInputSurface mInputSurface;

    private int mNewWidth = -1;
    private int mNewHeight = -1;
    private int mNewBitRate = -1;

    private int mOldWith;
    private int mOldHeight;
    private long mOldDuration;

    // 输入视频到最大输入大小
    private int mMaxVideoIntputSize;
    // 输入视频的帧率
    private int mVideoFrameRate;

    // 输入音频到最大输入大小
    private int mMaxAudioIntputSize;

    private SlimProgressListener mListener;


    public MediaCodecTransCoder() {

    }

    /**
     * @param srcVideoPath
     * @param outputVideoPath
     * @param newWidth
     * @param newHeight
     * @param newBitrate
     * @param listener
     * @return
     */
    public boolean convertVideo(final String srcVideoPath,
                                String outputVideoPath,
                                int newWidth,
                                int newHeight,
                                int newBitrate,
                                SlimProgressListener listener) {

        this.mSrcVideoPath = srcVideoPath;
        this.mOutputVideoPath = outputVideoPath;
        this.mListener = listener;

        if (checkParamsError(newWidth, newHeight, newBitrate)) {
            return false;
        }

        // 原始视频的信息
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mSrcVideoPath);
        mOldWith = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        mOldHeight = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        mOldDuration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;

        mNewBitRate = newBitrate;
        mNewWidth = newWidth;
        mNewHeight = newHeight;


        File outputFile = new File(outputVideoPath);
        File inputFile = new File(mSrcVideoPath);
        if (!inputFile.canRead()) {
            return false;
        }

        boolean result = false;

        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;

        try {
            videoExtractor = createExtractor();
            int videoExtractorTrackIndex = getAndSelectTrackIndex(videoExtractor, true);

            audioExtractor = createExtractor();
            int audioExtractorTrackIndex = getAndSelectTrackIndex(audioExtractor, false);

            mMediaMuxer = new MediaMuxer(mOutputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            if (videoExtractorTrackIndex != -1) {
                MediaFormat videoTrackFormat = videoExtractor.getTrackFormat(videoExtractorTrackIndex);
                Log.e(TAG, "doDownload : videoTrackFormat =  ");
                outputMediaFormat(videoTrackFormat);

                mMaxVideoIntputSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                mVideoFrameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

                Log.e(TAG, "doDownload :"
                        + " mMaxVideoIntputSize =  " + mMaxVideoIntputSize
                        + " mVideoFrameRate = " + mVideoFrameRate);

            }
            int muxerAudioTrackIndex = -1;
            if (audioExtractorTrackIndex >= 0) {
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                MediaFormat audioTrackFormat = audioExtractor.getTrackFormat(audioExtractorTrackIndex);
                Log.e(TAG, "audioTrackFormat =  ");
                outputMediaFormat(audioTrackFormat);
                mMaxAudioIntputSize = audioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

                muxerAudioTrackIndex = mMediaMuxer.addTrack(audioTrackFormat);

            }

            if (newWidth == mOldWith && newHeight == mOldHeight) {
                PlayerLog.d(TAG, "-------宽高相同，不解码，不编码-------");

                // 宽高相同，不解码，不编码
                int muxerVideoTrackIndex = mMediaMuxer.addTrack(videoExtractor.getTrackFormat(videoExtractorTrackIndex));
                mMediaMuxer.start();
                // 写抽取器到muxer
                writeTrackToMuxer(videoExtractor, muxerVideoTrackIndex, mMaxVideoIntputSize, true);

            } else {
                PlayerLog.d(TAG, "-------解码，在编码 ，使用新的格式-------");

                // 解码，在编码 ，使用新的格式
                decodeAndEncodeVideo(videoExtractor, videoExtractorTrackIndex);

            }
            // 直接写音频到muxer
            PlayerLog.d(TAG, "-------直接写音频到muxer，不解码，不编码------- muxerAudioTrackIndex = " + muxerAudioTrackIndex);

            writeTrackToMuxer(audioExtractor, muxerAudioTrackIndex, mMaxAudioIntputSize, false);

            result = true;

        } catch (Exception e) {
            e.printStackTrace();
            result = false;

        } finally {
            if (audioExtractor != null) {
                audioExtractor.release();
                audioExtractor = null;
            }
        }

        releaseCoder();
        return result;

    }


    private boolean checkParamsError(int newWidth, int newHeight, int newBitrate) {
        if (newWidth <= 0 || newHeight <= 0 || newBitrate <= 0)
            return true;
        else
            return false;
    }

    /**
     * 不解码，编码extractor直接写到muxer
     *
     * @param mediaExtractor
     * @param muxerTrackIndex
     * @param maxIuputSize
     * @param isVideo
     */
    private void writeTrackToMuxer(MediaExtractor mediaExtractor, int muxerTrackIndex, int maxIuputSize, boolean isVideo) {

        PlayerLog.d(TAG, "-------writeTrackToMuxer 不解码，不编码 ------- isVideo = " + isVideo);

        if (muxerTrackIndex != -1) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.presentationTimeUs = 0;

            ByteBuffer buffer = ByteBuffer.allocate(maxIuputSize);

            boolean extractorDone = false;
            while (!extractorDone) {
                int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                if (isVideo) {
                    bufferInfo.offset = 0;
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    bufferInfo.presentationTimeUs += 1000 * 1000 / mVideoFrameRate;
                } else {
                    bufferInfo.offset = 0;
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = mediaExtractor.getSampleFlags();
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                }

                PlayerLog.e(TAG, "writeTrackToMuxer = "
                        + " isVideo = " + isVideo
                        + " offset = " + bufferInfo.offset
                        + " size = " + bufferInfo.size
                        + " flags = " + bufferInfo.flags
                        + " presentationTimeUs = " + bufferInfo.presentationTimeUs);


                mMediaMuxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo);

                boolean extractorFrameDone = mediaExtractor.advance();
                if (!extractorFrameDone) {
                    extractorDone = true;
                }
            }
        }
    }

    /**
     * 解码->编码视频
     * @param videoExtractor
     * @param videoExtractorTrackIndex
     */
    private void decodeAndEncodeVideo(MediaExtractor videoExtractor, int videoExtractorTrackIndex) {
        PlayerLog.d(TAG, "-------decodeAndEncodeVideo-------");

        MediaFormat videoExtractorTrackFormat = videoExtractor.getTrackFormat(videoExtractorTrackIndex);
        outputMediaFormat(videoExtractorTrackFormat);

        // 初始化解码器 和编码器
        prepareEncoder(videoExtractorTrackFormat);

        try {
            boolean outputDone = false;
            boolean inputDone = false;
            boolean decoderDone = false;

            ByteBuffer[] decoderInputBuffers = null;
            ByteBuffer[] encoderOutputBuffers = null;


            decoderInputBuffers = mVideoDecoder.getInputBuffers();
            encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            int muxerVideoTrackIndex = -1;
            PlayerLog.d(TAG, "-------开始循环 编解码-------");

            // 1, 每个循环抽取 一帧
            // 2，内循环 遍历 编码器和解码器的输出buffer

            while (!outputDone) {
                PlayerLog.e(TAG, "------- 外循环 -------" );
                PlayerLog.d(TAG, "------- outputDone -------" + outputDone);
                if (!inputDone) {
                    // 抽取一帧
                    PlayerLog.d(TAG, "-------inputDone -------" + inputDone);
                    PlayerLog.d(TAG, "-------抽取一帧 -------");

                    boolean extractorSampleDone = false;
                    int index = videoExtractor.getSampleTrackIndex();
                    PlayerLog.d(TAG, "-------getSampleTrackIndex -------" + index);
                    if (index == -1) {
                        // 抽取采样结束 写EOS帧
                        extractorSampleDone = true;
                        int inputBufIndex = mVideoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                        PlayerLog.d(TAG, "-------dequeueInputBuffer ------- inputBufIndex = " + inputBufIndex);
                        if (inputBufIndex >= 0) {
                            mVideoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            // 整个文件输入结束
                            inputDone = true;

                            PlayerLog.d(TAG, "-------整个文件输入结束 ------- " + inputDone);
                        }
                    } else if (index >= 0) {

                        int inputBufferIndex = mVideoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                        PlayerLog.d(TAG, "-------dequeueInputBuffer ------- inputBufferIndex = " + inputBufferIndex);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer;
                            if (Build.VERSION.SDK_INT < 21) {
                                inputBuffer = decoderInputBuffers[inputBufferIndex];
                            } else {
                                inputBuffer = mVideoDecoder.getInputBuffer(inputBufferIndex);
                            }
                            PlayerLog.d(TAG, "-------读取数据到buffer ------- inputBufferIndex = " + inputBufferIndex);

                            // 读取数据到buffer
                            int sampleSize = videoExtractor.readSampleData(inputBuffer, 0);
                            PlayerLog.d(TAG, "-------读取数据到buffer ------- sampleSize = " + sampleSize);

                            if (sampleSize < 0) {
                                mVideoDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                //  整个文件输入结束
                                inputDone = true;
                                PlayerLog.d(TAG, "-------整个文件输入结束 ------- " + inputDone);

                            } else {
                                mVideoDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                                // 指针指向 下一帧
                                videoExtractor.advance();

                                PlayerLog.d(TAG, "-------指针指向 下一帧 -------" );
                            }
                        }
                    }
                }

                // 解码器 输出可用
                boolean decoderOutputAvailable = !decoderDone;
                // 编码器 输出可用
                boolean encoderOutputAvailable = true;

                PlayerLog.e(TAG, "------- 内循环 -------"
                        + " decoderDone = " + decoderDone
                        + " decoderOutputAvailable = " + decoderOutputAvailable + " "
                        + " encoderOutputAvailable = " + encoderOutputAvailable );

                while (decoderOutputAvailable || encoderOutputAvailable) {

                    // 获取带有数据输出buffer
                    int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        PlayerLog.d(TAG, "------- encoderStatus ------- MediaCodec.INFO_TRY_AGAIN_LATER ");
                        // 暂无编码器输出
                        encoderOutputAvailable = false;

                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        PlayerLog.d(TAG, "------- encoderStatus ------- MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ");

                        if (Build.VERSION.SDK_INT < 21) {
                            encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
                        }

                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        PlayerLog.d(TAG, "------- encoderStatus ------- MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ");

                        // 新格式，只会调用一次
                        MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                        if (muxerVideoTrackIndex == -1) {
                            muxerVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
                            mMuxerVideoTrackIndex = muxerVideoTrackIndex;
                            mMediaMuxer.start();

                            PlayerLog.d(TAG, "------- Muxer.start() ------- mMuxerVideoTrackIndex = " + mMuxerVideoTrackIndex);
                        }

                    } else if (encoderStatus < 0) {
                        throw new RuntimeException("unexpected result from mVideoEncoder.dequeueOutputBuffer: " + encoderStatus);
                    } else {
                        // 编码器的输出Buffer 是要写到 混合器Muxer的
                        PlayerLog.d(TAG, "------- encoderStatus ------- " + encoderStatus);

                        ByteBuffer encodedData;
                        if (Build.VERSION.SDK_INT < 21) {
                            encodedData = encoderOutputBuffers[encoderStatus];
                        } else {
                            encodedData = mVideoEncoder.getOutputBuffer(encoderStatus);
                        }
                        if (encodedData == null) {
                            throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                        }

                        if (mBufferInfo.size > 1) {
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                PlayerLog.d(TAG, "------- 向muxer写数据 writeSampleData ------- mBufferInfo.size = " + mBufferInfo.size);

                                // 向muxer写数据
                                mMediaMuxer.writeSampleData(mMuxerVideoTrackIndex, encodedData, mBufferInfo);

                            } else if (muxerVideoTrackIndex == -1) {
                                // 刚开始 没有muxer轨道时

                                byte[] csd = new byte[mBufferInfo.size];
                                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                                encodedData.position(mBufferInfo.offset);
                                encodedData.get(csd);
                                ByteBuffer sps = null;
                                ByteBuffer pps = null;
                                for (int a = mBufferInfo.size - 1; a >= 0; a--) {
                                    if (a > 3) {
                                        if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
                                            sps = ByteBuffer.allocate(a - 3);
                                            pps = ByteBuffer.allocate(mBufferInfo.size - (a - 3));
                                            sps.put(csd, 0, a - 3).position(0);
                                            pps.put(csd, a - 3, mBufferInfo.size - (a - 3)).position(0);
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                // 其实是直接改变Muxer的帧格式 就行
                                MediaFormat newFormat = MediaFormat.createVideoFormat(MIME_TYPE, mNewWidth, mNewHeight);
                                if (sps != null && pps != null) {
                                    newFormat.setByteBuffer("csd-0", sps);
                                    newFormat.setByteBuffer("csd-1", pps);
                                }
                                muxerVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
                                mMediaMuxer.start();
                            }
                        }
                        // 编码器输出完成
                        outputDone = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        PlayerLog.d(TAG, "------- 编码器输出完成 ------- outputDone = " + outputDone);

                        // 释放buffer给编码器(encoder)，或者 把buffer渲染到 Surface上(decoder)
                        mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
                        PlayerLog.d(TAG, "------- releaseOutputBuffer ------- encoderStatus = " + encoderStatus);
                    }

                    // 编码器没有输出 下次循环
                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        PlayerLog.d(TAG, "------- encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER ------- continue ");
                        continue;
                    }
                    // 编码器 有数据输出了
                    PlayerLog.d(TAG, "------- 编码器 有数据输出了 ------- ");

                    if (!decoderDone) {
                        // 获取解码器输出buffer

                        int decoderStatus = mVideoDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            PlayerLog.d(TAG, "------- decoderStatus ------- MediaCodec.INFO_TRY_AGAIN_LATER ");

                            decoderOutputAvailable = false;
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            PlayerLog.d(TAG, "------- decoderStatus ------- MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ");

                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            PlayerLog.d(TAG, "------- decoderStatus ------- MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ");

                            MediaFormat newFormat = mVideoDecoder.getOutputFormat();
                            Log.e(TAG, "newFormat = " + newFormat);
                        } else if (decoderStatus < 0) {
                            throw new RuntimeException("unexpected result from mVideoDecoder.dequeueOutputBuffer: " + decoderStatus);
                        } else {
                            boolean doRender = false;
                            PlayerLog.d(TAG, "-------可以渲染  ------- mBufferInfo.size = " + mBufferInfo.size);
                            // 可以渲染
                            doRender = mBufferInfo.size != 0;

                            // 解码器的输出 是要渲染到 Surface，当然也可以 写到buffer, 这里是把Buffer给SurfaceTexture的图像流
                            mVideoDecoder.releaseOutputBuffer(decoderStatus, doRender);
                            PlayerLog.d(TAG, "------- mVideoDecoder releaseOutputBuffer  ------- ");

                            if (doRender) {
                                boolean errorWait = false;
                                try {
                                    mInputSurface.awaitNewImage();
                                } catch (Exception e) {
                                    errorWait = true;
                                    Log.e(TAG, e.getMessage());
                                }
                                if (!errorWait) {
                                    // opengl 画纹理图像Id到Surface
                                    mInputSurface.drawImage();
                                    mInputSurface.setPresentationTime(mBufferInfo.presentationTimeUs * 1000);

                                    if (mListener != null) {
                                        PlayerLog.d(TAG, "------- mVideoDecoder onProgress  ------- " + "presentationTimeUs = " + mBufferInfo.presentationTimeUs);
                                        mListener.onProgress((float) mBufferInfo.presentationTimeUs / (float) mOldDuration * 100);
                                    }
                                    // 把当前帧 送给 后边的编码器，去编码
                                    mInputSurface.swapBuffers();
                                    PlayerLog.d(TAG, "-------swapBuffers  ------- ");
                                }
                            }
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                PlayerLog.d(TAG, "------- decoder EOS ------- "
                                        + "decoderDone = " + decoderDone + " "
                                        + "decoderOutputAvailable = " + decoderOutputAvailable);
                                decoderDone = true;
                                decoderOutputAvailable = false;
                                Log.e(TAG, "decoder stream end");
                                mVideoEncoder.signalEndOfInputStream();
                            }
                        }
                    }
                }
            }

        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (videoExtractor != null) {
                videoExtractor.release();
                videoExtractor = null;
            }
        }

    }


    /**
     * 创建收抽取器
     * @return
     * @throws IOException
     */
    public MediaExtractor createExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mSrcVideoPath);
        return mediaExtractor;
    }

    /**
     * 获取并且选择 轨道
     * @param mediaExtractor
     * @param isVideo
     * @return
     */
    public int getAndSelectTrackIndex(MediaExtractor mediaExtractor, boolean isVideo) {

        for (int index = 0 ; index < mediaExtractor.getTrackCount(); index ++) {
            MediaFormat mediaFormat  = mediaExtractor.getTrackFormat(index);

            Log.e(TAG, "getAndSelectTrackIndex " + " index = " + index);
            outputMediaFormat(mediaFormat);

            if (isVideo) {
                if (isVideoTrack(mediaFormat)) {
                    mediaExtractor.selectTrack(index);
                    return index;
                }
            } else {
                if (isAudioTrack(mediaFormat)) {
                    mediaExtractor.selectTrack(index);
                    return index;
                }
            }
        }
        return -1;
    }


    /**
     * 是否 video轨道
     * @param mediaFormat
     * @return
     */
    public boolean isVideoTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/");
    }

    /**
     * 是否 audio 轨道
     * @param mediaFormat
     * @return
     */
    public boolean isAudioTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/");
    }

    /**
     * @param mediaFormat
     */
    private void outputMediaFormat(MediaFormat mediaFormat) {
        Class clazz = MediaFormat.class;
        try {
            Method getMap = clazz.getDeclaredMethod("getMap");
            //获取私有权限
            getMap.setAccessible(true);
            HashMap<String, Object> map = (HashMap<String, Object>) getMap.invoke(mediaFormat);

            if (map != null) {
                Set<String> keySet = map.keySet();
                for (String key : keySet) {
                    Object value = map.get(key);
                    String strValue = "";
                    if (value instanceof Integer) {
                        strValue = ((Integer) value).intValue() + "";
                    } else if (value instanceof Float) {
                        strValue = ((Float) value).floatValue() + "";
                    } else if (value instanceof Long) {
                        strValue = ((Long) value).longValue() + "";
                    } else if (value instanceof String){
                        strValue = (String) value;
                    }

                    Log.e(TAG, "outputMediaFormat " + " key = " + key + " value = " + value);
                }
            }

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }






    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     *  初始化编解码器
     */
    private void prepareEncoder(MediaFormat videoDecoderInputFormat) {
        PlayerLog.d(TAG, "-------decodeAndEncodeVideo-------");

        mBufferInfo = new MediaCodec.BufferInfo();


        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        // 创建编码器
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 配置编码器输出格式
        MediaFormat videoEncoderFormat = MediaFormat.createVideoFormat(MIME_TYPE, mNewWidth, mNewHeight);
        videoEncoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoEncoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, mNewBitRate);
        videoEncoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FRAME_RATE);
        videoEncoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_IFRAME_INTERVAL);

        PlayerLog.e(TAG, "videoEncoderFormat: " + videoEncoderFormat);

        // 配置编码器
        mVideoEncoder.configure(videoEncoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 创建 编码器输入的Surface，作为编码器的数据输入，把buffer渲染到Surface上即是对编码器写数据
        mInputSurface = new CodecInputSurface(mVideoEncoder.createInputSurface());
        mInputSurface.makeCurrent();
        mVideoEncoder.start();

        // 创建解码器
        try {
            mVideoDecoder = MediaCodec.createDecoderByType(videoDecoderInputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
        }
        PlayerLog.e(TAG, "videoDecoderInputFormat: " + videoDecoderInputFormat);

        // 创建opengl 渲染器
        mInputSurface.createRender();
        // 配置 解码器 传入一个Surface，解码数据渲染到surface
        mVideoDecoder.configure(videoDecoderInputFormat, mInputSurface.getSurface(), null, 0);
        mVideoDecoder.start();

        // 解码器的输出和编码器的输入已Surface 相连接

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.

        mMuxerVideoTrackIndex = -1;
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseCoder() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
            mVideoDecoder.release();
            mVideoDecoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }


}
