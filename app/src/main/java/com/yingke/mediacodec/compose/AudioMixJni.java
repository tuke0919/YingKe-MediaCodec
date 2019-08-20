package com.yingke.mediacodec.compose;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/20
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class AudioMixJni {

    static {
        System.loadLibrary("audio_mix");
    }

    /**
     * 音频混合 buffer
     * @param sourceA
     * @param sourceB
     * @param dst
     * @param firstVol
     * @param secondVol
     * @return
     */
    public static native byte[] audioMix(byte[] sourceA,byte[] sourceB,byte[] dst,float firstVol , float secondVol);


}
