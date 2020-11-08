package com.yingke.mediacodec.recorder.glsurface;

import android.content.Context;
import android.opengl.EGL14;
import android.util.AttributeSet;

import com.yingke.mediacodec.recorder.encoder.MediaVideoEncoder;

import javax.microedition.khronos.opengles.GL10;

/**
 * 功能：
 * </p>
 * <p>Copyright xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/11
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaCodecRecordGlSurfaceView extends CameraGlSurfaceView {

    // 视频编码器
    private MediaVideoEncoder mVideoEncoder;
    private boolean flip = true;

    public MediaCodecRecordGlSurfaceView(Context context) {
        super(context);
    }

    public MediaCodecRecordGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }




    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);

        // ---------------视频写入----------------
        // 减少一半的视频数据写入
        flip = !flip;
        if (flip) {
            // ~30fps
            synchronized (this) {
                if (mVideoEncoder != null) {
                    // 通知捕获线程 相机帧可用
                    mVideoEncoder.frameAvailableSoon(mTextureId, gLCubeBuffer, gLTextureBuffer, mTextureMatrix);
                }
            }
        }
    }

    /**
     * 设置视频编码器
     * 开始录制视频时，由主线程||异步线程回调回来的
     *
     * @param videoEncoder
     */
    public void setVideoEncoder(final MediaVideoEncoder videoEncoder) {

        queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (MediaCodecRecordGlSurfaceView.this) {
                    if (videoEncoder != null) {
                        videoEncoder.setEglContext(surfaceWidth, surfaceHeight, EGL14.eglGetCurrentContext());
                    }
                    MediaCodecRecordGlSurfaceView.this.mVideoEncoder = videoEncoder;
                }
            }
        });
    }
}
