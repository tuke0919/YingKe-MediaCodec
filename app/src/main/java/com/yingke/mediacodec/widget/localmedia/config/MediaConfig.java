package com.yingke.mediacodec.widget.localmedia.config;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/24
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaConfig {

    /**
     * 媒体类型
     */
    public static enum MediaType{
        // 全部
        MEDIA_TYPE_ALL,
        // 图片
        MEDIA_TYPE_IMAGE,
        // 视频
        MEDIA_TYPE_VIDEO,
        // 音频
        MEDIA_TYPE_AUDIO
    }

    /**
     * MimeType
     */
    public static interface MimeType {

        String IMAGE_PREFIX = "image";
        String IMAGE_PNG = "image/png";
        String IMAGE_JPEG = "image/jpeg";
        String IMAGE_WEBP = "image/webp";
        String IMAGE_GIF = "image/gif";
        String IMAGE_BMP = "image/bmp";
        String IMAGE_X_MS_BMP  = "image/x-ms-bmp";

        String IMAGE_PNG_UPPER  = "image/PNG";
        String IMAGE_JPEG_UPPER = "image/JPEG";
        String IMAGE_WEBP_UPPER = "image/WEBP";
        String IMAGE_GIF_UPPER  = "image/GIF";

        String VIDEO_PREFIX = "video";
        String VIDEO_3GP = "video/3gp";
        String VIDEO_3GPP = "video/3gpp";
        String VIDEO_3GPP2 = "video/3gpp2";
        String VIDEO_AVI = "video/avi";
        String VIDEO_MP4 = "video/mp4";
        String VIDEO_QUICKTIME = "video/quicktime";
        String VIDEO_X_MSVIDEO = "video/x-msvideo";
        String VIDEO_X_MATROSKA = "video/x-matroska";
        String VIDEO_MPEG = "video/mpeg";
        String VIDEO_WEBM = "video/webm";
        String VIDEO_MP2TS = "video/mp2ts";

        String AUDIO_PREFIX = "audio";
        String AUDIO_MPEG = "audio/mpeg";
        String AUDIO_X_MS_WMA = "audio/x-ms-wma";
        String AUDIO_X_WAV = "audio/x-wav";
        String AUDIO_AMR = "audio/amr";
        String AUDIO_WAV = "audio/wav";
        String AUDIO_AAC = "audio/aac";
        String AUDIO_MP4 = "audio/mp4";
        String AUDIO_QUICKTIME = "audio/quicktime";
        String AUDIO_LAMR = "audio/lamr";
        String AUDIO_3GPP = "audio/3gpp";

    }

    /**
     * 媒体后缀
     */
    public interface MediaSuffix {

        String IMAGE_DOT_PNG  = ".png";
        String IMAGE_DOT_JPG = ".jpg";
        String IMAGE_DOT_JPEG = ".jpeg";
        String IMAGE_DOT_WEBP = ".webp";
        String IMAGE_DOT_BMP  = ".bmp";
        String IMAGE_DOT_GIF  = ".gif";
        String IMAGE_DOT_PNG_UPPER  = ".PNG";
        String IMAGE_DOT_JPG_UPPER = ".JPG";
        String IMAGE_DOT_JPEG_UPPER = ".JPEG";
        String IMAGE_DOT_WEBP_UPPER = ".WEBP";
        String IMAGE_DOT_BMP_UPPER  = ".BMP";
        String IMAGE_DOT_GIF_UPPER  = ".GIF";

        String VIDEO_DOT_3GP  = ".3gp";
        String VIDEO_DOT_MP4  = ".mp4";
        String VIDEO_DOT_MPEG = ".mpeg";
        String VIDEO_DOT_3GPP = ".3gpp";
        String VIDEO_DOT_AVI  = ".avi";
        String VIDEO_DOT_MOV  = ".mov";

        String AUDIO_DOT_MP3  = ".mp3";
        String AUDIO_DOT_AMR  = ".amr";
        String AUDIO_DOT_AAC  = ".aac";
        String AUDIO_DOT_WAR  = ".war";
        String AUDIO_DOT_FLAC = ".flac";
        String AUDIO_DOT_LAMR = ".lamr";

    }

    /**
     * 选择模式
     */
    public enum  SelectionMode{
        /**
         * 多选
         */
        MULTI_SELECTION,
        /**
         * 单选
         */
        SINGLE_SELECTION
    }





}
