package com.yingke.mediacodec.videorecorder.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraProxy implements ICamera, Camera.AutoFocusCallback {

    public static final String TAG = "CameraProxy";

    private Context mContext;
    private int mCameraId;
    private Camera mCamera;
    private Parameters mParameters;
    private CameraInfo mCameraInfo;

    private Size mPreviewSize;
    private Size mPictureSize;

    private int mPreviewDefaultWidth = 1920; // default 1440
    private int mPreviewDefaultHeight = 1080; // default 1080
    private float mPreviewScale;

    // 相机预览的数据回调
    private PreviewCallback mPreviewCallback;
    // 方向监听器
    private OrientationEventListener mOrientationEventListener;
    // 拍照旋转角度
    private int mPictureRotation = 0;
    // 预览帧缓存
    private byte[] mPreviewBuffer;

    // 是否需要设置显示方向
    private boolean mNeedDisplayOrientation;

    public CameraProxy(Context context) {
        this(context, true);
    }

    public CameraProxy(Context context, boolean needDisplayOrientation) {
        mContext = context;
        mCameraId = CameraInfo.CAMERA_FACING_BACK;
        mNeedDisplayOrientation = needDisplayOrientation;

        mOrientationEventListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                Log.d(TAG, "onOrientationChanged：orientation = " + orientation);
                setPictureRotate(orientation);
            }
        };

        mPreviewScale = mPreviewDefaultHeight * 1f / mPreviewDefaultWidth;
    }

    @Override
    public int getCameraId() {
        return mCameraId;
    }

    /**
     * 打开相机
     */
    @Override
    public void openCamera() {
        Log.d(TAG, "openCamera cameraId: " + mCameraId);
        mCamera = Camera.open(mCameraId);
        mCameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        initConfig();
        setDisplayOrientation();
        Log.d(TAG, "openCamera enable mOrientationEventListener");
        mOrientationEventListener.enable();
    }

    @Override
    public void openCamera(int cameraId) {
        releaseCamera();
        mCameraId = cameraId;
        openCamera();
    }



    /**
     * 开始预览
     * @param holder
     */
    @Override
    public void startPreview(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    /**
     * 开始预览
     * @param surface
     */
    @Override
    public void startPreview(SurfaceTexture surface) {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                mCamera.setPreviewTexture(surface);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    /**
     * 停止预览
     */
    @Override
    public void stopPreview() {
        if (mCamera != null) {
            Log.v(TAG, "stopPreview");
            mCamera.stopPreview();
        }
    }

    /**
     * 释放相机资源
     */
    @Override
    public void releaseCamera() {
        if (mCamera != null) {
            Log.v(TAG, "releaseCamera");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        Log.d(TAG, "openCamera disable mOrientationEventListener");
        mOrientationEventListener.disable();
    }


    /**
     * @return 是否前置摄像头
     */
    @Override
    public boolean isFrontCamera() {
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * 初始化相机参数
     */
    private void initConfig() {
        Log.v(TAG, "initConfig");
        try {
            mParameters = mCamera.getParameters();
            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持

            // 设置闪光模式
            List<String> supportedFlashModes = mParameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF)) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            }
            // 设置聚焦模式
            List<String> supportedFocusModes = mParameters.getSupportedFocusModes();

            // 连续聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            // 自动聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            }

            // 设置预览图片格式
            mParameters.setPreviewFormat(ImageFormat.NV21);
            // 设置拍照图片格式
            mParameters.setPictureFormat(ImageFormat.JPEG);
            // 设置曝光强度
            mParameters.setExposureCompensation(0);
            // 设置预览图片大小
            mPreviewSize = getSuitableSize(mParameters.getSupportedPreviewSizes(), "preview");

            mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            Log.d(TAG, "previewWidth: " + mPreviewSize.width + ", previewHeight: " + mPreviewSize.height);

            // 设置拍照图片大小
//            mPictureSize = getSuitableSize(mParameters.getSupportedPictureSizes(), "picture");
            mPictureSize = mParameters.getSupportedPictureSizes().get(0);
            mParameters.setPictureSize(mPictureSize.width, mPictureSize.height);
            Log.d(TAG, "pictureWidth: " + mPictureSize.width + ", pictureHeight: " + mPictureSize.height);

            // 将设置好的parameters添加到相机里
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param sizes
     * @param type
     * @return
     */
    private Size getSuitableSize(List<Size> sizes, String type) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            Log.d(TAG, " type = " + type + " SupportedSize, width: " + size.width + ", height: " + size.height);
            // 先判断比例是否相等
            if (size.width * mPreviewScale == size.height) {
                int delta = Math.abs(mPreviewDefaultWidth - size.width);
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index);
    }

    /**
     * 设置相机显示的方向，必须设置，否则显示的图像方向会错误
     * 注意：此方法设置的是显示方向，是设置进来的 SurfaceHolder，TextureView，SurfaceTexture的方向，是对底层“相机传感器图像”进行过旋转。
     * 如果是Camera+GLSurfaceView，纹理坐标应该和SurfaceTexture的图像 相对应，当然也可以不设置调用此方法，直接和底层“相机传感器图像” 相对应
     * （此例不设置，对底层“相机传感器图像” 纹理映射，旋转纹理坐标即可）
     */
    private void setDisplayOrientation() {
        if (!mNeedDisplayOrientation) {
            return;
        }
        // SurfaceView重建会变
        // 1.竖屏应用时 和 锁定屏幕时 rotation = 0
        // 2.没有锁定屏幕时 ：比如竖屏应用->横屏应用(指的是 随屏幕旋转，没有锁定屏幕)
        // 逆时针0->90->180->270，rotation = 1，顺时针0->90->180->270，rotation = 3，
        int rotation = ((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Log.d(TAG, "setDisplayOrientation: rotation = " + rotation + " degrees = " + degrees );

        int result;
        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror

            Log.d(TAG, "setDisplayOrientation: front - mCameraInfo.orientation = "
                    + mCameraInfo.orientation
                    + " result = " + result );

        } else {  // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;


            Log.d(TAG, "setDisplayOrientation: back - mCameraInfo.orientation = "
                    + mCameraInfo.orientation
                    + " result = " + result );
        }
        mCamera.setDisplayOrientation(result);
    }

    /**
     * 设置拍照的图片的旋转角度
     * @param orientation 与“重力传感器有关”，手机竖直是0，顺时针0~360度
     */
    private void setPictureRotate(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;

        orientation = (orientation + 45) / 90 * 90;
        int rotation;
        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (mCameraInfo.orientation - orientation + 360) % 360;

            Log.d(TAG, "setPictureRotate：front" +
                    " mCameraInfo.orientation  = " + mCameraInfo.orientation +
                    " rotation = " + rotation);

        } else {  // back-facing camera
            rotation = (mCameraInfo.orientation + orientation) % 360;

            Log.d(TAG, "setPictureRotate：back" +
                    " mCameraInfo.orientation  = " + mCameraInfo.orientation +
                    " rotation = " + rotation);
        }
        mPictureRotation = rotation;
    }

    /**
     * 设置预览帧回调
     * @param previewCallback
     */
    @Override
    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
        if (mPreviewBuffer == null) {
            mPreviewBuffer = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2];
        }
        mCamera.addCallbackBuffer(mPreviewBuffer);
        // 设置预览的回调
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
    }

    /**
     * 拍照
     * @param pictureCallback
     */
    @Override
    public void takePicture(Camera.PictureCallback pictureCallback) {
        mCamera.takePicture(null, null, pictureCallback);
    }

    /**
     * 拍照
     * @param shutter
     * @param raw
     * @param jpeg
     */
    @Override
    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg) {
        mCamera.takePicture(shutter, raw, jpeg);
    }

    /**
     * 切换是摄像头
     */
    @Override
    public boolean switchCamera() {
        mCameraId ^= 1; // 先改变摄像头朝向
        releaseCamera();
        openCamera();
        return true;
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public Size getPictureSize() {
        return mPictureSize;
    }

    @Override
    public Camera getCamera() {
        return mCamera;
    }

    @Override
    public int getPictureRotation() {
        return mPictureRotation;
    }


    /**
     * 聚焦到某点
     * @param x
     * @param y
     * @param width
     * @param height
     */
    @Override
    public void focusOnPoint(int x, int y, int width, int height) {
        Log.v(TAG, "touch point (" + x + ", " + y + ")");
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        // 1.先要判断是否支持设置聚焦区域
        if (parameters.getMaxNumFocusAreas() > 0) {

            // 2.以触摸点为中心点，view窄边的1/4为聚焦区域的默认边长
            int length = Math.min(width, height) >> 3; // 1/8的长度
            int left = x - length;
            int top = y - length;
            int right = x + length;
            int bottom = y + length;

            // 3.映射，因为相机聚焦的区域是一个(-1000,-1000)到(1000,1000)的坐标区域
            left = left * 2000 / width - 1000;
            top = top * 2000 / height - 1000;
            right = right * 2000 / width - 1000;
            bottom = bottom * 2000 / height - 1000;

            // 4.判断上述矩形区域是否超过边界，若超过则设置为临界值
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;

            Log.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            ArrayList<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 600));

            // 5.设置聚焦区域
            parameters.setFocusAreas(areas);
        }
        try {
            // 先要取消掉进程中所有的聚焦功能
            mCamera.cancelAutoFocus();
            mCamera.setParameters(parameters);
            // 调用聚焦
            mCamera.autoFocus(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 缩放
     * @param isZoomIn
     */
    @Override
    public void handleZoom(boolean isZoomIn) {

        if (mParameters != null && mParameters.isZoomSupported()) {
            // 最大缩放
            int maxZoom = mParameters.getMaxZoom();
            // 当前的缩放
            int zoom = mParameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            Log.d(TAG, "handleZoom: zoom: " + zoom);
            mParameters.setZoom(zoom);
            mCamera.setParameters(mParameters);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus: " + success);
    }
}
