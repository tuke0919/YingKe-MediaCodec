package com.yingke.mediacodec.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.yingke.mediacodec.player.PlayerLog;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/19
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class CodecUtil {

    public static final String TAG = "CodecUtil";


    public static MediaMuxer createMuxer(String mVideoOutputPath) throws IOException {
        MediaMuxer mediaMuxer = new MediaMuxer(mVideoOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        return mediaMuxer;
    }

    /**
     * 创建收抽取器
     * @param path 本地路径 或 网络url
     * @return
     * @throws IOException
     */
    public static MediaExtractor createExtractor(String path) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(path);
        return mediaExtractor;
    }

    /**
     * 获取并且选择 分离器轨道
     * @param mediaExtractor
     * @param isVideo
     * @return
     */
    public static int getAndSelectTrackIndex(MediaExtractor mediaExtractor, boolean isVideo) {

        for (int index = 0 ; index < mediaExtractor.getTrackCount(); index ++) {
            MediaFormat mediaFormat  = mediaExtractor.getTrackFormat(index);

            PlayerLog.e(TAG, "getAndSelectTrackIndex " + " index = " + index);
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
     * @param mediaFormat
     */
    public static void outputMediaFormat(MediaFormat mediaFormat) {
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
     * 是否 video轨道
     * @param mediaFormat
     * @return
     */
    public static boolean isVideoTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/");
    }

    /**
     * 是否 audio 轨道
     * @param mediaFormat
     * @return
     */
    public static boolean isAudioTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/");
    }
}
