package com.yingke.mediacodec.recorder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yingke.mediacodec.R;

public class MediaCodecRecorderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_recorder);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new RecorderFragment()).commit();
        }
    }
}
