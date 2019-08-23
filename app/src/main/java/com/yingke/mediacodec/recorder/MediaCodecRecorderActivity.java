package com.yingke.mediacodec.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yingke.mediacodec.BaseActivity;
import com.yingke.mediacodec.R;

public class MediaCodecRecorderActivity extends BaseActivity {

    public static final String RECORD_OUTPUT_PATH = "RECORD_OUTPUT_PATH";


    public static void startForResult(Context context, int requestCode) {
        Intent intent = new Intent(context, MediaCodecRecorderActivity.class);
        ((Activity) context).startActivityForResult(intent, requestCode);
    }

    public RecorderFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_recorder);
        if (savedInstanceState == null) {
            mFragment = new RecorderFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, mFragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        String outputPath = mFragment.getOutputPath();
        Intent intent = new Intent();
        intent.putExtra(RECORD_OUTPUT_PATH, outputPath);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
