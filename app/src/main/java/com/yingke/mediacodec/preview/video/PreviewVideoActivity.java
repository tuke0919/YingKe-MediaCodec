package com.yingke.mediacodec.preview.video;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.yingke.mediacodec.R;


/**
 * 简单的视频预览
 */
public class PreviewVideoActivity extends FragmentActivity implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        View.OnClickListener {

    public static final String KEY_VIDEO_PATH = "videoPath";

    private String videoPath = "";
    private ImageView videoBack;
    private MediaController mediaController;
    private VideoView videoView;
    private ImageView videoPlay;
    private int mPositionWhenPaused = -1;

    public static void start(Context context, String videoPath) {
        Intent intent = new Intent(context, PreviewVideoActivity.class);
        intent.putExtra(PreviewVideoActivity.KEY_VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_preview_video);

        videoPath = getIntent().getStringExtra(KEY_VIDEO_PATH);

        videoBack = (ImageView) findViewById(R.id.video_preview_back);
        videoView = (VideoView) findViewById(R.id.video_view);
        videoView.setBackgroundColor(Color.BLACK);

        videoPlay = (ImageView) findViewById(R.id.video_preview_play);

        mediaController = new MediaController(this);

        videoView.setOnCompletionListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setMediaController(mediaController);


        videoBack.setOnClickListener(this);
        videoPlay.setOnClickListener(this);
    }


    @Override
    public void onStart() {
        // Play Video
        videoView.setVideoPath(videoPath);
        videoView.start();
        super.onStart();
    }


    @Override
    public void onResume() {
        // Resume video player
        if (mPositionWhenPaused >= 0) {
            videoView.seekTo(mPositionWhenPaused);
            mPositionWhenPaused = -1;
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        // Stop video when the activity is pause.
        mPositionWhenPaused = videoView.getCurrentPosition();
        videoView.stopPlayback();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mediaController = null;
        videoView = null;
        videoPlay = null;
        super.onDestroy();
    }




    @Override
    public void onCompletion(MediaPlayer mp) {
        videoPlay.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    // video started
                    videoView.setBackgroundColor(Color.TRANSPARENT);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.video_preview_back) {
            finish();
        } else if (id == R.id.video_preview_play) {
            videoView.start();
            videoPlay.setVisibility(View.INVISIBLE);
        }
    }
}
