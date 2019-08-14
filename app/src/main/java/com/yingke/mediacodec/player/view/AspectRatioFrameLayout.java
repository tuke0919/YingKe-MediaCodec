package com.yingke.mediacodec.player.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.yingke.mediacodec.R;

import static com.yingke.mediacodec.player.view.AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_FILL;
import static com.yingke.mediacodec.player.view.AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_FIT;
import static com.yingke.mediacodec.player.view.AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_FIXED_HEIGHT;
import static com.yingke.mediacodec.player.view.AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_FIXED_WIDTH;
import static com.yingke.mediacodec.player.view.AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_ZOOM;


/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class AspectRatioFrameLayout extends FrameLayout {

    @IntDef({
            RESIZE_MODE_FIT,
            RESIZE_MODE_FIXED_WIDTH,
            RESIZE_MODE_FIXED_HEIGHT,
            RESIZE_MODE_FILL,
            RESIZE_MODE_ZOOM
    })
    public @interface ResizeMode {
        int RESIZE_MODE_FIT = 0;
        int RESIZE_MODE_FIXED_WIDTH = 1;
        int RESIZE_MODE_FIXED_HEIGHT = 2;
        int RESIZE_MODE_FILL = 3;
        int RESIZE_MODE_ZOOM = 4;
    }

    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;

    private float videoAspectRatio;
    private int resizeMode;

    public AspectRatioFrameLayout(@NonNull Context context) {
        this(context,null);
    }

    public AspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        resizeMode = RESIZE_MODE_FIT;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0);
            try {
                resizeMode = a.getInt(R.styleable.AspectRatioFrameLayout_resize_mode, RESIZE_MODE_FIT);
            } finally {
                a.recycle();
            }
        }
    }

    /**
     * 设置宽高比
     * @param widthHeightRatio
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    /**
     * 设置模式
     * @param resizeMode
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (resizeMode == RESIZE_MODE_FILL || videoAspectRatio <= 0) {
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        final int horizPadding = getPaddingLeft() + getPaddingRight();
        final int vertPadding = getPaddingTop() + getPaddingBottom();

        width -= horizPadding;
        height -= vertPadding;

        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;

        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            return;
        }

        switch (resizeMode) {
            case RESIZE_MODE_FIXED_WIDTH:
                height = (int) (width / videoAspectRatio);
                break;
            case RESIZE_MODE_FIXED_HEIGHT:
                width = (int) (height * videoAspectRatio);
                break;
            case RESIZE_MODE_ZOOM:
                if (aspectDeformation > 0) {
                    width = (int) (height * videoAspectRatio);
                } else {
                    height = (int) (width / videoAspectRatio);
                }
                break;
            default:
                if (aspectDeformation > 0) {
                    height = (int) (width / videoAspectRatio);
                } else {
                    width = (int) (height * videoAspectRatio);
                }
                break;
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
