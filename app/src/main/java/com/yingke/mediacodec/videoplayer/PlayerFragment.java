package com.yingke.mediacodec.videoplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.videoplayer.media.IFrameCallback;
import com.yingke.mediacodec.videoplayer.media.MediaMoviePlayer;
import com.yingke.mediacodec.videoplayer.view.PlayerTextureView;

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
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class PlayerFragment extends Fragment implements View.OnClickListener {

    private static final boolean DEBUG = true;
    private static final String TAG = "PlayerFragment";
    String path = "http://mov.bn.netease.com/open-movie/nos/mp4/2016/12/15/FC7CH3GGQ_shd.mp4";

    private PlayerTextureView mPlayerView;
    private ImageButton mPlayerButton;

    // MediaCodec播放器
    private MediaMoviePlayer mMoviePlayer;

    public PlayerFragment() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frag_video_player, container, false);
        mPlayerView = (PlayerTextureView)rootView.findViewById(R.id.player_view);
        mPlayerView.setAspectRatio(640 / 480.f);
        mPlayerButton = (ImageButton)rootView.findViewById(R.id.play_btn);
        mPlayerButton.setOnClickListener(this);
        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG){
            Log.v(TAG, "onResume:");
        }
        mPlayerView.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG){
            Log.v(TAG, "onPause:");
        }
        stopPlay();
        mPlayerView.onPause();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn:
                if (mMoviePlayer == null) {
                    startPlay();
                } else {
                    stopPlay();
                }
                break;
        }
    }

    /**
     * 开始播放
     */
    private void startPlay() {
        if (DEBUG) {
            Log.v(TAG, "startPlaying:");
        }
        final Activity activity = getActivity();
        try {
            final File dir = activity.getFilesDir();
            dir.mkdirs();
            final File path = new File(dir, "fc7ch3ggq_shd.mp4");
            prepareSampleMovie(path);
            mPlayerButton.setImageResource(R.mipmap.icon_pause);

            // 准备
            mMoviePlayer = new MediaMoviePlayer(getActivity(), mPlayerView.getSurface(), mIFrameCallback, true);
            mMoviePlayer.prepare(path.toString());

        } catch (IOException e) {
            Log.e(TAG, "startPlay:", e);
        }

    }

    /**
     * 停止播放
     */
    private void stopPlay() {
        if (DEBUG) {
            Log.v(TAG, "startPlaying: mPlayer = " + mMoviePlayer);
        }
        mPlayerButton.setImageResource(R.mipmap.icon_play);

        if (mMoviePlayer != null) {
            mMoviePlayer.release();
            mMoviePlayer = null;
        }
    }


    /**
     * 解码器回调帧
     */
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onPrepared() {
            // 宽高比
            final float aspect = mMoviePlayer.getWidth() / (float)mMoviePlayer.getHeight();
            final Activity activity = getActivity();
            if ((activity != null) && !activity.isFinishing())
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 设置宽高比
                        mPlayerView.setAspectRatio(aspect);
                    }
                });
            // prepare后播放
            mMoviePlayer.play();
        }

        @Override
        public void onFinished() {
            mMoviePlayer = null;
            final Activity activity = getActivity();
            if ((activity != null) && !activity.isFinishing())
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerButton.setImageResource(R.mipmap.icon_play);
                    }
                });
        }

        @Override
        public boolean onFrameAvailable(long presentationTimeUs) {
            return false;
        }

    };


        /**
     * @param path
     * @throws IOException
     */
    private final void prepareSampleMovie(File path) throws IOException {
        final Activity activity = getActivity();

        if (!path.exists()) {
            if (DEBUG) {
                Log.i(TAG, "copy sample movie file from res/raw to app private storage");
            }
            final BufferedInputStream in =
                    new BufferedInputStream(activity.getResources().openRawResource(R.raw.fc7ch3ggq_shd));
            final BufferedOutputStream out = new BufferedOutputStream(activity.openFileOutput(path.getName(), Context.MODE_PRIVATE));
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
