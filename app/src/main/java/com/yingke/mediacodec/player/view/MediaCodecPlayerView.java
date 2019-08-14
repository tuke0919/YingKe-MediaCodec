package com.yingke.mediacodec.player.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.player.media.IPlayerListener;
import com.yingke.mediacodec.player.media.MediaMoviePlayer;

import static com.yingke.mediacodec.player.media.MediaMoviePlayer.DEBUG;
import static com.yingke.mediacodec.player.media.MediaMoviePlayer.TAG;


/**
 * 功能：播放器
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaCodecPlayerView extends FrameLayout implements IPlayerView{

    private static final int SURFACE_TYPE_NONE = 0;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;

    private AspectRatioFrameLayout mRatioFrameLayout;
    private MediaCodecPlayerControl mPlayerControl;
    private View mSurfaceView;
    // 显示
    private Surface mSurface;
    // 解码器
    private MediaMoviePlayer mPlayer;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    public MediaCodecPlayerView(Context context) {
        super(context);
    }

    public MediaCodecPlayerView(Context context,  AttributeSet attrs) {
        super(context, attrs);
        initViews(attrs);
        initializePlayer();
    }

    public MediaCodecPlayerView(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 初始化布局
     * @param attrs
     */
    private void initViews(AttributeSet attrs) {

        int surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        int resizeMode = AspectRatioFrameLayout.ResizeMode.RESIZE_MODE_FIT;
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PlayerView, 0, 0);
            surfaceType = a.getInt(R.styleable.MediaCodecPlayerView_surface_type, surfaceType);
            resizeMode = a.getInt(R.styleable.MediaCodecPlayerView_surface_type, resizeMode);
            a.recycle();
        }

        LayoutInflater.from(getContext()).inflate(R.layout.media_codec_player_view, this);
        mRatioFrameLayout = findViewById(R.id.media_codec_content_frame);

        // surface
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mSurfaceView = surfaceType == SURFACE_TYPE_TEXTURE_VIEW ? new TextureView(getContext()) : new SurfaceView(getContext());
        mSurfaceView.setLayoutParams(params);
        mRatioFrameLayout.addView(mSurfaceView, 0);
        mRatioFrameLayout.setResizeMode(resizeMode);

        if (mSurfaceView instanceof SurfaceView) {
            mSurface = ((SurfaceView) mSurfaceView).getHolder().getSurface();
        }

        if (mSurfaceView instanceof TextureView) {
            mSurface = new Surface(((TextureView) mSurfaceView).getSurfaceTexture());
        }

        // 控制器
        FrameLayout controlContainer = findViewById(R.id.media_codec_control_container);
        mPlayerControl = new MediaCodecPlayerControl(getContext());
        ViewGroup.LayoutParams params1 = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mPlayerControl.setLayoutParams(params1);
        controlContainer.addView(mPlayerControl);
        mPlayerControl.setMediaPlayer(this);

    }

    /**
     * 初始化播放器
     */
    public void initializePlayer(){
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPlayer = new MediaMoviePlayer(getContext(), mSurface, mIPlayerListener, true);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mPlayer != null && mPlayerControl != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    break;
                case MotionEvent.ACTION_MOVE:

                    break;
                case MotionEvent.ACTION_UP:
                    togglePlayerControl();
                    break;
                case MotionEvent.ACTION_CANCEL:

                    break;
            }


        }
        return true;
    }

    /**
     * 进度条的显示和隐藏
     */
    private void togglePlayerControl() {
        if (mPlayerControl.isShowing()) {
            mPlayerControl.hide();
        } else {
            mPlayerControl.show();
        }
    }


    @Override
    public void prepare() {
        if (DEBUG) {
            Log.e(TAG, "prepare()" );
        }

        if (mPlayer != null) {
            mPlayer.prepare();
        }
    }

    @Override
    public void start() {
        if (DEBUG) {
            Log.e(TAG, "start()" );
        }
        if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    @Override
    public void pause() {
        if (DEBUG) {
            Log.e(TAG, "pause()" );
        }
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Override
    public void resume() {
        if (DEBUG) {
            Log.e(TAG, "pause()" );
        }
        if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    @Override
    public String getDuration() {
        if (DEBUG) {
            Log.e(TAG, "getDuration()" );
        }
        return "";
    }

    @Override
    public int getCurrentPosition() {
        if (DEBUG) {
            Log.e(TAG, "getCurrentPosition()" );
        }
        return 0;
    }

    @Override
    public boolean isStop() {
        if (DEBUG) {
            Log.e(TAG, "isStop()" );
        }
        return mPlayer != null && mPlayer.isStop();
    }

    @Override
    public boolean isPlaying() {
        if (DEBUG) {
            Log.e(TAG, "isPlaying()" );
        }
        return mPlayer != null && mPlayer.isPlaying();
    }

    @Override
    public boolean isPaused() {
        if (DEBUG) {
            Log.e(TAG, "isPaused()" );
        }
        return mPlayer != null && mPlayer.isPaused();
    }

    @Override
    public void setVideoPath(String path) {
        if (DEBUG) {
            Log.e(TAG, "setVideoPath() mPlayer = " + mPlayer );
        }

       if (mPlayer != null) {
           mPlayer.setSourcePath(path);
       }
    }


    /**
     * 解码器回调帧
     */
    private final IPlayerListener mIPlayerListener = new IPlayerListener() {
        @Override
        public void onPrepared() {

            if (DEBUG) {
                Log.e(TAG, "onPrepared()" );
            }

            // 宽高比
            final float aspect = mPlayer.getWidth() / (float)mPlayer.getHeight();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 设置宽高比
                    mRatioFrameLayout.setAspectRatio(aspect);
                    togglePlayerControl();
                }
            });

            // 播放
            mPlayer.play();
        }

        @Override
        public void onFinished() {
            if (DEBUG) {
                Log.e(TAG, "onFinished()" );
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPlayerControl.getPlayBtn().setImageResource(R.mipmap.icon_play);
                }
            });
        }

        @Override
        public boolean onFrameAvailable(long presentationTimeUs) {
            return false;
        }

    };

}
