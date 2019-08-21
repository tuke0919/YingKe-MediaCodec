package com.yingke.mediacodec.connect;

import android.media.MediaExtractor;

/**
 * 功能：输出视频 分离器和轨道
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class FormatExtrator {

    // 音频轨道/ 视频轨道
    private int trackIndex;
    // 分离器
    private MediaExtractor mediaExtractor;

    public int getTrackIndex() {
        return trackIndex;
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public MediaExtractor getMediaExtractor() {
        return mediaExtractor;
    }

    public void setMediaExtractor(MediaExtractor mediaExtractor) {
        this.mediaExtractor = mediaExtractor;
    }
}
