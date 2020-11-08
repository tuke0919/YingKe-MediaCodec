package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.utils.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.yingke.mediacodec.compose.AudioCodec.TAG;

/**
 * 功能：拼接 多个音频文件
 * </p>
 * <p>Copyright corp.xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MixAudioThread extends Thread {

    public static final String TAG = "MixAudioThread";

    private static final int TIMEOUT_USEC = 0;

    // 音频输出格式
    private MediaFormat mAudioOutputFormat;

    // 混合器监听
    private OnMuxerListener mMediaMuxer;
    // 输入视频
    private List<VideoInfo> mInputVideos;

    // 输出视频分离器
    private List<FormatExtrator> mFormatExtrators;

    public MixAudioThread(List<VideoInfo> inputFiles, OnMuxerListener mediaMuxer) {
        this.mInputVideos = inputFiles;
        this.mMediaMuxer = mediaMuxer;
        mFormatExtrators = new ArrayList<>();
    }


    @Override
    public void run() {
        try {
            prepare();
            simpleAudioMix();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 为每个video创建 分离器和音频轨道
     */
    public void prepare() throws IOException {
        PlayerLog.e(TAG, "---prepare --- ");

        for (int i = 0; i < mInputVideos.size(); i++) {
            // 创建分离器
            MediaExtractor extractor = CodecUtil.createExtractor(mInputVideos.get(i).getPath());
            // 创建选择音频轨道
            int trackIndex = CodecUtil.getAndSelectTrackIndex(extractor, false);
            FormatExtrator formatExtrator = new FormatExtrator();
            formatExtrator.setTrackIndex(trackIndex);
            formatExtrator.setMediaExtractor(extractor);
            mFormatExtrators.add(formatExtrator);
        }
    }

    /**
     * 两个音频简单相连
     *
     * 只是将原视频中的音频分离出来，然后进行添加
     * 不需要编解码等操作
     * 支持多个视频文件的音频连续写入
     * 暂时不考虑视频中没有音频的情况
     * 不考虑部分视频的音频有问题 导致音频速度加快或者减慢的情况（主要是因为立体声和单声道引起的
     * stereo和mono）
     */
    private void simpleAudioMix() {

        PlayerLog.e(TAG, "---simpleAudioMix --- ");

        MediaExtractor firstExtractor = mFormatExtrators.get(0).getMediaExtractor();
        int firstTrackIndex = mFormatExtrators.get(0).getTrackIndex();
        // 音频输出格式
        mAudioOutputFormat = firstExtractor.getTrackFormat(firstTrackIndex);

        // 将第一个视频的audioFormat作为整体音频的audioFormat
        mMediaMuxer.addTrackFormat(OnMuxerListener.MediaType.MEDIA_TYPE_AUDIO, mAudioOutputFormat);


        ByteBuffer buffer = ByteBuffer.allocate(50 * 1024);

        MediaExtractor currentExtractor = firstExtractor;

        // 当前音频轨道
        int currentIndex = 0;
        // 是否到下一段音频
        boolean isNextAudio = false;
        // 是否第一帧
        boolean isFirstFrame = true;
        long lastSampleTime1 = 0;
        long lastSampleTime = 0;

        if (firstTrackIndex != -1) {
            while (true) {
                int readSampleData = currentExtractor.readSampleData(buffer, 0);
                PlayerLog.e(TAG, "---readSampleData --- readSampleData = " + readSampleData);

                if (readSampleData < 0) {
                    PlayerLog.e(TAG, "---当前音频采样完毕，更换分离器--- ：start ---" );

                    // 说明 本地读取完毕了
                    currentIndex ++;

                    if (currentIndex < mFormatExtrators.size()) {
                        // 说明还有新的音频要添加
                        currentExtractor.release();
                        // 更换extractor
                        currentExtractor = mFormatExtrators.get(currentIndex).getMediaExtractor();
                        isNextAudio = true;
                    } else {
                        // 说明已经没有其他的音频了 就break掉
                        break;
                    }

                    PlayerLog.e(TAG, "---当前音频采样完毕，更换分离器--- ：end ---" );

                } else {

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    bufferInfo.offset = 0;
                    bufferInfo.size = readSampleData;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;

                    if (isNextAudio) {

                        PlayerLog.e(TAG, "---读下一段音频，时间戳 + 23219 ---");

                        // 是新的一段音频
                        bufferInfo.presentationTimeUs = lastSampleTime + 23219;
                        isNextAudio = false;

                    } else {

                        if (isFirstFrame) {
                            bufferInfo.presentationTimeUs = 0;
                            isFirstFrame = false;
                        } else {
                            bufferInfo.presentationTimeUs = lastSampleTime + (currentExtractor.getSampleTime() - lastSampleTime1);
                        }
                    }
                    // 最后一帧显示时间
                    lastSampleTime = bufferInfo.presentationTimeUs;
                    PlayerLog.e(TAG, "---保存当前Buffer采样，时间戳 --- lastSampleTime = " + lastSampleTime);

                    // 当不是第一段音频时 lastSampleTime1 != lastSampleTime;
                    lastSampleTime1 = currentExtractor.getSampleTime();
                    PlayerLog.e(TAG, "---当前音频内采样，时间戳 --- lastSampleTime1 = " + lastSampleTime1);

                    PlayerLog.e(TAG, "---混合气 写音频数据---");
                    // 向混合器写数据
                    mMediaMuxer.writeSampleData(OnMuxerListener.MediaType.MEDIA_TYPE_AUDIO, buffer, bufferInfo);
                    // 指向下一帧
                    currentExtractor.advance();
                    PlayerLog.e(TAG, "---指向下一帧 音频 ---");
                }
            }
            currentExtractor.release();
            mMediaMuxer.writeAudioEnd();
        }
    }


}
