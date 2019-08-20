package com.yingke.mediacodec.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/16
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class FileUtils {
    private static final String DIR_NAME = "MediaCodec";
    private static String DIR_NAME_RECORD = "MediaCodecRecorder";
    private static String DIR_NAME_TRANCODE = "MediaCodecTranCoder";
    private static String DIR_NAME_COMPOSE_AUDIO = "MediaCodecAudio";

    private static String DIR_NAME_PCM = "pcm";



    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    /**
     * 转码 获取输出文件路径
     *
     * @param fileNamePrefix  文件名前缀
     * @param suffix .mp4 - video，.m4a - audio
     * @return 没有写权限 返回null
     */
    public static final File getTrancodeOutputFile(String fileNamePrefix, final String suffix) {
        final File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME + "/" + DIR_NAME_TRANCODE);
        if (!dir.getParentFile().exists()) {
            dir.getParentFile().mkdir();
        }
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, fileNamePrefix + "-" + getDateTimeString() + suffix);
        }
        return null;
    }

    /**
     * 获取当前时间的格式化形式
     * @return
     */
    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }


    /**
     * 合成音频 获取输出文件路径
     *
     * @param fileName
     * @return 没有写权限 返回null
     */
    public static final File getComposeAudioOutputFile(String prefix, String fileName) {
        final File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME + "/" + DIR_NAME_COMPOSE_AUDIO);
        if (!dir.getParentFile().exists()) {
            dir.getParentFile().mkdir();
        }
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, prefix + "-" + fileName);
        }
        return null;
    }

    /**
     * 获取 pcm路径
     * @param fileName
     * @return
     */
    public static final String getAudioPcmPath(String fileName) {
        final File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME + "/" + DIR_NAME_COMPOSE_AUDIO + "/" + DIR_NAME_PCM);
        if (!dir.getParentFile().exists()) {
            dir.getParentFile().mkdir();
        }
        dir.mkdirs();
        if (dir.canWrite()) {
            try {
                File file = new File(dir,  fileName + ".pcm");
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                return file.getAbsolutePath();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }






}
