package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.transcode.opengl.CodecInputSurface;
import com.yingke.mediacodec.utils.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaFormat.KEY_HEIGHT;
import static android.media.MediaFormat.KEY_WIDTH;


/**
 * 功能：
 * </p>
 * <p>Copyright corp.xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MixVideoThread extends Thread {
    public static final String TAG = "MixVideoThread";

    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_USEC = 10;

    private static final int OUTPUT_BITRATE = 3000000;
    private static final int OUTPUT_FRAME_RATE = 25;
    private static final int OUTPUT_KEY_FRAME_INTERVAL = 10;
    private static final float BPP = 0.25f;

    // 混合器监听
    private OnMuxerListener mMediaMuxer;
    // 输入视频
    private List<VideoInfo> mInputVideos;
    // 输出视频 解码器，格式，分离器 列表
    private List<DecoderFormatExtractor> mDecoderFormatExtrators;

    // 最终输出格式
    // 宽高已第一个视频为准，宽高不一致会有视频会被拉伸/压缩，原因在CodecInputSurface.drawImage opengGl着色器没有设置合适的矩阵，后期优化吧
    private MediaFormat mVideoOutputFormat;
    // 编码器，只有一个编码器
    private MediaCodec mFinalEncoder;
    // 编码器输入Surface 和 解码器输出Surface，并互相传输数据
    private CodecInputSurface mCodecInputSurface;

    // 当前解码器
    private MediaCodec currentDecoder;
    // 当前分离器
    private MediaExtractor currentExtractor;


    public MixVideoThread(List<VideoInfo> inputFiles, OnMuxerListener mediaMuxer) {
        this.mInputVideos = inputFiles;
        this.mMediaMuxer = mediaMuxer;
        mDecoderFormatExtrators = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            prepare();
            connectMultiVideo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 为每个video创建 分离器和视频轨道
     */
    public void prepare() throws IOException {
        for (int i = 0; i < mInputVideos.size(); i++) {
            // 创建分离器
            MediaExtractor extractor = CodecUtil.createExtractor(mInputVideos.get(i).getPath());
            // 获取并选择视频轨道
            int trackIndex = CodecUtil.getAndSelectTrackIndex(extractor, true);

            DecoderFormatExtractor formatExtrator = new DecoderFormatExtractor();
            // 设置 视频轨道
            formatExtrator.setTrackIndex(trackIndex);
            // 设置 分离器
            formatExtrator.setMediaExtractor(extractor);

            MediaCodec decoder = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            // 设置解码器
            formatExtrator.setDecoder(decoder);
            // 设置 格式
            formatExtrator.setMediaFormat(mediaFormat);
            // 添加
            mDecoderFormatExtrators.add(formatExtrator);
        }

        // 根据第一个视频信息来确定编码信息
        VideoInfo firstVideo = mInputVideos.get(0);

        if (firstVideo.getRotation() == 0 || firstVideo.getRotation() == 180) {
            mVideoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, firstVideo.getWidth(), firstVideo.getHeight());
        } else {
            mVideoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, firstVideo.getHeight(),firstVideo.getWidth());
        }
        mVideoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mVideoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate(firstVideo.getWidth(), firstVideo.getHeight()));
        mVideoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FRAME_RATE);
        mVideoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_KEY_FRAME_INTERVAL);
        mVideoOutputFormat.setInteger(MediaFormat.KEY_DURATION, firstVideo.getDuration());

        PlayerLog.e("VideoConnectFragment", "mVideoOutputFormat : "
                + " width = " + mVideoOutputFormat.getInteger(KEY_WIDTH)
                + " height = " + mVideoOutputFormat.getInteger(KEY_HEIGHT) );


        // 初始化 编码器，只有一个编码器
        mFinalEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mFinalEncoder.configure(mVideoOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodecInputSurface = new CodecInputSurface(mFinalEncoder.createInputSurface());
        mCodecInputSurface.makeCurrent();
        mFinalEncoder.start();
    }

    /**
     * 计算 码率
     * @return
     */
    private int calcBitRate(int width, int height) {
        final int bitrate = (int) (BPP * OUTPUT_FRAME_RATE * width * height);
//        final int bitrate = 800000;
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }



    // 是否整体编解码完毕
    private boolean totalOutputDone = false;
    // 是否当前段的视频 输入完毕
    private boolean currentVideoInputDone = false;
    // 是否读下一个视频
    private boolean isReadNextVideo = false;

    // 解码器 输入Buffer时间戳
    private long decoderInputTimeStamp = 0;
    // 解码器 输入的第一帧
    private boolean isFirstDecodeInputFrame = true;
    // 是否是第一段视频的第一帧
    private  boolean isFirstDecodeOutputFrame = true;
    // 解码器 上一帧时间戳
    private long lastDecoderInputTime = 0;

    // 解码器 输出完成
    private boolean decoderOutputDone = false;
    // 编码器 输出完成
    private boolean encoderOutputDone = false;
    // 解码器 输出视频改变
    private boolean isDecoderOutputVideoChanged = false;

    // 编码器 输入时间戳，写入Surface的时间戳
    private long encoderInputTimeStamp = 0;
    // 编码器 上一帧时间戳
    private long lastEncoderInputTimeStamp = 0;
    // 当前解码视频的位置
    private int currentVideoIndex = 0;

    // 解码器 输入Buffers
    ByteBuffer[] decoderInputBuffers;
    // 编码器 输出Buffers
    ByteBuffer[] encoderOutputBuffers;


    /**
     * 连接多个 视频
     * 读取多个数据 -》解码 -》编码
     */
    public void connectMultiVideo() {

        // 当前解码器
        currentDecoder = mDecoderFormatExtrators.get(0).getDecoder();
        // 创建 解码器输入Surface
        mCodecInputSurface.createRender();
        currentDecoder.configure(mDecoderFormatExtrators.get(0).getMediaFormat(), mCodecInputSurface.getSurface(), null, 0);
        currentDecoder.start();


        // 当前分离器
        currentExtractor = mDecoderFormatExtrators.get(0).getMediaExtractor();

        decoderInputBuffers = currentDecoder.getInputBuffers();
        encoderOutputBuffers = mFinalEncoder.getOutputBuffers();

        // 解码器 输出bufferInfo
        MediaCodec.BufferInfo decoderOutputInfo = new MediaCodec.BufferInfo();
        // 编码器 输出bufferInfo
        MediaCodec.BufferInfo encoderOutputInfo = new MediaCodec.BufferInfo();

        long start = System.currentTimeMillis();
        while (!totalOutputDone) {
            if(!currentVideoInputDone){
                // 每次读取一帧数据
                readOneFrameData(currentDecoder, currentExtractor, decoderInputBuffers);
            }
            decoderOutputDone = false;
            encoderOutputDone = false;
            while (!decoderOutputDone || !encoderOutputDone){
                // 解码器 解码输出
                decodeVideoData(decoderOutputInfo);
                if (!encoderOutputDone) {
                    // 编码器 编码数据 到混合器
                    encodeVideoDataToMuxer(encoderOutputInfo);
                }
            }
        }

        // 释放最后一个decoder
        mFinalEncoder.stop();
        mFinalEncoder.release();
        mMediaMuxer.writeVideoEnd();
        long end = System.currentTimeMillis();
        Log.e(TAG, "---视频编码完成---视频编码耗时-==" + (end - start));
    }

    /**
     * 从当前 视频中读取一帧数据，兼容多个视频
     * @param currentDecoder      当前视频解码器
     * @param currentExtractor    当前视频分离器
     * @param decoderInputBuffers 解码器 输入buffer
     */
    public void readOneFrameData(MediaCodec currentDecoder, MediaExtractor currentExtractor, ByteBuffer[] decoderInputBuffers) {
        PlayerLog.e(TAG, "---readOneFrameData---");

        int inputIndex = currentDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        PlayerLog.e(TAG, "---currentDecoder 输入buffer index--- inputIndex =  " + inputIndex);

        if (inputIndex >= 0){
            // 说明解码器有可用buffer
            ByteBuffer inputBuffer = decoderInputBuffers[inputIndex];
            inputBuffer.clear();

            // 从分离器 读数据
            int readSampleData = currentExtractor.readSampleData(inputBuffer, 0);
            if (readSampleData < 0) {
                /**
                 * 说明当前视频文件 该分离器中 没有数据了 发送一个解码流结束的标志位
                 * */
                PlayerLog.e(TAG, "-----当前视频 读数据结束 发送end--flag");
                currentVideoInputDone = true;
                currentDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                /**
                 * 重写输入数据的时间戳
                 * 关键点在于 如果是下一段视频的数据
                 * 那么 + 30000
                 */
                if(isReadNextVideo) {
                    PlayerLog.e(TAG, "----读下一段视频，时间戳加30000----");
                    isReadNextVideo = false;
                    decoderInputTimeStamp += 30000;
                } else {
                    if (isFirstDecodeInputFrame) {
                        isFirstDecodeInputFrame = false;
                        decoderInputTimeStamp = 0;
                    } else {
                        // 是一直累加的
                        decoderInputTimeStamp += currentExtractor.getSampleTime() - lastDecoderInputTime;
                    }
                    // 当读 下一段视频时，这个值有用
                    lastDecoderInputTime = currentExtractor.getSampleTime();
                    PlayerLog.e(TAG, "----当前采样帧 PTS ---- PTS = " + lastDecoderInputTime);
                    PlayerLog.e(TAG, "----解码器输入帧 PTS ---- PTS = " + decoderInputTimeStamp);

                    // 向解码器 输入buffer
                    currentDecoder.queueInputBuffer(inputIndex, 0, readSampleData, decoderInputTimeStamp, 0);
                    // 分离器 指针指向下一帧
                    currentExtractor.advance();
                    PlayerLog.e(TAG, "----指向下一帧----");
                }
            }
        }
    }

    /**
     * 解码器 解码输出
     * @param decoderOutputInfo  解码器输出 bufferInfo
     */
    public void decodeVideoData(MediaCodec.BufferInfo decoderOutputInfo ) {
        PlayerLog.e(TAG, "---decodeVideoData---");

        /*说明解码器的输出output有数据*/
        int outputIndex = currentDecoder.dequeueOutputBuffer(decoderOutputInfo, TIMEOUT_USEC);
        PlayerLog.e(TAG, "---currentDecoder 输出buffer index--- outputIndex =  " + outputIndex);
        switch (outputIndex) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                PlayerLog.e(TAG, "---currentDecoder 暂无数据输出 --- decoderOutputDone = " + decoderOutputDone);
                // 没有可用解码器输出
                decoderOutputDone = true;
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                break;
            default:
                if (outputIndex >= 0) {
                    /*
                     * 判断本次是否有数据 以及本次数据是否需要传入编码器
                     * */
                    boolean doRender = (decoderOutputInfo.size != 0);
                    if (doRender) {
                        PlayerLog.e(TAG, "---currentDecoder 有数据输出 --- decoderOutputInfo.size = " + decoderOutputInfo.size);
                    }
                    /*
                     * 根据当前解码出来的数据的时间戳 判断 是否需要写入编码器
                     * */
                    boolean isUseful = true;
                    if (decoderOutputInfo.presentationTimeUs <= 0) {
                        doRender = false;
                    }
                    if (doRender) {
                        PlayerLog.e(TAG, "---currentDecoder 有数据输出 渲染到输出Surface图像流 SurfaceTexture---" );
                    }

                    // 渲染到 configure的输出Surface，使用SurfaceTexture类型的Surface承载图像流
                    currentDecoder.releaseOutputBuffer(outputIndex, doRender && isUseful);

                    if (doRender && isUseful) {
                        /**
                         * 是有效数据 让他写到编码器中
                         * 并且对时间戳 进行重写
                         * */
                        PlayerLog.e(TAG, "---等待图像流 帧可用，线程等待 5s......--- ");
                        mCodecInputSurface.awaitNewImage();
                        PlayerLog.e(TAG, "---帧已可用，OpenGl绘制图像流的纹理id 到解码器的输入Surface --- ");
                        mCodecInputSurface.drawImage();
                        PlayerLog.e(TAG, "---帧已可用，OpenGl绘制图像流的纹理id 到解码器的输入Surface --结束 --- ");

                        if (isFirstDecodeOutputFrame) {
                            /**
                             * 如果是第一个视频的话，有可能时间戳不是从0 开始的 所以需要初始化
                             * */
                            isFirstDecodeOutputFrame = false;
                        } else {
                            /**
                             * 如果是更换了一个视频源 就+30000us
                             */
                            if (isDecoderOutputVideoChanged) {
                                PlayerLog.e(TAG, "---解码器 更换了一个视频源=== + 30000--- ");
                                isDecoderOutputVideoChanged = false;
                                // 编码器的输入时间, 给编码器的时间
                                encoderInputTimeStamp = (encoderInputTimeStamp + 30000);
                            } else {
                                encoderInputTimeStamp = (encoderInputTimeStamp + (decoderOutputInfo.presentationTimeUs - lastEncoderInputTimeStamp));
                            }
                        }
                        PlayerLog.e(TAG, "---在编码画面帧的时候，重置编码器输入时间戳 --- encoderInputTimeStamp = " + encoderInputTimeStamp);

                        mCodecInputSurface.setPresentationTime(encoderInputTimeStamp * 1000);
                        PlayerLog.e(TAG, "---swapBuffers--- " );
                        mCodecInputSurface.swapBuffers();
                    } else {
                        PlayerLog.e(TAG, "---解码出来的视频有问题--- " + doRender + "   " + isUseful);
                    }
                    // 编码器 上一帧时间戳
                    lastEncoderInputTimeStamp = decoderOutputInfo.presentationTimeUs;

                    if ((decoderOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        PlayerLog.e(TAG, "---当前视频解码结束--- " );
                        /**
                         * 解码器解码完成了，说明该分离器的数据写入完成了 并且都已经解码完成了
                         * 更换分离器和解码器或者结束编解码
                         * */
                        currentVideoIndex ++;
                        if (currentVideoIndex < mDecoderFormatExtrators.size()) {

                            PlayerLog.e(TAG, "---当前视频解码结束--- ：更换分离器，更换解码器 --- ：start----");

                            /**
                             * 说明还有需要解码的
                             * 1),更换分离器
                             * 2),更换解码器
                             * */
                            // 覆盖新的分离器
                            currentExtractor.release();
                            currentExtractor = mDecoderFormatExtrators.get(currentVideoIndex).getMediaExtractor();

                            // 覆盖新的解码器
                            currentDecoder.stop();
                            currentDecoder.release();
                            currentDecoder = mDecoderFormatExtrators.get(currentVideoIndex).getDecoder();

                            // 配置新的解码器
                            currentDecoder.configure(mDecoderFormatExtrators.get(currentVideoIndex).getMediaFormat(), mCodecInputSurface.getSurface(), null, 0);
                            currentDecoder.start();

                            // 新的解码器输入buffers
                            decoderInputBuffers = currentDecoder.getInputBuffers();

                            // 解码器继续写入数据
                            currentVideoInputDone = false;
                            isDecoderOutputVideoChanged = true;
                            isReadNextVideo = true;
                            PlayerLog.e(TAG, "---当前视频解码结束--- ：更换分离器，更换解码器 --- ：end----");
                        } else {
                            /**
                             * 没有数据了 就给编码器发送一个结束的标志位
                             * */
                            mFinalEncoder.signalEndOfInputStream();
                            currentVideoInputDone = true;
                            PlayerLog.e(TAG, "---所有视频都解码完成了 告诉编码器 可以结束了 --- ");
                        }
                    }
                }
                break;
        }

    }

    /**
     * 编码器 编码数据 到混合器
     * @param encodeOutputInfo
     */
    public void encodeVideoDataToMuxer(MediaCodec.BufferInfo encodeOutputInfo) {
        PlayerLog.e(TAG, "---encodeVideoDataToMuxer---");

        int encodeOutputState = mFinalEncoder.dequeueOutputBuffer(encodeOutputInfo, TIMEOUT_USEC);
        PlayerLog.e(TAG, "---mFinalEncoder 输出buffer index--- encodeOutputState =  " + encodeOutputState);

        switch (encodeOutputState) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                // 说明没有可用的编码器
                encoderOutputDone = true;
                PlayerLog.e(TAG, "---mFinalEncoder 暂无数据输出 --- encoderOutputDone = " + encoderOutputDone);

                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                encoderOutputBuffers = mFinalEncoder.getOutputBuffers();
                break;
            case  MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                // 编码器输出格式变化
                MediaFormat newFormat = mFinalEncoder.getOutputFormat();
                // 混合器添加输出格式
                mMediaMuxer.addTrackFormat(OnMuxerListener.MediaType.MEDIA_TYPE_VIDEO, newFormat);
                PlayerLog.e(TAG, "---mFinalEncoder 添加MediaFormat --- ");

                break;
            default:
                if (encodeOutputState >= 0) {
                    // 整个输出流程结束
                    totalOutputDone = (encodeOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (totalOutputDone) {
                        break;
                    }
                    if ((encodeOutputInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encodeOutputInfo.size = 0;
                    }
                    // 获取 编码器输出Buffer
                    ByteBuffer encoderOutputBuffer = encoderOutputBuffers[encodeOutputState];
                    if (encodeOutputInfo.size > 0) {
                        PlayerLog.e(TAG, "---mFinalEncoder 写入混合器的数据 --- "
                                + " presentationTime = " + encodeOutputInfo.presentationTimeUs
                                + " size = " + encodeOutputInfo.size
                                + " flags= " + encodeOutputInfo.flags);
                        mMediaMuxer.writeSampleData(OnMuxerListener.MediaType.MEDIA_TYPE_VIDEO, encoderOutputBuffer, encodeOutputInfo);
                    }
                    PlayerLog.e(TAG, "---释放buffer 给编码器 --- ");

                    // 释放Buffer 给编码器
                    mFinalEncoder.releaseOutputBuffer(encodeOutputState, false);
                }
                break;
        }
    }






}
