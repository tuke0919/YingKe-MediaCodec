package com.yingke.mediacodec.transcode;

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
        TransCodeTask task = new TransCodeTask(listener);
        task.execute(srcPath, destPath, outputWidth, outputHeight, bitrate);
        return task;
    }


}
