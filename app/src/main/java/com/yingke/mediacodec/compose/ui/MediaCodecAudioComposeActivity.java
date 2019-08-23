package com.yingke.mediacodec.compose.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.SingleFragmentActivity;
import com.yingke.mediacodec.player.PlayerLog;

/**
 * MediaCodec多音频混音
 */
public class MediaCodecAudioComposeActivity extends SingleFragmentActivity {

    public static final String TAG = "MediaCodecVideoConnectActivity";

    private AudioMixFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment createFragment() {
        mFragment = AudioMixFragment.newInstance();
        return mFragment;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PlayerLog.d(TAG, "option - onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_audio_compose, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PlayerLog.d(TAG, "option - onPrepareOptionsMenu");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PlayerLog.d(TAG, "option - onOptionsItemSelected");

        if (item.getItemId() == R.id.action_multi_aac) {
            if(mFragment != null) {
                mFragment.mixMultiAudio();
            }
            return true;
        }

        if (item.getItemId() == R.id.action_saperate) {
            if(mFragment != null) {
                mFragment.seperateAudio();
            }
            return true;
        }

        if (item.getItemId() == R.id.action_pcm_aac) {
            if(mFragment != null) {
                mFragment.switchPcmToAudio();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mFragment != null) {
            mFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
