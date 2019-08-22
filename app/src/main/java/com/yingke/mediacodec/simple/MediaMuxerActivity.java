package com.yingke.mediacodec.simple;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.yingke.mediacodec.BaseActivity;
import com.yingke.mediacodec.R;

/**
 * 功能：MediaExtractor + MediaMuxer  分离抽取 ，合成视频
 */
public class MediaMuxerActivity extends BaseActivity {

    private DownloadVideo mDownloadVideo;
    private TextView mMuxerBtn;
    private TextView mMuxerLog;

    private String mVideoNetworkUrl = "http://mov.bn.netease.com/open-movie/nos/mp4/2016/12/15/fc7ch3ggq_shd.mp4";
    private String mOutputVideoPath = Environment.getExternalStorageDirectory().getPath()+"/temp.mp4";

    private StringBuilder logSb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_muxer);
        mMuxerBtn = findViewById(R.id.muxer_btn);
        mMuxerLog = findViewById(R.id.muxer_log);
        mMuxerLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        mMuxerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadVideo == null) {
                    mDownloadVideo = new DownloadVideo(mVideoNetworkUrl, mOutputVideoPath);
                    mDownloadVideo.start();
                }
            }
        });
    }

    @Override
    public boolean hasToolbar() {
        return true;
    }

    @Override
    protected boolean isTransStatusBar() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDownloadVideo != null) {
            mDownloadVideo.onDestroy();
        }
    }
}
