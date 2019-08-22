package com.yingke.mediacodec.widget.localmedia.config;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;


import com.yingke.mediacodec.R;
import com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaType;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;

import java.io.File;

import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_BMP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_BMP_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_GIF;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_GIF_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_JPEG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_JPEG_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_JPG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_JPG_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_PNG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_PNG_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_WEBP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaSuffix.IMAGE_DOT_WEBP_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaType.MEDIA_TYPE_ALL;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaType.MEDIA_TYPE_AUDIO;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaType.MEDIA_TYPE_IMAGE;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MediaType.MEDIA_TYPE_VIDEO;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_3GPP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_AAC;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_AMR;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_LAMR;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_MP4;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_MPEG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_QUICKTIME;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_WAV;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_X_MS_WMA;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.AUDIO_X_WAV;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_BMP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_GIF;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_GIF_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_JPEG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_JPEG_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_PNG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_PNG_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_WEBP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_WEBP_UPPER;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.IMAGE_X_MS_BMP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_3GP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_3GPP;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_3GPP2;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_AVI;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_MP2TS;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_MP4;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_MPEG;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_QUICKTIME;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_WEBM;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_X_MATROSKA;
import static com.yingke.mediacodec.widget.localmedia.config.MediaConfig.MimeType.VIDEO_X_MSVIDEO;


/**
 * 功能：MediaType <--> MimeType <--> file 三者判断转换
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/24
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaMimeType {


    public static MediaType ofAll() {
        return MEDIA_TYPE_ALL;
    }

    public static MediaType ofImage() {
        return MEDIA_TYPE_IMAGE;
    }

    public static MediaType ofVideo() {
        return MEDIA_TYPE_VIDEO;
    }

    public static MediaType ofAudio() {
        return MEDIA_TYPE_AUDIO;
    }

    /**
     * 判断媒体类型
     * @param mimeType
     * @return
     */
    public static MediaType judgeMediaType(String mimeType) {
        switch (mimeType) {
            case IMAGE_PNG:
            case IMAGE_PNG_UPPER:
            case IMAGE_JPEG:
            case IMAGE_JPEG_UPPER:
            case IMAGE_WEBP:
            case IMAGE_WEBP_UPPER:
            case IMAGE_GIF:
            case IMAGE_BMP:
            case IMAGE_GIF_UPPER:
            case IMAGE_X_MS_BMP:
                return MEDIA_TYPE_IMAGE;
            case VIDEO_3GP:
            case VIDEO_3GPP:
            case VIDEO_3GPP2:
            case VIDEO_AVI:
            case VIDEO_MP4:
            case VIDEO_QUICKTIME:
            case VIDEO_X_MSVIDEO:
            case VIDEO_X_MATROSKA:
            case VIDEO_MPEG:
            case VIDEO_WEBM:
            case VIDEO_MP2TS:
                return MEDIA_TYPE_VIDEO;
            case AUDIO_MPEG:
            case AUDIO_X_MS_WMA:
            case AUDIO_X_WAV:
            case AUDIO_AMR:
            case AUDIO_WAV:
            case AUDIO_AAC:
            case AUDIO_MP4:
            case AUDIO_QUICKTIME:
            case AUDIO_LAMR:
            case AUDIO_3GPP:
                return MEDIA_TYPE_AUDIO;
        }
        return MEDIA_TYPE_IMAGE;
    }


    /**
     * 是否是gif
     *
     * @param mimeType
     * @return
     */
    public static boolean isGifByMimeType(String mimeType) {
        switch (mimeType) {
            case IMAGE_GIF:
            case IMAGE_GIF_UPPER:
                return true;
        }
        return false;
    }

    /**
     * 是否是gif
     *
     * @param path
     * @return
     */
    public static boolean isImageGifByPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            int lastIndex = path.lastIndexOf(".");
            String pictureType = path.substring(lastIndex, path.length());
            return pictureType.startsWith(IMAGE_DOT_GIF)
                    || pictureType.startsWith(IMAGE_DOT_GIF_UPPER);
        }
        return false;
    }

    /**
     * 是否是视频
     *
     * @param mineType
     * @return
     */
    public static boolean isVideoByMimeType(String mineType) {
        switch (mineType) {
            case VIDEO_3GP:
            case VIDEO_3GPP:
            case VIDEO_3GPP2:
            case VIDEO_AVI:
            case VIDEO_MP4:
            case VIDEO_QUICKTIME:
            case VIDEO_X_MSVIDEO:
            case VIDEO_X_MATROSKA:
            case VIDEO_MPEG:
            case VIDEO_WEBM:
            case VIDEO_MP2TS:
                return true;
        }
        return false;
    }

    /**
     * 是否是网络图片
     *
     * @param path
     * @return
     */
    public static boolean isHttp(String path) {
        if (!TextUtils.isEmpty(path)) {
            if (path.startsWith("http")
                    || path.startsWith("https")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件类型是图片还是视频
     *
     * @param file
     * @return mimeType {@link MediaConfig.MimeType}
     */
    public static String getMimeTypeByPath(File file) {
        if (file != null) {
            String name = file.getName();
            if (name.endsWith(MediaConfig.MediaSuffix.VIDEO_DOT_MP4)
                    || name.endsWith(MediaConfig.MediaSuffix.VIDEO_DOT_AVI)
                    || name.endsWith(MediaConfig.MediaSuffix.VIDEO_DOT_3GPP)
                    || name.endsWith(MediaConfig.MediaSuffix.VIDEO_DOT_3GP)
                    || name.startsWith(MediaConfig.MediaSuffix.VIDEO_DOT_MOV)) {
                return VIDEO_MP4;
            } else if (name.endsWith(IMAGE_DOT_PNG_UPPER)
                    || name.endsWith(IMAGE_DOT_PNG)
                    || name.endsWith(IMAGE_DOT_GIF)
                    || name.endsWith(IMAGE_DOT_GIF_UPPER)
                    || name.endsWith(IMAGE_DOT_JPG)
                    || name.endsWith(IMAGE_DOT_JPG_UPPER)
                    || name.endsWith(IMAGE_DOT_WEBP)
                    || name.endsWith(IMAGE_DOT_WEBP_UPPER)
                    || name.endsWith(IMAGE_DOT_JPEG)
                    || name.endsWith(IMAGE_DOT_JPEG_UPPER)
                    || name.endsWith(IMAGE_DOT_BMP)
                    || name.endsWith(IMAGE_DOT_BMP_UPPER)) {
                return IMAGE_JPEG;
            } else if (name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_MP3)
                    || name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_AMR)
                    || name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_AAC)
                    || name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_WAR)
                    || name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_FLAC)
                    || name.endsWith(MediaConfig.MediaSuffix.AUDIO_DOT_LAMR)) {
                return AUDIO_MPEG;
            }
        }
        return IMAGE_JPEG;
    }

    /**
     * is type Equal
     *
     * @param p1
     * @param p2
     * @return
     */
    public static boolean mimeToEqual(String p1, String p2) {
        return judgeMediaType(p1) == judgeMediaType(p2);
    }

    /**
     * 创建 图片 mimeType
     * @param path
     * @return
     */
    public static String createImageMimeType(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                String fileName = file.getName();
                int last = fileName.lastIndexOf(".") + 1;
                String temp = fileName.substring(last, fileName.length());
                return MediaConfig.MimeType.IMAGE_PREFIX + "/" + temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return IMAGE_JPEG;
        }
        return IMAGE_JPEG;
    }

    /**
     * 创建 视频 mimeType
     * @param path
     * @return
     */
    public static String createVideoMimeType(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                String fileName = file.getName();
                int last = fileName.lastIndexOf(".") + 1;
                String temp = fileName.substring(last, fileName.length());
                return MediaConfig.MimeType.VIDEO_PREFIX + "/" + temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return VIDEO_MP4;
        }
        return VIDEO_MP4;
    }


    /**
     * Picture or video
     *
     * @return
     */
    public static MediaType getMediaTypeByMime(String mimeType) {
        if (!TextUtils.isEmpty(mimeType)) {
            if (mimeType.startsWith(MediaConfig.MimeType.VIDEO_PREFIX)) {
                return MEDIA_TYPE_VIDEO;
            } else if (mimeType.startsWith(MediaConfig.MimeType.AUDIO_PREFIX)) {
                return MEDIA_TYPE_AUDIO;
            }
        }
        return MEDIA_TYPE_IMAGE;
    }

    /**
     * get Local video duration
     *
     * @return
     */
    public static int getLocalVideoDuration(String videoPath) {
        int duration;
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(videoPath);
            duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return duration;
    }

    /**
     * 是否是长图
     *
     * @param media
     * @return true 是 or false 不是
     */
    public static boolean isLongImage(LocalMediaResource media) {
        if (null != media) {
            int width = media.getWidth();
            int height = media.getHeight();
            int h = width * 3;
            return height > h;
        }
        return false;
    }

    /**
     * 获取图片后缀
     *
     * @param path
     * @return
     */
    public static String getImageSuffix(String path) {
        try {
            int index = path.lastIndexOf(".");
            if (index > 0) {
                String imageType = path.substring(index, path.length());
                switch (imageType) {
                    case IMAGE_DOT_PNG:
                    case IMAGE_DOT_JPG:
                    case IMAGE_DOT_JPEG:
                    case IMAGE_DOT_WEBP:
                    case IMAGE_DOT_BMP :
                    case IMAGE_DOT_GIF:
                    case IMAGE_DOT_PNG_UPPER:
                    case IMAGE_DOT_JPG_UPPER:
                    case IMAGE_DOT_JPEG_UPPER:
                    case IMAGE_DOT_WEBP_UPPER:
                    case IMAGE_DOT_BMP_UPPER:
                    case IMAGE_DOT_GIF_UPPER:
                        return imageType;
                    default:
                        return IMAGE_DOT_PNG;
                }
            } else {
                return IMAGE_DOT_PNG;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return IMAGE_DOT_PNG;
        }
    }

    /**
     * 根据不同的类型，返回不同的错误提示
     *
     * @param mediaType
     * @return
     */
    public static String error(Context context, MediaType mediaType) {
        Context ctx = context.getApplicationContext();
        switch (mediaType) {
            case MEDIA_TYPE_IMAGE:
                return ctx.getString(R.string.selector_image_error);
            case MEDIA_TYPE_VIDEO:
                return ctx.getString(R.string.selector_video_error);
            case MEDIA_TYPE_AUDIO:
                return ctx.getString(R.string.selector_audio_error);
            default:
                return ctx.getString(R.string.selector_image_error);
        }
    }






}
