package com.yingke.mediacodec.connect;

import android.media.MediaCodec;
import android.media.MediaFormat;

/**
 * 功能：解码器，格式，分离器的封装
 * </p>
 * <p>Copyright corp.netease.com 2019 All right reserved </p>
 *
 * @author tuke 时间 2019/8/21
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class DecoderFormatExtractor extends FormatExtrator {

    // 解码器
    public MediaCodec decoder;
    // 输入文件格式
    public MediaFormat mediaFormat;

    public MediaCodec getDecoder() {
        return decoder;
    }

    public void setDecoder(MediaCodec decoder) {
        this.decoder = decoder;
    }

    public MediaFormat getMediaFormat() {
        return mediaFormat;
    }

    public void setMediaFormat(MediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
    }
}
