package com.yingke.mediacodec.connect.ui;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.SingleFragmentActivity;
import com.yingke.mediacodec.player.PlayerLog;

/**
 * MediaCodec 多个视频拼接
 */
public class MediaCodecVideoConnectActivity extends SingleFragmentActivity {

    public static final String TAG = "MediaCodecVideoConnectActivity";

    private VideoConnectFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment createFragment() {
        mFragment = VideoConnectFragment.newInstance();
        return mFragment;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PlayerLog.d(TAG, "option - onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_connect, menu);
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

        if (item.getItemId() == R.id.action_merge) {
            if(mFragment != null) {
                mFragment.mergeMultiVideo();
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
