package com.yingke.mediacodec.player;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.player.view.IPlayerView;
import com.yingke.mediacodec.player.view.MediaCodecPlayerView;

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
public class PlayerFragment extends Fragment {

    private static final boolean DEBUG = true;
    private static final String TAG = "PlayerFragment";
    String path = "http://mov.bn.netease.com/open-movie/nos/mp4/2016/12/15/FC7CH3GGQ_shd.mp4";

    private IPlayerView mPlayerView;

    public PlayerFragment() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frag_video_player, container, false);
        mPlayerView = (MediaCodecPlayerView)rootView.findViewById(R.id.player_view);
        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG){
            Log.v(TAG, "onResume:");
        }
    }

    @Override
    public void onPause() {
        if (DEBUG){
            Log.v(TAG, "onPause:");
        }
        stopPlay();
        super.onPause();
    }

    /**
     * 停止播放
     */
    private void stopPlay() {
        if (DEBUG) {
            Log.v(TAG, "startPlaying: mPlayer = " + mPlayerView);
        }

        if (mPlayerView != null) {
            mPlayerView.pause();
        }
    }



}
