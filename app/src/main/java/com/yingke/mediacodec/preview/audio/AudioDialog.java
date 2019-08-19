package com.yingke.mediacodec.preview.audio;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yingke.mediacodec.utils.DateUtils;
import com.yingke.mediacodec.R;


/**
 * 功能：简单音频预览
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/31
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class AudioDialog extends Dialog implements Handler.Callback{

    public static final int WHAT_REMOVE = -1;
    public static final int WHAT_INIT_PLAYER = 0;
    public static final int WHAT_RELEASE_PLAYER = 1;
    public static final int WHAT_UPDATE_UI = 2;


    private MediaPlayer mediaPlayer;
    private TextView tv_musicStatus;
    private SeekBar musicSeekBar;
    private TextView tv_musicTime;
    private TextView tv_musicTotal;

    private TextView tv_PlayPause;
    private TextView tv_Stop;
    private TextView tv_Quit;

    private String audioPath;
    private String audioName;

    private Handler handler;
    private State state;

    public AudioDialog(Context context, String audioPath) {
        this(context, R.style.audio_dialog_style);
        this.audioPath = audioPath;

        handler = new Handler(Looper.getMainLooper(), this);
        state = new State();
    }

    public AudioDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_audio_dialog);
        initViews();
        initDatas();
    }

    public void initViews() {

        tv_musicStatus = (TextView) findViewById(R.id.tv_musicStatus);
        tv_musicTime = (TextView) findViewById(R.id.tv_musicTime);
        musicSeekBar = (SeekBar) findViewById(R.id.musicSeekBar);
        tv_musicTotal = (TextView) findViewById(R.id.tv_musicTotal);
        tv_PlayPause = (TextView) findViewById(R.id.tv_play_pause);
        tv_Stop = (TextView) findViewById(R.id.tv_stop);
        tv_Quit = (TextView) findViewById(R.id.tv_quit);

        tv_PlayPause.setOnClickListener(new audioOnClick(audioPath));
        tv_Stop.setOnClickListener(new audioOnClick(audioPath));
        tv_Quit.setOnClickListener(new audioOnClick(audioPath));

        // 初始化
        Message message = Message.obtain(handler,WHAT_INIT_PLAYER );
        handler.sendMessageDelayed(message, 30);

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeMessages(WHAT_INIT_PLAYER);
                handler.removeMessages(WHAT_RELEASE_PLAYER);
                handler.removeMessages(WHAT_UPDATE_UI);

                // 停止
                Message message = Message.obtain(handler, WHAT_RELEASE_PLAYER);
                handler.sendMessageDelayed(message, 30);

            }
        });
        // 更新UI
        handler.sendEmptyMessage(WHAT_UPDATE_UI);
    }

    public void initDatas() {
        audioName = audioPath.substring(audioPath.indexOf("/") + 1);
        tv_musicStatus.setText(audioName);
        tv_musicStatus.setSelected(true);

        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        params.height = LinearLayout.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;

        window.setWindowAnimations(R.style.Audio_Dialog_Style_Anim);
        window.setAttributes(params);

    }


    /**
     * 初始化音频播放组件
     *
     * @param path
     */
    private void initPlayer(String path) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                }
            });

            state.setState(State.STATE_INIT);
            playAudio();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 播放音频
     */
    private void playAudio() {
        if (mediaPlayer != null) {
            musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            musicSeekBar.setMax(mediaPlayer.getDuration());
        }

        if (state.isInitState()) {
            handler.sendEmptyMessage(WHAT_UPDATE_UI);
            state.setState(State.STATE_PLAY);
        }

        if (state.isPlayState()) {
            tv_PlayPause.setText(getContext().getString(R.string.audio_dialog_pause_text));
            playOrPause();
        }

        if (state.isPauseState()) {
            tv_PlayPause.setText(getContext().getString(R.string.audio_dialog_play_text));
            playOrPause();
        }
    }



    /**
     * 停止播放
     *
     * @param path
     */
    public void stopPlayer(String path) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);

                state.setState(State.STATE_STOP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 释放资源
     */
    private void releasePlayer(String path) {
        stopPlayer(path);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 暂停播放
     */
    public void playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_REMOVE:
                break;
            case WHAT_INIT_PLAYER:
                initPlayer(audioPath);
                break;
            case WHAT_RELEASE_PLAYER:
                releasePlayer(audioPath);
                break;
            case WHAT_UPDATE_UI:
                updateUI();
                break;
        }
        return true;
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        try {
            if (mediaPlayer != null) {
                tv_musicTime.setText(DateUtils.parseTime(mediaPlayer.getCurrentPosition()));
                musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                musicSeekBar.setMax(mediaPlayer.getDuration());
                tv_musicTotal.setText(DateUtils.parseTime(mediaPlayer.getDuration()));

                Message message = Message.obtain(handler,WHAT_UPDATE_UI );
                handler.sendMessageDelayed(message, 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 播放音频点击事件
     */
    public class audioOnClick implements View.OnClickListener {
        private String path;

        public audioOnClick(String path) {
            super();
            this.path = path;
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.tv_play_pause) {
                // 更换状态
                state.setState(!state.isPlayState() ? State.STATE_PLAY : State.STATE_PAUSE);
                playAudio();
            }
            if (id == R.id.tv_stop) {
                tv_PlayPause.setText(getContext().getString(R.string.audio_dialog_play_text));
                stopPlayer(path);
            }
            if (id == R.id.tv_quit) {
                dismiss();
            }
        }
    }

    private class State {

        private static final int STATE_INIT = 0;
        private static final int STATE_PLAY = 1;
        private static final int STATE_PAUSE = 2;
        private static final int STATE_STOP = 3;

        private  int state;

        public void setState(int state) {
            this.state = state;
        }

        public boolean isInitState() {
            return state == STATE_INIT;
        }

        public boolean isPlayState() {
            return state == STATE_PLAY;
        }

        public boolean isPauseState() {
            return state == STATE_PAUSE;
        }

        public boolean isStopState() {
            return state == STATE_STOP;
        }

    }




}
