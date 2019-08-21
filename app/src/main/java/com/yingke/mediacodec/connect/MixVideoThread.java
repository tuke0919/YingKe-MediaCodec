package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.yingke.mediacodec.transcode.opengl.CodecInputSurface;
import com.yingke.mediacodec.utils.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MixVideoThread extends Thread {

    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_USEC = 0;

    // 混合器监听
    private OnMuxerListener mMediaMuxer;
    // 输入视频
    private List<VideoInfo> mInputVideos;
    // 输出视频 解码器，格式，分离器 列表
    private List<DecoderFormatExtractor> mDecoderFormatExtrators;

    // 最终输出格式
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
//            editVideo(videoOutputFormat);
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
        if (firstVideo.getDuration() == 0 || firstVideo.getDuration() == 180) {
            mVideoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, firstVideo.getWidth(), firstVideo.getHeight());
        } else {
            mVideoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, firstVideo.getHeight(),firstVideo.getWidth());
        }
        mVideoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mVideoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, firstVideo.getBitRate());
        mVideoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, firstVideo.getFrameRate());
        mVideoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, firstVideo.getFrameInterval());


        // 初始化 编码器，只有一个编码器
        mFinalEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mFinalEncoder.configure(mVideoOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodecInputSurface = new CodecInputSurface(mFinalEncoder.createInputSurface());
        mCodecInputSurface.makeCurrent();
        mFinalEncoder.start();
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
    ByteBuffer[] decoderInputBuffers = currentDecoder.getInputBuffers();
    // 编码器 输出Buffers
    ByteBuffer[] encoderOutputBuffers = mFinalEncoder.getOutputBuffers();
    /**
     * 连接多个 视频
     * 读取多个数据 -》解码 -》编码
     */
    public void connectMultiVideo() {

        // 当前解码器
        currentDecoder = mDecoderFormatExtrators.get(0).getDecoder();
        currentDecoder.configure(mDecoderFormatExtrators.get(0).getMediaFormat(), mCodecInputSurface.getSurface(), null, 0);
        currentDecoder.start();
        // 当前分离器
        currentExtractor = mDecoderFormatExtrators.get(0).getMediaExtractor();


        // 解码器 输出bufferInfo
        MediaCodec.BufferInfo decoderOutputInfo = new MediaCodec.BufferInfo();


        // 编码器 输出bufferInfo
        MediaCodec.BufferInfo encoderOutputInfo = new MediaCodec.BufferInfo();

        while (!totalOutputDone) {

            if(!currentVideoInputDone){
                // 每次读取一帧数据
                readOneFrameData(currentDecoder, currentExtractor, decoderInputBuffers);
            }

            while (!decoderOutputDone || ! encoderOutputDone){



            }



        }






    }

    /**
     * 从当前 视频中读取一帧数据，兼容多个视频
     * @param currentDecoder      当前视频解码器
     * @param currentExtractor    当前视频分离器
     * @param decoderInputBuffers 解码器 输入buffer
     */
    public void readOneFrameData(MediaCodec currentDecoder, MediaExtractor currentExtractor, ByteBuffer[] decoderInputBuffers) {
        int inputIndex = currentDecoder.dequeueInputBuffer(TIMEOUT_USEC);
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
                Log.e("send", "-----发送end--flag");
                currentVideoInputDone = true;
                currentDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                /**
                 * 重写输入数据的时间戳
                 * 关键点在于 如果是下一段视频的数据
                 * 那么 + 30000
                 */
                if(isReadNextVideo) {
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
                    // 向解码器 输入buffer
                    currentDecoder.queueInputBuffer(inputIndex, 0, readSampleData, decoderInputTimeStamp, 0);
                    // 分离器 指针指向下一帧
                    currentExtractor.advance();
                }
            }
        }
    }

    /**
     * 解码器 解码输出
     * @param decoderOutputInfo  解码器输出 bufferInfo
     */
    public void decodeVideoData(MediaCodec.BufferInfo decoderOutputInfo ) {

        /*说明解码器的输出output有数据*/
        int outputIndex = currentDecoder.dequeueOutputBuffer(decoderOutputInfo, TIMEOUT_USEC);
        Log.e("videoo", "  解码器出来的index   " + outputIndex);
        switch (outputIndex) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
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
                     * 8、判断本次是否有数据 以及本次数据是否需要传入编码器
                     * */
                    boolean doRender = (decoderOutputInfo.size != 0);
                    /*
                     * 9、根据当前解码出来的数据的时间戳 判断 是否需要写入编码器
                     * */
                    boolean isUseful = true;
                    if (decoderOutputInfo.presentationTimeUs <= 0) {
                        doRender = false;
                    }
                    // 渲染到 configure的输出Surface，使用SurfaceTexture类型的Surface承载图像流
                    currentDecoder.releaseOutputBuffer(outputIndex, doRender && isUseful);

                    if (doRender && isUseful) {
                        /**
                         * 是有效数据 让他写到编码器中
                         * 并且对时间戳 进行重写
                         * */
                        Log.e("videoo", "---卡主了？ 一  " + decoderOutputInfo.size);
                        mCodecInputSurface.awaitNewImage();
                        Log.e("videoo", "---卡住了  === 二");
                        mCodecInputSurface.drawImage();
                        Log.e("videoo", "---卡住了  === 三！！！");


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
                                Log.e("videoo", "---更换了一个视频源=== + 30000");
                                isDecoderOutputVideoChanged = false;
                                // 编码器的输入时间, 给编码器的时间
                                encoderInputTimeStamp = (encoderInputTimeStamp + 30000);
                            } else {
                                encoderInputTimeStamp = (encoderInputTimeStamp + (decoderOutputInfo.presentationTimeUs - lastEncoderInputTimeStamp));
                            }
                        }

                        Log.e("videooo", "---在编码画面帧的时候，重置时间戳===" + encoderInputTimeStamp);
                        mCodecInputSurface.setPresentationTime(encoderInputTimeStamp * 1000);
                        mCodecInputSurface.swapBuffers();
                    } else {
                        Log.e("videoo", "---解码出来的视频有问题=== " + doRender + "   " + isUseful);
                    }
                    // 编码器 上一帧时间戳
                    lastEncoderInputTimeStamp = decoderOutputInfo.presentationTimeUs;

                    if ((decoderOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        /**
                         * 解码器解码完成了，说明该分离器的数据写入完成了 并且都已经解码完成了
                         * 更换分离器和解码器或者结束编解码
                         * */
                        currentVideoIndex ++;
                        if (currentVideoIndex < mDecoderFormatExtrators.size()) {
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
                            Log.e("videoo", "---更换分离器 and 解码器---==");
                        } else {
                            /**
                             * 没有数据了 就给编码器发送一个结束的标志位
                             * */
                            mFinalEncoder.signalEndOfInputStream();
                            currentVideoInputDone = true;
                            Log.e("videoo", "---所有视频都解码完成了 告诉编码器 可以结束了---==");
                        }
                    }
                }
                break;
        }

    }






}
