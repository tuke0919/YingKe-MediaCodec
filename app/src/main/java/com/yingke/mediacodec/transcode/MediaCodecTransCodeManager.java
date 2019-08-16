package com.yingke.mediacodec.transcode;

import com.yingke.mediacodec.transcode.listener.ProgressListener;

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
public class MediaCodecTransCodeManager {

    private static  TransCodeTask task;

    /**
     * @param srcPath     原地址
     * @param destPath    输出地址
     * @param outputWidth  新宽
     * @param outputHeight 新高
     * @param bitrate      新码率
     * @param listener     监听器
     * @return
     */
    public static TransCodeTask convertVideo(String srcPath, String destPath, int outputWidth, int outputHeight, int bitrate, ProgressListener listener) {
        task = new TransCodeTask(listener);
        task.execute(srcPath, destPath, outputWidth, outputHeight, bitrate);
        return task;
    }

    public static void cancelTransCodeTask() {
        if(task != null) {
            task.cancel(true);
        }
    }


}
