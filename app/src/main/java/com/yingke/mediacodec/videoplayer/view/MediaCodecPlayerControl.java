package com.yingke.mediacodec.videoplayer.view;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.yingke.mediacodec.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 */
public class MediaCodecPlayerControl extends FrameLayout {

    private ImageButton mPlayBtn;
    private IPlayerView mPlayer;

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
                if (mPlayer == null) {
                    return;
                }
                 if (mPlayer.isPlaying()) {
                     mPlayer.pause();
                     mPlayBtn.setImageResource(R.mipmap.icon_play);
                 } else {
                     startPlay();
                     mPlayBtn.setImageResource(R.mipmap.icon_pause);
                 }
            }
        });
    }

    /**
     * 设置播放器
     * @param player
     */
    public void setMediaPlayer(IPlayerView player){
        mPlayer = player;
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

            mPlayer.setVideoPath(path.toString());
            mPlayer.start();

        } catch (IOException e) {

        }
    }

    /**
     * @param path
     * @throws IOException
     */
    private final void prepareSampleMovie(File path) throws IOException {

        if (!path.exists()) {
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
