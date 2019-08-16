package com.yingke.mediacodec;

import android.os.Environment;

import java.io.File;
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
    private static String DIR_NAME_1 = "MediaCodecRecorder";

    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    /**
     * 获取输出文件路径
     *
     * @param suffix .mp4 - video，.m4a - audio
     * @return 没有写权限 返回null
     */
    public static final File getOutputFile(String dirName, String fileName, final String suffix) {
        DIR_NAME_1 = dirName;
        final File dir = new File(Environment.getExternalStorageDirectory(), DIR_NAME + "/" + DIR_NAME_1);
        if (!dir.getParentFile().exists()) {
            dir.getParentFile().mkdir();
        }
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, fileName + "-" + getDateTimeString() + suffix);
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

}
