package com.yingke.mediacodec.simple;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/6
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class DownloadVideo {

    private String mVideoNetworkUrl;
    private String mVideoOutputPath ;

    public DownloadVideo() {

    }

    private void doDownload() throws IOException {
        int maxVideoIntputSize = 0;
        int maxAudioIntputSize = 0;
        int videoFrameRate = 0;

        // 复用器 video 轨道
        int videoMuxerTrackIndex = -1;
        // 复用器 audio 轨道
        int audioMuxerTrackIndex = -1;

        MediaMuxer mediaMuxer = new MediaMuxer(mVideoOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor videoExtractor = createExtractor();
        int videoExtractorTrackIndex = getAndSelectTrackIndex(videoExtractor, true);

        MediaExtractor audioExtractor = createExtractor();
        int audioExtractorTrackIndex = getAndSelectTrackIndex(audioExtractor, false);

        if (videoExtractorTrackIndex != -1) {
            MediaFormat videoTrackFormat = videoExtractor.getTrackFormat(videoExtractorTrackIndex);
            videoMuxerTrackIndex = mediaMuxer.addTrack(videoTrackFormat);
            maxVideoIntputSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            videoFrameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
        }

        if (audioExtractorTrackIndex != -1) {
            MediaFormat audioTrackFormat = audioExtractor.getTrackFormat(audioExtractorTrackIndex);
            audioMuxerTrackIndex = mediaMuxer.addTrack(audioTrackFormat);

            maxAudioIntputSize = audioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

        }

        mediaMuxer.start();

        if (videoMuxerTrackIndex != -1) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.presentationTimeUs = 0;

            ByteBuffer buffer = ByteBuffer.allocate(maxVideoIntputSize);
            while (true) {
                int sampleSize = videoExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                bufferInfo.offset = 0;
                bufferInfo.size = sampleSize;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                bufferInfo.presentationTimeUs += 1000 * 1000 / videoFrameRate;

                mediaMuxer.writeSampleData(videoMuxerTrackIndex, buffer, bufferInfo);

                boolean videoExtractorDone = videoExtractor.advance();
                if (!videoExtractorDone) {
                    break;
                }
            }
        }

        if (audioMuxerTrackIndex != -1) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.presentationTimeUs = 0;

            ByteBuffer buffer = ByteBuffer.allocate(maxAudioIntputSize);
            while (true) {
                int sampleSize = audioExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                bufferInfo.offset = 0;
                bufferInfo.size = sampleSize;
                bufferInfo.flags = audioExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += 1000 * 1000 / videoFrameRate;

                mediaMuxer.writeSampleData(audioMuxerTrackIndex, buffer, bufferInfo);

                boolean audioExtractorDone = audioExtractor.advance();
                if (!audioExtractorDone) {
                    break;
                }
            }
        }

        videoExtractor.release();
        audioExtractor.release();

        mediaMuxer.stop();
        mediaMuxer.release();

    }

    /**
     * 创建收抽取器
     * @return
     * @throws IOException
     */
    public MediaExtractor createExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mVideoNetworkUrl);
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


}
