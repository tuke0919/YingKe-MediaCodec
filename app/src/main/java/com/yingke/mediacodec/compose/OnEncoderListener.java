package com.yingke.mediacodec.compose;

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
public interface OnEncoderListener {

    /**
     * 解码成功
     */
    void onEncodeSuc();

    /**
     * 解码失败
     */
    void onEncodeErr();
}
