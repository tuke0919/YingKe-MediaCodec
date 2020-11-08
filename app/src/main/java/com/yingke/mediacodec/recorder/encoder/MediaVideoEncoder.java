package com.yingke.mediacodec.recorder.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.yingke.mediacodec.player.PlayerLog;

import java.io.IOException;
import java.nio.FloatBuffer;


/**
 * 功能：
 * </p>
 * <p>Copyright corp.xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/11
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaVideoEncoder extends MediaEncoder {

    private static final String TAG = MediaVideoEncoder.class.getSimpleName();

    private static final String MIME_TYPE = "video/avc";
    // FPS 帧率
    private static final int FRAME_RATE = 25;
    private static final float BPP = 0.25f;

    private final int mVideoWidth;
    private final int mVideoHeight;

    // 渲染到Surface
    private RecordSurfaceRender mSurfaceRender;
    // 由MediaCodec创建的输入surface
    private Surface mMediaCodecIntputSurface;


    /**
     * 构造方法
     *
     * @param mediaMuxerManager
     * @param mediaEncoderListener
     */
    public MediaVideoEncoder(MediaMuxerManager mediaMuxerManager,
                             MediaEncoderListener mediaEncoderListener,
                             int videoWidth,
                             int videoHeight) {

        super(TAG,mediaMuxerManager, mediaEncoderListener);

        PlayerLog.i(TAG, "MediaVideoEncoder constructor： ");
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;

        // 开始绘制线程，将camera图像帧绘制到 视频编码器的输入Surface上，就是在向编码器写数据
        mSurfaceRender = RecordSurfaceRender.createSurfaceRenderTask(TAG);
    }

    /**
     * 运行在GLThread
     *
     * @param gLCubeBuffer
     * @param gLTextureBuffer
     * @return
     */
    public boolean frameAvailableSoon(int textureId,
                                      FloatBuffer gLCubeBuffer,
                                      FloatBuffer gLTextureBuffer,
                                      float[] textureMatrix) {
        PlayerLog.d(TAG, "---MediaVideoEncoder ：frameAvailableSoon---");

        boolean result;
        if (result = super.frameAvailableSoon()) {
            mSurfaceRender.drawFrame(textureId, gLCubeBuffer, gLTextureBuffer, textureMatrix);
        }
        return result;
    }


    /**
     * 初始化编码器
     * @throws IOException
     */
    @Override
    public void prepare() throws IOException {
        PlayerLog.d(TAG, "---prepare---");

        mTrackIndex = -1;
        mMuxerStarted = mIsEndOfStream = false;

        //-----------------MediaFormat-----------------------
        // mediaCodeC采用的是H.264编码
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoWidth, mVideoHeight);
        // 数据来源自surface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        // fps帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置关键帧的时间
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        //-----------------Encoder编码器-----------------------
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // 获取编码器输入Surface，只能在#configure and #start 之间调用，相机帧写入此Surface
        mMediaCodecIntputSurface = mMediaCodec.createInputSurface();
        // 开始
        mMediaCodec.start();


        PlayerLog.i(TAG, "prepare finishing");
        if (mMediaEncoderListener != null) {
            try {
                mMediaEncoderListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }

    }

    /**
     * 设置Egl环境
     *
     * @param surfaceWidth
     * @param surfaceHeight
     * @param shareEglContext
     */
    public void setEglContext(int surfaceWidth, int surfaceHeight, EGLContext shareEglContext) {
        mSurfaceRender.setEglContext(surfaceWidth, surfaceHeight, mMediaCodecIntputSurface, shareEglContext);
    }


    @Override
    public void release() {
        PlayerLog.i(TAG, "MediaVideoEncoder release:");
        if (mMediaCodecIntputSurface != null) {
            mMediaCodecIntputSurface.release();
            mMediaCodecIntputSurface = null;
        }
        if (mSurfaceRender != null) {
            mSurfaceRender.release();
            mSurfaceRender = null;
        }
        super.release();
    }


    @Override
    public void signalEndOfInputStream() {
        PlayerLog.d(TAG, "sending EOS to encoder");

        // video 向编码器写入EOS帧
        mMediaCodec.signalEndOfInputStream();
        mIsEndOfStream = true;
    }

    /**
     * 码率
     * @return
     */
    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mVideoWidth * mVideoHeight);
//        final int bitrate = 800000;
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }
}
