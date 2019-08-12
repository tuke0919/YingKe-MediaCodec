package com.yingke.mediacodec.videorecorder.glsurface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


import com.yingke.mediacodec.videorecorder.camera.CameraProxy;
import com.yingke.mediacodec.videorecorder.camera.ICamera;
import com.yingke.mediacodec.videorecorder.shader.CameraInputFilter;
import com.yingke.mediacodec.videorecorder.shader.OpenGlUtils;
import com.yingke.mediacodec.videorecorder.shader.Rotation;
import com.yingke.mediacodec.videorecorder.shader.TextureRotationUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraGlSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer,  SurfaceTexture.OnFrameAvailableListener {


    // SurfaceTexure纹理id
    protected int mTextureId = OpenGlUtils.NO_TEXTURE;

    // 顶点坐标
    protected FloatBuffer gLCubeBuffer;

    // 纹理坐标
    protected FloatBuffer gLTextureBuffer;

    // GLSurfaceView的宽高
    protected int surfaceWidth;
    protected int surfaceHeight;

    // 图像宽高
    protected int imageWidth;
    protected int imageHeight;

    protected ScaleType mScaleType = ScaleType.FIT_XY;

    // 相机输入着色器
    private CameraInputFilter mCameraInputFilter;

    // 接收相机的图像流
    private SurfaceTexture mSurfaceTexture;

    // 相机
    private ICamera mCameraManger;

    private boolean mIsRecording;

    public CameraGlSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBufferData();

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mCameraManger = getCamera(context);
        this.getHolder().addCallback(this);
        mScaleType = ScaleType.CENTER_CROP;
    }


    /**
     * 初始化顶点数据
     */
    private void initBufferData() {
        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
    }

    /**
     * 返回相机
     * @param context
     * @return
     */
    public ICamera getCamera(Context context) {
        return new CameraProxy(context, false);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0,0, 0, 0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);


        if(mCameraInputFilter == null) {
            mCameraInputFilter = new CameraInputFilter();
        }
        // 创建主程序，获取引用等
        mCameraInputFilter.init();

        if (mTextureId == OpenGlUtils.NO_TEXTURE) {
            // 获取扩展纹理id，相当于起了变量名，开辟了内存空间
            mTextureId = OpenGlUtils.getExternalOESTextureID();

            if (mTextureId != OpenGlUtils.NO_TEXTURE) {
                // 创建SurfaceTexture
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                // 设置 图像帧可用监听器
                mSurfaceTexture.setOnFrameAvailableListener(this);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width, height);
        surfaceWidth = width;
        surfaceHeight = height;
        mCameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);

        // 打开相机
        openCamera();
    }

    /**
     * 打开相机
     */
    private void openCamera(){

        mCameraManger.openCamera();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraManger.getCameraId(), cameraInfo);
        int orientation = cameraInfo.orientation;

        if( orientation == 90 || orientation == 270){
            imageWidth = mCameraManger.getPreviewSize().height;
            imageHeight = mCameraManger.getPreviewSize().width;
        }else{
            imageWidth = mCameraManger.getPreviewSize().width;
            imageHeight = mCameraManger.getPreviewSize().height;
        }

        mCameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);

        // 调整方向
        adjustSize(orientation, mCameraManger.getCameraId() == 1 , true);

        // 设置图像流 开始预览
        if(mSurfaceTexture != null) {
            mCameraManger.startPreview(mSurfaceTexture);
        }

    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        if(mSurfaceTexture == null) {
            return;
        }
        // 更新图片
        mSurfaceTexture.updateTexImage();

        // 获取并设置纹理变换矩阵
        float[] textureMatrix = new float[16];
        mSurfaceTexture.getTransformMatrix(textureMatrix);
        mCameraInputFilter.setTextureTransformMatrix(textureMatrix);


        // 没有使用FBO，直接绘制到窗口默认帧缓冲上，然后到windowSurface
        mCameraInputFilter.onDrawFrame(mTextureId, gLCubeBuffer, gLTextureBuffer);
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        // 释放相机资源
        if (mCameraManger != null) {
            mCameraManger.releaseCamera();
            mCameraManger = null;
        }

        mSurfaceTexture = null;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // 当图像帧 可用时 SurfaceTexture把这帧图像写入 mTextureId，此处调用onDrawFrame(GL10 gl)绘图
        requestRender();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSurfaceTexture != null && mCameraManger !=  null) {
            mCameraManger.startPreview(mSurfaceTexture);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraManger != null) {
            mCameraManger.stopPreview();
        }
    }



    /**
     * 切换摄像头
     */
    public void switchCamera() {
        mCameraManger.switchCamera();
        mCameraManger.startPreview(mSurfaceTexture);
    }

    /**
     * @return 输出文件路径
     */
    public File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MagicCamera");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        String timeStamp = System.currentTimeMillis() + "";
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    /**
     * 开始录视频
     */
    public void startRecord() {
        mIsRecording = true;
    }

    /**
     * 结束录视频
     */
    public void stopRecord() {
        mIsRecording = false;
    }

    /**
     * 聚焦，缩放功能
     */
    private float mOldDistance;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            // 点击聚焦
            mCameraManger.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDistance = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDistance = getFingerSpacing(event);
                if (newDistance > mOldDistance) {
                    // 放大
                    mCameraManger.handleZoom(true);
                } else if (newDistance < mOldDistance) {
                    // 缩小
                    mCameraManger.handleZoom(false);
                }
                mOldDistance = newDistance;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 两个手指的距离
     * @param event
     * @return
     */
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    protected void deleteTextures() {
        if(mTextureId != OpenGlUtils.NO_TEXTURE){
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{mTextureId
                    }, 0);
                    mTextureId = OpenGlUtils.NO_TEXTURE;
                }
            });
        }
    }


    /**
     * 调整 角度
     * @param rotation
     * @param flipHorizontal
     * @param flipVertical
     */
    protected void adjustSize(int rotation, boolean flipHorizontal, boolean flipVertical){

        float[] textureCords = TextureRotationUtil.getRotation(Rotation.fromInt(rotation), flipHorizontal, flipVertical);

        float[] cube = TextureRotationUtil.CUBE;
        // 宽比
        float ratio1 = (float)surfaceWidth / imageWidth;
        // 高比
        float ratio2 = (float)surfaceHeight / imageHeight;

        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float)surfaceWidth;
        float ratioHeight = imageHeightNew / (float)surfaceHeight;

        if(mScaleType == ScaleType.CENTER_INSIDE){
            cube = new float[]{
                    TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                    TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                    TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                    TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
            };
        }else if(mScaleType == ScaleType.FIT_XY){

        }else if(mScaleType == ScaleType.CENTER_CROP){
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distVertical), addDistance(textureCords[1], distHorizontal),
                    addDistance(textureCords[2], distVertical), addDistance(textureCords[3], distHorizontal),
                    addDistance(textureCords[4], distVertical), addDistance(textureCords[5], distHorizontal),
                    addDistance(textureCords[6], distVertical), addDistance(textureCords[7], distHorizontal),
            };
        }

        // 顶点和纹理坐标重新赋值
        gLCubeBuffer.clear();
        gLCubeBuffer.put(cube).position(0);
        gLTextureBuffer.clear();
        gLTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }


    public enum  ScaleType{
        CENTER_INSIDE,
        CENTER_CROP,
        FIT_XY
    }

}
