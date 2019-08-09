package com.yingke.mediacodec.videoplayer.view;

import android.animation.Animator;
import android.content.Context;
import android.content.Loader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.videoplayer.PlayerLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import static com.yingke.mediacodec.videoplayer.media.MediaMoviePlayer.DEBUG;
import static com.yingke.mediacodec.videoplayer.media.MediaMoviePlayer.TAG;
import static com.yingke.mediacodec.videoplayer.media.MediaMoviePlayer.TAG_STATIC;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 */
public class MediaCodecPlayerControl extends FrameLayout {

    private boolean mIsShowing;

    private ImageButton mPlayBtn;
    private IPlayerView mPlayerView;

    public MediaCodecPlayerControl(@NonNull Context context) {
        super(context);
        initView();
    }

    public MediaCodecPlayerControl(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MediaCodecPlayerControl(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void initView(){
        LayoutInflater.from(getContext()).inflate(R.layout.media_codec_player_control, this);
        mPlayBtn = findViewById(R.id.play_btn);
        mPlayBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerView == null) {
                    return;
                }
                if (mPlayerView.isStop()) {
                    if (DEBUG) {
                        Log.e(TAG, "startPlay()" );
                    }
                    startPlay();
                    mPlayBtn.setImageResource(R.mipmap.icon_pause);
                    return;
                }
                if (mPlayerView.isPlaying()) {
                     if (DEBUG) {
                         Log.e(TAG, "mPlayerView.pause()" );
                     }

                     mPlayerView.pause();
                     mPlayBtn.setImageResource(R.mipmap.icon_play);
                     return;
                }

                if (mPlayerView.isPaused()){
                     if (DEBUG) {
                         Log.e(TAG, "mPlayerView.start()" );
                     }

                     mPlayerView.start();
                     mPlayBtn.setImageResource(R.mipmap.icon_pause);
                     return;
                }
            }
        });
    }

    /**
     * 显示
     */
    public void show() {
        PlayerLog.w("hide()");
        setAlpha(1);
        setVisibility(VISIBLE);
        mIsShowing = true;

        animate().cancel();

        if (mPlayerView != null && mPlayerView.isPlaying()) {
            hide();
        }

    }

    /**
     * 隐藏
     */
    public void hide() {
        PlayerLog.w("hide()");

        postDelayed(new Runnable() {
            @Override
            public void run() {
                animate().alpha(0).setDuration(500).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setVisibility(VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsShowing = false;
                        setVisibility(GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();

            }
        }, 1000);
    }

    /**
     * @return
     */
    public boolean isShowing() {
        PlayerLog.w("isShowing() = " + mIsShowing);
        return mIsShowing;
    }

    /**
     * 设置播放器
     * @param player
     */
    public void setMediaPlayer(IPlayerView player){
        mPlayerView = player;
    }

    /**
     * 播放按钮
     * @return
     */
    public ImageButton getPlayBtn() {
        return mPlayBtn;
    }

    /**
     * 开始播放
     */
    private void startPlay() {

        try {
            final File dir = getContext().getFilesDir();
            dir.mkdirs();
            final File path = new File(dir, "fc7ch3ggq_shd.mp4");
            prepareSampleMovie(path);

            mPlayerView.setVideoPath(path.toString());
            mPlayerView.prepare();

        } catch (IOException e) {

        }
    }

    /**
     * 资源文件拷贝到文件
     * @param path
     * @throws IOException
     */
    private final void prepareSampleMovie(File path) throws IOException {

        if (!path.exists()) {
            mPlayBtn.setImageResource(R.mipmap.icon_loading);

            final BufferedInputStream in = new BufferedInputStream(getContext().getResources().openRawResource(R.raw.fc7ch3ggq_shd));
            final BufferedOutputStream out = new BufferedOutputStream(getContext().openFileOutput(path.getName(), Context.MODE_PRIVATE));
            byte[] buf = new byte[8192];
            int size = in.read(buf);
            while (size > 0) {
                out.write(buf, 0, size);
                size = in.read(buf);
            }
            in.close();
            out.flush();
            out.close();
        }
    }


}
