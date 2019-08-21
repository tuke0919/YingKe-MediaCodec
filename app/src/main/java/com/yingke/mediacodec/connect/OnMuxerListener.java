package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

import static com.yingke.mediacodec.connect.OnMuxerListener.MediaType.MEDIA_TYPE_AUDIO;
import static com.yingke.mediacodec.connect.OnMuxerListener.MediaType.MEDIA_TYPE_VIDEO;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 */
public interface OnMuxerListener {


    @IntDef({
            MEDIA_TYPE_AUDIO,
            MEDIA_TYPE_VIDEO
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaType{
        int MEDIA_TYPE_AUDIO = 100;
        int MEDIA_TYPE_VIDEO = 101;
    }

    /**
     * 向混合器 添加格式
     * @param mediaType
     * @param mediaFormat
     */
    void addTrackFormat(@MediaType int mediaType, MediaFormat mediaFormat);

    /**
     * 向混合器 写数据
     * @param mediaType
     * @param buffer
     * @param bufferInfo
     */
    void writeSampleData(@MediaType int mediaType, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo);


    /**
     * 写音频结束
     */
    void writeAudioEnd();

    /**
     * 写视频结束
     */
    void writeVideoEnd();


}
