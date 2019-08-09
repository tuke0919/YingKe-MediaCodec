package com.yingke.mediacodec.videoplayer;

import android.util.Log;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/9
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class PlayerLog {

    public static final boolean DEBUG = true;
    public static final String TAG_STATIC = "MediaMoviePlayer:";
    public static final String TAG = TAG_STATIC;

    public static void d(String message){
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message){
        if (DEBUG) {
            Log.e(TAG, message);
        }
    }

    public static void i(String message){
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }

    public static void w(String message){
        if (DEBUG) {
            Log.w(TAG, message);
        }
    }

}
