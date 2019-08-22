package com.yingke.mediacodec.player;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yingke.mediacodec.BaseActivity;
import com.yingke.mediacodec.R;

/**
 * 基于 MediaCodec 的播放器
 */
public class MediaCodecPlayerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_player);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new PlayerFragment()).commit();
        }
    }
}
