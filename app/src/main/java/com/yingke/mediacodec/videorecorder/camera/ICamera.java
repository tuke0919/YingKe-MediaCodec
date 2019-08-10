package com.yingke.mediacodec.videorecorder.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.SurfaceHolder;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/25
 */
public interface ICamera {

    /**
     * @return
     */
    int getCameraId();

    /**
     * 打开相机
     */
    void openCamera();

    /**
     * 打开相机
     */
    void openCamera(int cameraId);

    /**
     * 开始预览
     * @param holder
     */
    void startPreview(SurfaceHolder holder);

    /**
     * 开始预览
     */
    void startPreview(SurfaceTexture texture);

    /**
     * 停止预览
     */
    void stopPreview();

    /**
     * 停止预览 并释放资源
     */
    void releaseCamera();

    /**
     * 切换摄像头
     * @return
     */
    boolean switchCamera();

    /**
     * @return 预览宽高
     */
    Camera.Size getPreviewSize();

    /**
     * @return 图片宽高
     */
    Camera.Size getPictureSize();

    /**
     * @return 是否前置摄像头
     */
    boolean isFrontCamera();

    /**
     * @param previewCallback
     */
    void setPreviewCallback(Camera.PreviewCallback previewCallback);

    /**
     * @param pictureCallback
     */
    void takePicture(Camera.PictureCallback pictureCallback);

    /**
     * @param shutter
     * @param raw
     * @param jpeg
     */
    void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg);

    /**
     * @return 相机
     */
    Camera getCamera();

    /**
     * @return 拍照旋转角度
     */
    int getPictureRotation();

    /**
     * 聚焦到某点
     * @param x
     * @param y
     * @param width
     * @param height
     */
    void focusOnPoint(int x, int y, int width, int height);


    /**
     * 缩放
     * @param isZoomIn
     */
    void handleZoom(boolean isZoomIn);



}
